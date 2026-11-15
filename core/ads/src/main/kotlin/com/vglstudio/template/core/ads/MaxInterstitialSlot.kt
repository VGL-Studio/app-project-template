package com.vglstudio.template.core.ads

import android.app.Activity
import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The one full-screen slot of AppLovin MAX of a [format] (one MAX ad unit per format, M-10): interstitial (plan T-069)
 * or rewarded (plan T-106). It holds at most one loaded ad, shared by every placement of the format that has MAX in
 * its platforms. Main thread only.
 *
 * Every way to a send goes through [send]: a load the controller asks for ([loadIfNeeded]), the wait for MAX to be
 * ready ([maxReady]), the retry after a failed load (F-05) and the reload after a close or a failed show (F-06). Right
 * before sending, [shouldLoad] (MAX in the order and a placement of the format on) and [mayRequestNow] (the consent
 * gate, S-15) are asked; a no ends the round with no retry. Unit ids never reach the log.
 *
 * The rewarded slot follows the show of its SDK object itself: the SDK keeps one rewarded object per ad unit and drops
 * a load sent while that object shows, with no callback. So from [showTaken] until the SDK reports the end of the show
 * (the close, or a failed show of every show call before the ad came on screen), the slot sends nothing, then loads
 * again 2 seconds later (F-06), even when the controller ended the turn before. With no such report within
 * [FullScreenAdPresence.SHOWING_CAP_MILLIS] (F-02), the next send destroys that object and builds a new one.
 */
