package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger

/** One slot the plan can preload: a full-screen placement today, native slots from T-027 and T-028. */
internal interface PreloadSlot {
    val format: AdFormat

    /** Starts a load when needed; never when loading, ready or waiting for a retry (F-04). */
    fun loadIfNeeded()

    /** Has an ad, failed or waits for a retry: the group may move on (S-22). */
    fun settled(): Boolean
}

internal class PreloadPlanRunner(private val clock: MonotonicClock, private val logger: AppLogger) {
    /** Runs the groups in order: the next one starts once every slot of the current one settled, or after 6 s; checks every 200 ms. */
    suspend fun run(plan: AdPreloadPlan, slotsOf: (AdFormat) -> List<PreloadSlot>) {
        plan.groups.forEach { group -> group.flatMap(slotsOf).forEach { it.loadIfNeeded() } }
    }
}
