package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Runs every preload, reload and refill through the plan (S-23, S-24, S-25). Main thread only. */
internal class AdPreloader(
    private val scope: CoroutineScope,
    private val runner: PreloadPlanRunner,
    private val slotsOf: (AdFormat) -> List<PreloadSlot>,
    private val gate: () -> PreloadGate,
    private val askConsentAgain: () -> Unit,
    private val logger: AppLogger,
) {
    private var plan: AdPreloadPlan = AdPreloadPlan.fromPlacements()
    private val resets = MutableStateFlow(0)

    /** Read by [retryingNativeAdStates]: a new value cuts the native retry wait short (S-24). */
    val nativeRetryResets: StateFlow<Int> = resets.asStateFlow()

    var running: Boolean = false
        private set

    var finishedOnce: Boolean = false
        private set

    fun setPlan(plan: AdPreloadPlan) {
        this.plan = plan
    }

    fun request(reason: PreloadReason) {
        val current = plan
        scope.launch { runner.run(current, slotsOf) }
    }

    fun onGateChanged() {}

    fun onNetworkReturned() {
        request(PreloadReason.NETWORK_BACK)
    }

    fun onActivityResumed(fullScreenShowing: Boolean) {}
}
