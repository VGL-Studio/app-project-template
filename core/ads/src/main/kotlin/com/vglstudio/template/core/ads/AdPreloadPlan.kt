package com.vglstudio.template.core.ads

/** Groups of ad slots preloaded one after the other (S-22). A format in no group is never preloaded. */
class AdPreloadPlan(groups: List<Set<AdFormat>>) {
    val groups: List<Set<AdFormat>> = groups

    companion object {
        /** HD-02 default: app open and the first step of a flow first, then every other format the placements use. */
        fun fromPlacements(placements: List<AdPlacement> = AdPlacement.entries): AdPreloadPlan =
            defaultPreloadPlan(placements.map { it.format })
    }
}

internal fun defaultPreloadPlan(formats: Collection<AdFormat>): AdPreloadPlan =
    AdPreloadPlan(listOf(AdFormat.entries.toSet()))
