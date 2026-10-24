package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Runs every preload, reload when Internet comes back and native refill through the plan (S-22 to S-25). A preload
 * blocked by a closed gate is held, one at most, and runs once when the gate opens (S-23). Records no opportunity
 * (A-04, HD-06). Main thread only.
 */
internal class AdPreloader(
    private val scope: CoroutineScope,
    private val runner: PreloadPlanRunner,
    private val slotsOf: (AdFormat) -> List<PreloadSlot>,
    private val gate: () -> PreloadGate,
    /** coordinator.onPreloadRequested() of T-020 (S-09). */
    private val askConsentAgain: () -> Unit,
    private val logger: AppLogger,
) {
    private var plan: AdPreloadPlan = AdPreloadPlan.fromPlacements()
    private var held: PreloadReason? = null
    private var everRequested = false
    private val resets = MutableStateFlow(0)

    /** Read by [retryingNativeAdStates]: a new value cuts the native retry wait short (S-24). */
    val nativeRetryResets: StateFlow<Int> = resets.asStateFlow()

    /** True while a plan, or a native refill, runs. */
    var running: Boolean = false
        private set

    /** True once a whole plan has run to its end (S-25). */
    var finishedOnce: Boolean = false
        private set

    /** HD-02; while a plan runs, used from the next run. */
    fun setPlan(plan: AdPreloadPlan) {
        if (running) logger.warn(TAG, "Preload plan set while a plan runs: used from the next run")
        this.plan = plan
    }

    /** S-09, S-23: asks consent again, then runs, holds or ignores the preload. */
    fun request(reason: PreloadReason) {
        askConsentAgain()
        val decision = decidePreload(reason, gate(), everRequested, running)
        if (reason == PreloadReason.SDK_READY) everRequested = true
        logger.info(TAG, "Preload $reason: $decision")
        when (decision) {
            PreloadDecision.RUN -> launch(plan, wholePlan = true)
            PreloadDecision.HOLD -> if (held == null) held = reason
            else -> Unit
        }
    }

    /** The gate changed: the held preload runs once when it may, and is dropped when consent is denied (S-23). */
    fun onGateChanged() {
        val reason = held ?: return
        when (decidePreload(reason, gate(), everRequested, running)) {
            PreloadDecision.RUN -> {
                held = null
                logger.info(TAG, "Held preload $reason: run")
                launch(plan, wholePlan = true)
            }
            PreloadDecision.IGNORE_CONSENT_DENIED -> {
                held = null
                logger.info(TAG, "Held preload $reason: dropped, consent denied")
            }
            else -> Unit
        }
    }

    /** S-24: usable Internet is back: native slots retry at once, and the plan preloads again. */
    fun onNetworkReturned() {
        logger.info(TAG, "Network back")
        resets.value += 1
        request(PreloadReason.NETWORK_BACK)
    }

    /** S-25: an app activity is resumed: refills the native slots in plan order. Never held. */
    fun onActivityResumed(fullScreenShowing: Boolean) {
        val skipped = when {
            !finishedOnce -> "plan never finished"
            running -> "plan running"
            fullScreenShowing -> "full-screen ad showing"
            else -> null
        }
        if (skipped != null) {
            logger.debug(TAG, "Native refill skipped: $skipped")
            return
        }
        val decision = decidePreload(PreloadReason.NATIVE_REFILL, gate(), everRequested, running)
        if (decision != PreloadDecision.RUN) {
            logger.info(TAG, "Native refill skipped: $decision")
            return
        }
        val refill = plan.nativeOnly()
        if (refill.groups.isEmpty()) return
        logger.info(TAG, "Native refill: run")
        launch(refill, wholePlan = false)
    }

    private fun launch(toRun: AdPreloadPlan, wholePlan: Boolean) {
        running = true
        scope.launch {
            try {
                runner.run(toRun, slotsOf)
                if (wholePlan) finishedOnce = true
            } finally {
                running = false
            }
            // A preload held while this one ran gets its turn now.
            onGateChanged()
        }
    }

    private companion object {
        const val TAG = "Ads"
    }
}
