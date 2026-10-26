package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Skeleton of plan T-026, step 3.

internal interface NativeFullScreenPort {
    suspend fun comboEnabled(): Boolean
    fun beginPresence(): Boolean
    fun endPresence()
    fun holdResumeAds(): ForegroundAdSuppressor.Hold
    fun releaseResumeAds(hold: ForegroundAdSuppressor.Hold)
    fun interGone(): Boolean
    fun consentOpen(): Boolean
    fun loadStateAtOpen(): String
    fun takeAd(): LoadedNativeAd?
    fun ensureLoading(): Boolean
    fun loadGeneration(placement: AdPlacement): Int
    suspend fun awaitLoadResult(placement: AdPlacement, since: Int)
    fun recordIntervalMark(source: AdPlacement)
    fun recordOpportunity(result: String, loadState: String)
    fun reload(placement: AdPlacement)
}

internal class NativeFullScreenTurns(
    private val port: NativeFullScreenPort,
    private val clock: MonotonicClock,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
) {
    val view: StateFlow<NativeFullScreenView?> = MutableStateFlow(null)

    fun afterInterstitial(source: AdPlacement, onFinished: () -> Unit) {
        onFinished()
    }

    fun showStandalone(placement: AdPlacement, ad: LoadedNativeAd, onFinished: () -> Unit) {
        onFinished()
    }

    fun close() {}

    fun onBack() {}
}
