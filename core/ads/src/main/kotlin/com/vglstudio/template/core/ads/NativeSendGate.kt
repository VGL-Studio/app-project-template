package com.vglstudio.template.core.ads

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * The one send gate of every in-screen native request (L-072): waits until [foreground] is true, then asks
 * [mayRequestNow] (S-15), read when the wait is over, not when called. For the slots of :core:ads, [foreground] is
 * the in-screen window ([AdsController.inScreenAdWindow]): an app activity is started (O-05, L-A19) and no full-screen
 * ad is drawn over the app (KB-27, plan T-054). False means: send nothing, count no failure, plan no retry. Every
 * path calls it right before it sends: the first load of a slot, the next load after a claim, the one retry of a
 * claimer that lost, the 5 s retry with no ad, the 60 s refresh tick, the retry after a failed refresh, and the
 * refill and retry of a pool.
 */
internal suspend fun awaitSendAllowed(foreground: StateFlow<Boolean>, mayRequestNow: () -> Boolean): Boolean {
    foreground.first { it }
    return mayRequestNow()
}
