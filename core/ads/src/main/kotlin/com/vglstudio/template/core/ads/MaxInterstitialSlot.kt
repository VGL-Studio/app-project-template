package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/** The one interstitial slot of AppLovin MAX of the app (plan T-069). Main thread only. */
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
) {
    var state: FullScreenLoadState = FullScreenLoadState.Idle
        private set

    var lastLoadError: String? = null
        private set

    fun loadIfNeeded() = Unit

    fun take(): MaxInterstitialSdkAd? = null

    fun onClosedOrShowFailed() = Unit

    companion object {
        const val SLOT = "preload:interstitial/max"
    }
}
