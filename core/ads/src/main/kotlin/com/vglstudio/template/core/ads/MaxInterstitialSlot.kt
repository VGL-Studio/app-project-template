package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The one interstitial slot of AppLovin MAX of the app (plan T-069; one MAX ad unit per format, M-10), holding at most
 * one loaded ad, shared by every interstitial placement that has MAX in its platforms. Main thread only.
 *
 * Every way to a send goes through [send]: a load the controller asks for ([loadIfNeeded]), the wait for MAX to be
 * ready ([maxReady]), the retry after a failed load (F-05) and the reload after a close or a failed show (F-06). Right
 * before sending, [shouldLoad] (MAX in the order and an interstitial placement on) and [mayRequestNow] (the consent
 * gate, S-15) are asked; a no ends the round with no retry. Unit ids never reach the log.
 */
internal class MaxInterstitialSlot(
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
    /** A full-screen ad is on screen (the showing flag of F-01, F-02). */
    private val fullScreenBusy: () -> Boolean = { true },
) {
    /** Ready while an ad is held; Loading; WaitingRetry after a failure; Idle. */
    var state: FullScreenLoadState = FullScreenLoadState.Idle
        private set

    /** `load_state` of the latest failed load (section 8.2), null once a load succeeds. */
    var lastLoadError: String? = null
        private set

    private var ad: MaxInterstitialSdkAd? = null

    /** A load was asked while MAX was not ready: sent once MAX is ready (L-072). */
    private var wanted = false

    /**
     * The ad taken last is on screen: the one SDK object of the slot must not load meanwhile. Read with
     * [fullScreenBusy]: a turn ended without a callback (stuck recovery, forced end, the cap of F-02) drops that object.
     */
    private var showing = false

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
        showing = true
        showCallbacks = onShow
        return ad
    }

    /** F-06: the ad closed or failed to show; the slot loads again after 2 seconds. */
    fun onClosedOrShowFailed() {
        showing = false
        schedule(AdRetryPolicy.reloadAfterCloseMillis(AdFormat.INTERSTITIAL))
    }

    private fun send() {
        val unit = unitId() ?: return
        if (showing) {
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
                logger.info(TAG, "Interstitial (MAX) not requested: not in order or placements off")
                return@launch
            }
            if (!mayRequestNow()) {
                state = FullScreenLoadState.Idle
                logger.info(TAG, "Interstitial (MAX) not requested: consent gate closed")
                return@launch
            }
            val sdkAd = ad ?: newAd(unit).also {
                it.listener = Callbacks(unit)
                ad = it
            }
            telemetry.requestSent(SLOT, clock.nowMillis())
            sdkAd.load()
        }
    }

    private fun schedule(waitMillis: Long) {
        pending?.cancel()
        pending = scope.launch {
            delay(waitMillis)
            send()
        }
    }

    private fun onLoadFailed(code: Int) {
        lastLoadError = telemetry.maxLoadFailed(AdFormat.INTERSTITIAL, SLOT, PRELOAD, code, clock.nowMillis())
        val wait = AdRetryPolicy.fullScreenDelayMillis(failuresInARow())
        logger.warn(TAG, "Interstitial (MAX) failed to load: $code; retry in $wait ms")
        state = FullScreenLoadState.WaitingRetry(clock.nowMillis() + wait)
        schedule(wait)
    }

    /** The callbacks of the one SDK object: the load ones here, the show ones passed to [showCallbacks]. */
    private inner class Callbacks(private val unit: String) : MaxInterstitialListener {
        override fun onLoaded() {
            state = FullScreenLoadState.Ready
            lastLoadError = null
            telemetry.loadSucceeded(AdFormat.INTERSTITIAL, SLOT, PRELOAD, clock.nowMillis())
            telemetry.apiCalled(AdFormat.INTERSTITIAL, unit, null)
            logger.info(TAG, "Interstitial (MAX) loaded")
        }

        override fun onLoadFailed(code: Int) = this@MaxInterstitialSlot.onLoadFailed(code)

        override fun onDisplayed() {
            showCallbacks?.onDisplayed()
        }

        // The end of a show: its callbacks are dropped, so a stray second end of the SDK reaches nothing.
        override fun onHidden() {
            val callbacks = showCallbacks
            showCallbacks = null
            showing = false
            callbacks?.onHidden()
        }

        override fun onDisplayFailed(code: Int) {
            val callbacks = showCallbacks
            showCallbacks = null
            showing = false
            callbacks?.onDisplayFailed(code)
        }

        override fun onClicked() {
            showCallbacks?.onClicked()
        }
    }

    companion object {
        const val SLOT = "preload:interstitial/max"
        private const val TAG = "Ads"
    }
}
