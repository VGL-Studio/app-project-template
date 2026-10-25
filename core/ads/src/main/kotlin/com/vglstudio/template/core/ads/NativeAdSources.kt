package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/** Skeleton (plan T-040, step 3). */
@Suppress("UNUSED_PARAMETER", "unused")
internal class NativeAdSources(
    private val enabled: (AdPlacement) -> Flow<Boolean>,
    private val canRequestAds: StateFlow<Boolean>,
    private val newLoad: (AdPlacement, AdFormat, NativeLoadUse) -> suspend () -> NativeAdState,
    private val sendGate: suspend () -> Boolean,
    private val clock: MonotonicClock,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
) {
    fun source(placement: AdPlacement, format: AdFormat = placement.format): NativeAdSource =
        NativeAdSource({ NativeAdState.Unavailable }, sendGate, clock, scope, logger, placement.key)
}

internal fun AdFormat.usesSingleAdSource(): Boolean = false

@Suppress("UNUSED_PARAMETER")
internal fun singleAdSlotStates(
    source: NativeAdSource,
    foreground: StateFlow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    clock: MonotonicClock,
    logger: AppLogger,
    placementKey: String,
    firstTickDelayMillis: Long = NativeAdPolicy.REFRESH_INTERVAL_MILLIS,
): Flow<NativeAdState> = emptyFlow()
