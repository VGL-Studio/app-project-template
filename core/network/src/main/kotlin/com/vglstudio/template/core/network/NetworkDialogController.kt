package com.vglstudio.template.core.network

import com.vglstudio.template.core.analytics.Analytics
import com.vglstudio.template.core.analytics.AnalyticsEvent
import com.vglstudio.template.core.config.RemoteSettings
import com.vglstudio.template.core.logging.AppLogger
import com.vglstudio.template.core.network.di.NetworkScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Decides when the network dialog shows (spec network, ADR-0024). :app reports the current screen with [setScreen]
 * (`null` on the loading screen), the full-screen ad with [setFullScreenAdShowing] and each return to the foreground
 * with [onAppForeground]; it draws [state] over every screen and calls [retry] from the dialog. Other modules use
 * [NetworkStatus] and [NetworkFailureReporter], never this class.
 *
 * The dialog shows when the device has no usable Internet for longer than network_lost_delay_seconds, at once when
 * the app leaves loading or comes back to the foreground without network, and at once when a download fails
 * because of the network, even while the device still reports a network (KB-3). It never shows on the loading screen
 * nor over a full-screen ad. It closes as soon as the network is back, reported by the device or found by "Retry".
 */
@Singleton
class NetworkDialogController @Inject internal constructor(
    private val connectivity: ConnectivitySource,
    private val settings: RemoteSettings,
    private val analytics: Analytics,
    private val logger: AppLogger,
    private val clock: NetworkClock,
    @param:NetworkScope private val scope: CoroutineScope,
) : NetworkStatus,
    NetworkFailureReporter {
    // Every field below is read and written under this lock: calls come from the main thread, the network scope
    // and any thread that reports a failed download.
    private val lock = Any()
    private var screen: String? = null
    private var adShowing = false
    private var lost = false
    private var lostReason = ""
    private var checking = false
    private var stillOffline = false
    private var retryJob: Job? = null
    private var retryCount = 0
    private var shownAt = 0L
    private var foregroundCount = 0

    // Watches the network only past loading, so the system callback is registered only then.
    private val watching = MutableStateFlow(false)

    private val _state = MutableStateFlow<NetworkDialogState>(NetworkDialogState.Hidden)

    /** What :app draws over the current screen. */
    val state: StateFlow<NetworkDialogState> = _state.asStateFlow()

    private val _dialogShown = MutableStateFlow(false)

    override val dialogShown: StateFlow<Boolean> = _dialogShown.asStateFlow()

    private val _recovered = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val recovered: Flow<Unit> = _recovered.asSharedFlow()

    init {
        scope.launch {
            watching.collectLatest { active ->
                if (active) watchNetwork()
            }
        }
    }

    /**
     * Called by :app with the name of the current screen, `null` on the loading screen (KB-1). Leaving loading
     * without network shows the dialog at once; going back to loading hides it and drops any reported failure.
     */
    fun setScreen(screen: String?) {
        synchronized(lock) {
            val wasActive = this.screen != null
            this.screen = screen
            when {
                screen == null -> {
                    val wasShown = _state.value is NetworkDialogState.Shown
                    lost = false
                    resetRetry()
                    refresh()
                    if (wasShown) logger.info(TAG, "Dialog hidden: loading")
                }
                !wasActive && !connectivity.isConnected() -> markLost("offline_on_start")
                else -> refresh()
            }
        }
        watching.value = screen != null
    }

    /** Called by :app while a full-screen ad is on screen: the dialog waits for it and shows when it closes. */
    fun setFullScreenAdShowing(showing: Boolean) {
        synchronized(lock) {
            if (adShowing == showing) return
            adShowing = showing
            if (showing && _state.value is NetworkDialogState.Shown) {
                resetRetry()
                logger.info(TAG, "Dialog paused: full_screen_ad")
            }
            refresh()
        }
    }

    /** Called by :app at each start of the activity. The first start follows loading, which already checked. */
    fun onAppForeground() {
        synchronized(lock) {
            foregroundCount++
            if (foregroundCount == 1 || screen == null) return
            if (!connectivity.isConnected()) markLost("offline_on_foreground")
        }
    }

    /**
     * Checks the network at once and every [NetworkSettings.RETRY_POLL_MILLIS] for at most
     * [NetworkSettings.RETRY_MAX_MILLIS]. Connected: the dialog closes. Still offline: the dialog stays, with a line
     * saying so. Ignored while a check runs or when the dialog is hidden.
     */
    fun retry() {
        synchronized(lock) {
            val current = _state.value
            if (current !is NetworkDialogState.Shown || current.checking) return
            checking = true
            stillOffline = false
            retryCount++
            refresh()
            retryJob = scope.launch { checkAgain() }
        }
    }

    /** May be called from any thread. Shows the dialog at once past loading, even if the device reports a network. */
    override fun reportNetworkFailure(source: String) {
        synchronized(lock) {
            if (screen == null) return
            markLost("load_failed source=$source online=${connectivity.isConnected()}")
        }
    }

    private suspend fun watchNetwork() = coroutineScope {
        launch {
            connectivity
                .lostAfterDelay { settings.value(NetworkSettings.LOST_DELAY_SECONDS).first() }
                .collect { synchronized(lock) { markLost("device_offline") } }
        }
        connectivity.connectivity().collect { connected -> if (connected) closeOnline() }
    }

    private suspend fun checkAgain() {
        var waited = 0L
        while (!connectivity.isConnected()) {
            if (waited >= NetworkSettings.RETRY_MAX_MILLIS) {
                synchronized(lock) {
                    if (lost && checking) {
                        checking = false
                        stillOffline = true
                        refresh()
                        logger.info(TAG, "Retry: still_offline")
                    }
                }
                return
            }
            delay(NetworkSettings.RETRY_POLL_MILLIS)
            waited += NetworkSettings.RETRY_POLL_MILLIS
        }
        closeOnline()
    }

    /** Must hold [lock]. */
    private fun markLost(reason: String) {
        if (screen == null) return
        if (!lost) {
            lost = true
            lostReason = reason
            resetRetry()
        }
        refresh()
    }

    /** The network is back: closes the dialog and, if it was on screen, records how long it stayed. */
    private fun closeOnline() {
        synchronized(lock) {
            if (!lost) return
            val wasShown = _state.value is NetworkDialogState.Shown
            lost = false
            resetRetry()
            refresh()
            if (wasShown) {
                val durationMillis = clock.elapsedRealtimeMillis() - shownAt
                analytics.log(
                    AnalyticsEvent(
                        name = EVENT_RESTORED,
                        params = mapOf(
                            "duration_ms" to durationMillis.toString(),
                            "retry_count" to retryCount.toString(),
                        ),
                    ),
                )
                logger.info(TAG, "Dialog closed: online after $durationMillis ms, $retryCount retries")
                _recovered.tryEmit(Unit)
            }
        }
    }

    /** Must hold [lock]. Cancels a running check; when the check itself closes the dialog, it ends right after. */
    private fun resetRetry() {
        retryJob?.cancel()
        retryJob = null
        checking = false
        stillOffline = false
    }

    /** Must hold [lock]. Publishes the state; on Hidden to Shown, records network_lost_shown. */
    private fun refresh() {
        val current = screen
        val next = if (lost && current != null && !adShowing) {
            NetworkDialogState.Shown(checking = checking, stillOffline = stillOffline)
        } else {
            NetworkDialogState.Hidden
        }
        val wasShown = _state.value is NetworkDialogState.Shown
        _state.value = next
        _dialogShown.value = next is NetworkDialogState.Shown
        if (!wasShown && next is NetworkDialogState.Shown && current != null) {
            shownAt = clock.elapsedRealtimeMillis()
            retryCount = 0
            analytics.log(AnalyticsEvent(name = EVENT_LOST_SHOWN, params = mapOf("screen" to current)))
            logger.info(TAG, "Dialog shown: $lostReason on $current")
        }
    }

    private companion object {
        const val TAG = "Network"
        const val EVENT_LOST_SHOWN = "network_lost_shown"
        const val EVENT_RESTORED = "network_restored"
    }
}
