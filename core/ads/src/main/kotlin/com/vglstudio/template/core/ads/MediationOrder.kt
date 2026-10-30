package com.vglstudio.template.core.ads

// Skeleton of plan T-068, step 3: the tests are written first; step 4 writes the real rules.

/** One of the two ad platforms (ADR-0037). */
enum class AdPlatform { ADMOB, MAX }

/** `ads_mediation_order` (ADR-0037, KB-31). */
enum class MediationOrder(val code: String, val usesAdmob: Boolean, val usesMax: Boolean) {
    ADMOB("admob", true, false),
    MAX("max", false, true),
    ADMOB_MAX("admob_max", true, true),
    MAX_ADMOB("max_admob", true, true),
    ;

    val platforms: List<AdPlatform> get() = emptyList()

    companion object {
        val DEFAULT = MAX_ADMOB

        fun of(value: String): MediationOrder = DEFAULT

        fun effective(value: MediationOrder, maxAvailable: Boolean): MediationOrder = value
    }
}