internal class MaxInterstitialSlot(
    private val format: AdFormat = AdFormat.INTERSTITIAL,
    private val scope: CoroutineScope,
    private val clock: MonotonicClock,
    private val unitId: () -> String?,
    private val maxReady: StateFlow<Boolean>,
    private val shouldLoad: suspend () -> Boolean,
    private val mayRequestNow: () -> Boolean,
    private val newAd: (String) -> MaxInterstitialSdkAd,
    private val telemetry: AdTelemetry,
    private val failuresInARow: () -> Int,
    private val logger: AppLogger,
    /** A full-screen ad is on screen (the showing flag of F-01, F-02); not read by the rewarded slot. */
    private val fullScreenBusy: () -> Boolean = { true },
    /** R-03: whether a failed load [code] is a network error (plan T-106). */
    private val networkError: (code: Int) -> Boolean = { false },
) {
    init {
        require(format == AdFormat.INTERSTITIAL || format == AdFormat.REWARDED || format == AdFormat.APP_OPEN) {
            "$format has no MAX slot here"
        }
    }

    /** The key of the load counter of this slot (plan T-106). */
    val slot: String = slotOf(format)

    /** The label of the slot in the log. */
    private val label = if (format == AdFormat.REWARDED) "Rewarded (MAX)" else "Interstitial (MAX)"

    /** Ready while an ad is held; Loading; WaitingRetry after a failure; Idle. */
    var state: FullScreenLoadState = FullScreenLoadState.Idle
        private set

    /** `load_state` of the latest failed load (section 8.2), null once a load succeeds. */
    var lastLoadError: String? = null
        private set

    /**
     * R-03: null with no failed load since the last send (cleared at each send and when a load succeeds), else whether
     * that failure was the network (plan T-106).
     */
    var lastLoadNetworkError: Boolean? = null
        private set

    private var ad: MaxInterstitialSdkAd? = null

    /** A load was asked while MAX was not ready: sent once MAX is ready (L-072). */
    private var wanted = false

    /**
     * Interstitial only: the ad taken last is on screen; the one SDK object of the slot must not load meanwhile. Read
     * with [fullScreenBusy]: a turn ended without a callback (stuck recovery, forced end, the cap of F-02) drops that
     * object.
     */
    private var showing = false

    /** Rewarded only: show was called on the SDK object and the SDK has not reported the end of that show yet. */
    private var sdkShowing = false

    /** Rewarded only: when the first show call of the turn ran. */
    private var shownAt = 0L

    /** Rewarded only: the SDK reported the ad of the turn on screen. */
    private var displayed = false

    /** Rewarded only: show calls of the turn, and failed shows the SDK reported for it. */
    private var showCalls = 0
    private var showFailures = 0

    /** The show callbacks of the ad taken last ([take]). */
    private var showCallbacks: MaxInterstitialListener? = null

    /** The one retry or reload waiting; a new one replaces it. */
    private var pending: Job? = null

    init {
        scope.launch {
            maxReady.collect { ready ->
                if (ready && wanted) {
                    wanted = false
                    send()
                }
            }
        }
    }

    /** Loads unless an ad is held, one is loading or a retry is not due (F-04). */
    fun loadIfNeeded() = send()

    /**
     * Takes the held ad out to show it; the slot is empty after. The show callbacks of the SDK go to [onShow] until
     * the next take. Null when none is held.
     */
    fun take(onShow: MaxInterstitialListener? = null): MaxInterstitialSdkAd? {
        if (state != FullScreenLoadState.Ready) return null
        state = FullScreenLoadState.Idle
        if (format != AdFormat.REWARDED) showing = true
        showCallbacks = onShow
        displayed = false
        showCalls = 0
        showFailures = 0
        return ad
    }

    /**
     * Shows the ad taken last on [activity]: the first show of the turn and every show again (R-06). The SDK object
     * counts as showing from now until the SDK reports the end of the show (plan T-106).
     */
    fun showTaken(activity: Activity) {
        val sdkAd = ad ?: return
        if (showCalls == 0) shownAt = clock.nowMillis()
        showCalls++
        sdkShowing = true
        sdkAd.show(activity)
    }

    /**
     * Stuck recovery (F-03) or a lost host: the turn is dropped, so late callbacks of its show reach nothing. The SDK
     * object may still show: the slot still waits for the SDK to report the end (plan T-106).
     */
    fun releaseShow() {
        showCallbacks = null
    }

    /** F-06: the turn ended (closed or failed to show); the slot loads again after 2 seconds. */
    fun onClosedOrShowFailed() {
        showing = false
        showCallbacks = null
        scheduleReload()
    }

    private fun send() {
        val unit = unitId() ?: return
        if (format == AdFormat.REWARDED && rewardedShowBlocksLoad()) return
        // The showing flag is the interstitial's; the rewarded slot never sets it.
        if (format != AdFormat.REWARDED && showing) {
            if (fullScreenBusy()) return
            // The turn ended with no close from the SDK (forced end, cap of F-02): the ad may still be on screen, so
            // its SDK object is dropped, late callbacks included, and the next load builds a new one.
            ad?.listener = null
            ad = null
            showing = false
            showCallbacks = null
        }
        if (!needsLoad(state, clock.nowMillis(), gateOpen = true)) return
        if (!maxReady.value) {
            wanted = true
            return
        }
        // Loading before anything that suspends: two calls in one frame send once (L-068).
        state = FullScreenLoadState.Loading
        scope.launch {
            if (!shouldLoad()) {
                state = FullScreenLoadState.Idle
                logger.info(TAG, "$label not requested: not in order or placements off")
                return@launch
            }
            if (!mayRequestNow()) {
                state = FullScreenLoadState.Idle
                logger.info(TAG, "$label not requested: consent gate closed")
                return@launch
            }
            val sdkAd = ad ?: newAd(unit).also {
                it.listener = Callbacks(unit)
                ad = it
            }
            // R-03: the network flag is cleared at every send.
            lastLoadNetworkError = null
            telemetry.requestSent(slot, clock.nowMillis())
            sdkAd.load()
        }
    }

    /**
     * Rewarded: whether the SDK object may not load now. It may not while a turn holds the ad, nor while it shows
     * within the cap of F-02. Past that cap with no report from the SDK, the object is destroyed and a new one loads.
     */
    private fun rewardedShowBlocksLoad(): Boolean {
        if (sdkShowing && clock.nowMillis() - shownAt < FullScreenAdPresence.SHOWING_CAP_MILLIS) return true
        if (showCallbacks != null) return true
        if (!sdkShowing) return false
        logger.warn(TAG, "$label still showing after the cap: object dropped")
        ad?.let {
            it.listener = null
            it.destroy()
        }
        ad = null
        sdkShowing = false
        return false
    }

    private fun scheduleReload() = schedule(AdRetryPolicy.reloadAfterCloseMillis(format))

    private fun schedule(waitMillis: Long) {
        pending?.cancel()
        pending = scope.launch {
            delay(waitMillis)
            send()
        }
    }

    private fun onLoadFailed(code: Int) {
        lastLoadError = telemetry.maxLoadFailed(format, slot, PRELOAD, code, clock.nowMillis())
        lastLoadNetworkError = networkError(code)
        val wait = AdRetryPolicy.fullScreenDelayMillis(failuresInARow())
        logger.warn(TAG, "$label failed to load: $code; retry in $wait ms")
        state = FullScreenLoadState.WaitingRetry(clock.nowMillis() + wait)
        schedule(wait)
    }

    /** The callbacks of the one SDK object: the load ones here, the show ones passed to [showCallbacks]. */
    private inner class Callbacks(private val unit: String) : MaxInterstitialListener {
        override fun onLoaded() {
            state = FullScreenLoadState.Ready
            lastLoadError = null
            lastLoadNetworkError = null
            telemetry.loadSucceeded(format, slot, PRELOAD, clock.nowMillis())
            telemetry.apiCalled(format, unit, null)
            logger.info(TAG, "$label loaded")
        }

        override fun onLoadFailed(code: Int) = this@MaxInterstitialSlot.onLoadFailed(code)

        override fun onDisplayed() {
            if (format == AdFormat.REWARDED) {
                displayed = true
                // A failed show again may come before the first show is on screen: the object shows after all.
                sdkShowing = true
            }
            showCallbacks?.onDisplayed()
        }

        // The end of a show: its callbacks are dropped, so a stray second end of the SDK reaches nothing.
        override fun onHidden() {
            val callbacks = showCallbacks
            showCallbacks = null
            showing = false
            if (format == AdFormat.REWARDED) {
                sdkShowing = false
                // F-06 even when the controller already ended the turn (a late close after stuck recovery).
                scheduleReload()
            }
            callbacks?.onHidden()
        }

        override fun onDisplayFailed(code: Int) {
            if (format == AdFormat.REWARDED) {
                onRewardedDisplayFailed(code)
                return
            }
            val callbacks = showCallbacks
            showCallbacks = null
            showing = false
            callbacks?.onDisplayFailed(code)
        }

        /**
         * R-06: a failed show again does not end the show of the turn, so the callbacks stay. The SDK object stops
         * showing only when every show call of the turn failed before the ad came on screen.
         */
        private fun onRewardedDisplayFailed(code: Int) {
            showFailures++
            val callbacks = showCallbacks
            if (!displayed && showFailures >= showCalls) {
                sdkShowing = false
                // The turn was dropped before (stuck recovery): nobody else asks for the reload.
                if (callbacks == null) scheduleReload()
            }
            callbacks?.onDisplayFailed(code)
        }

        override fun onClicked() {
            showCallbacks?.onClicked()
        }

        override fun onRewarded() {
            showCallbacks?.onRewarded()
        }
    }

    companion object {
        const val SLOT = "preload:interstitial/max"
        const val REWARDED_SLOT = "preload:rewarded/max"
        const val APP_OPEN_SLOT = "preload:app_open/max"
        private const val TAG = "Ads"

        /** The key of the load counter of the MAX slot of [format]. */
        fun slotOf(format: AdFormat): String = if (format == AdFormat.REWARDED) REWARDED_SLOT else SLOT
    }
}
