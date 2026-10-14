package com.vglstudio.template.core.ads

/** Remote Config keys of the ads module (ADR-0003, ADR-0004) and their values before any remote value arrives. */
internal object AdsSettings {
    fun placementEnabledKey(placement: AdPlacement) = "ads_${placement.key}_enabled"

    const val PLACEMENT_ENABLED_DEFAULT = true

    /**
     * Personalized ads where the law requires a consent form (EEA, UK, Switzerland and similar regions).
     * Off by default: those users get non-personalized ads until the product owner turns it on.
     */
    const val PERSONALIZED_IN_REGULATED_REGIONS = "ads_personalized_regulated_regions_enabled"
    const val PERSONALIZED_IN_REGULATED_REGIONS_DEFAULT = false

    /** Minimum time between two full-screen ads, in seconds. */
    const val INTERSTITIAL_MIN_INTERVAL_SECONDS = "ads_interstitial_min_interval_seconds"
    const val INTERSTITIAL_MIN_INTERVAL_SECONDS_DEFAULT = 120L
}

/**
 * Whether ad requests must ask for non-personalized ads. Outside regulated regions the consent form is not
 * required and personalization follows Google's defaults; inside them it also needs the remote switch.
 */
internal fun requestsNonPersonalizedAds(inRegulatedRegion: Boolean, personalizedInRegulatedRegions: Boolean) =
    inRegulatedRegion && !personalizedInRegulatedRegions
