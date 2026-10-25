package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

/** Skeleton (plan T-040, step 3). */
@Suppress("UNUSED_PARAMETER")
internal fun refreshingNativeAdStates(
    first: NativeAdState.Loaded,
    source: NativeAdSource,
    foreground: StateFlow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    clock: MonotonicClock,
    logger: AppLogger,
    placementKey: String,
    firstTickDelayMillis: Long = NativeAdPolicy.REFRESH_INTERVAL_MILLIS,
): Flow<NativeAdState> = flowOf(first)
