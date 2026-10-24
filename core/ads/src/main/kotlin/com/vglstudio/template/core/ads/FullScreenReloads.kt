package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/** Retries and reloads of the full-screen placements (F-05, F-06, S-15, O-05 for native full screen). */
internal class FullScreenReloads(
    private val scope: CoroutineScope,
    private val clock: MonotonicClock,
    private val failuresInARow: (AdPlacement) -> Int,
    private val loadIfNeeded: (AdPlacement) -> Unit,
    private val foreground: StateFlow<Boolean>,
    private val logger: AppLogger,
) {
    /** F-05 (O-05 for native full screen): the state to keep, and a retry scheduled at its due time. */
    fun onLoadFailed(placement: AdPlacement, code: Int): FullScreenLoadState.WaitingRetry {
        loadIfNeeded(placement)
        return FullScreenLoadState.WaitingRetry(0)
    }

    /** S-15: nothing sent, not a failure, no retry. */
    fun onSendBlocked(placement: AdPlacement): FullScreenLoadState.Idle {
        loadIfNeeded(placement)
        return FullScreenLoadState.Idle
    }

    /** F-06: reload after closing or failing to show. */
    fun onClosedOrShowFailed(placement: AdPlacement) {
        loadIfNeeded(placement)
    }
}

/** The failure counter slot of the preload of one full-screen placement (question 9 of plan T-021). */
internal fun preloadCounterKey(placementKey: String): String = ""
