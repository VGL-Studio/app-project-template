package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "Ads"

/**
 * Loading, then the result of [load]; after a failure, Unavailable and a new try 5 s later (no ad held), sent only
 * in foreground and only when [mayRequestNow]; [retryResets] cuts the wait short (S-24). Never emits Loading again,
 * so an empty slot stays empty until an ad comes. A Loaded ad is destroyed when the collection ends.
 *
 * - O-05, L-A19: every send, the first one too, waits for [foreground]; a try due in background waits for it.
 * - S-15: [mayRequestNow] false right before a send ends the flow: nothing sent, no failure, no retry. The slot is
 *   built again when the consent gate changes.
 * - O-09: [load] throwing (not a cancellation) is a failure with code 0.
 */
internal fun retryingNativeAdStates(
    load: suspend () -> NativeAdState?,
    foreground: StateFlow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    logger: AppLogger,
    placementKey: String,
    @Suppress("UNUSED_PARAMETER") refresh: ((NativeAdState.Loaded) -> Flow<NativeAdState>)? = null,
): Flow<NativeAdState> = flow {
    emit(NativeAdState.Loading)
    var unavailableShown = false
    var failures = 0
    while (true) {
        foreground.first { it }
        if (!mayRequestNow()) {
            logger.info(TAG, "Native $placementKey request blocked")
            if (!unavailableShown) emit(NativeAdState.Unavailable)
            return@flow
        }
        val state = try {
            load() ?: NativeAdState.Unavailable
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Never the message of the error: it may hold anything.
            logger.info(TAG, "Native $placementKey failed: 0 (request not built)")
            NativeAdState.Unavailable
        }
        if (state is NativeAdState.Loaded) {
            try {
                emit(state)
                awaitCancellation()
            } finally {
                state.ad.destroy()
                logger.info(TAG, "Native $placementKey destroyed")
            }
        }
        failures++
        val generation = retryResets.value
        if (!unavailableShown) {
            emit(NativeAdState.Unavailable)
            unavailableShown = true
        }
        val wait = AdRetryPolicy.nativeDelayMillis(failures, holdsAd = false)
        logger.info(TAG, "Native $placementKey retry in $wait ms (failures $failures)")
        withTimeoutOrNull(wait) { retryResets.first { it != generation } }
    }
}

/**
 * One native load of a slot ([loadOneNativeAd]): fresh metrics events from [newEvents] and a fresh loader from
 * [newLoader] on every call, so the revenue hooks of T-023 (impression placement, 2 s fallback) hold the state of
 * this very ad. A loader that cannot be built or sent is reported to the events as a failure with code 0 (O-09),
 * then rethrown for [retryingNativeAdStates].
 */
internal fun nativeLoadOnce(
    newEvents: () -> NativeAdEvents?,
    newLoader: (NativeAdEvents?) -> NativeAdLoader,
    unitId: String,
    size: NativeAdSize,
    timeoutMillis: Long,
    logger: AppLogger,
    placementKey: String,
): suspend () -> NativeAdState = {
    val events = newEvents()
    try {
        loadOneNativeAd(newLoader(events), unitId, size, timeoutMillis, logger, placementKey)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        events?.onFailed(0, null)
        throw e
    }
}
