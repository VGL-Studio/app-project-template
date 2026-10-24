package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

/**
 * Loading, then the result of [load]; after a failure, Unavailable and a new try 5 s later (no ad held), sent only
 * in foreground and only when [mayRequestNow]; [retryResets] cuts the wait short (S-24). Never emits Loading again,
 * so an empty slot stays empty until an ad comes. A Loaded ad is destroyed when the collection ends.
 */
internal fun retryingNativeAdStates(
    load: suspend () -> NativeAdState,
    foreground: StateFlow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    logger: AppLogger,
    placementKey: String,
): Flow<NativeAdState> = flow {
    emit(NativeAdState.Loading)
    emit(load())
}

/** One native load of a slot: fresh metrics events and a fresh loader on every call (T-023). */
internal fun nativeLoadOnce(
    newEvents: () -> NativeAdEvents?,
    newLoader: (NativeAdEvents?) -> NativeAdLoader,
    unitId: String,
    size: NativeAdSize,
    timeoutMillis: Long,
    logger: AppLogger,
    placementKey: String,
): suspend () -> NativeAdState = {
    loadOneNativeAd(newLoader(null), unitId, size, timeoutMillis, logger, placementKey)
}
