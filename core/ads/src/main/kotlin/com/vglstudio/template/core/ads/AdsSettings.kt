package com.vglstudio.template.core.ads

import com.vglstudio.template.core.config.RemoteKey

/**
 * Remote Config keys of the ads module (ADR-0003, ADR-0004, ADR-0023), with their values before any remote
 * value arrives. The defaults are business decisions (ADR-0014). Public so :app can list every key of the app;
 * a key declared here must also be returned by [all] (checked by a unit test).
 */
object AdsSettings {
    /** Master switch of every ad. Declared for the ads rules of the approved spec; not read yet. */
    val ENABLED = RemoteKey.boolean(
        name = "ads_enabled",
        default = true,
        description = "Master switch for every ad. When false, the app shows no ad at all.",
    )

    /**
     * Personalized ads where the law requires a consent form (EEA, UK, Switzerland and similar regions).
     * On by default: users who gave consent get personalized ads; users who declined never do, whatever this says.
     */
    val PERSONALIZED_IN_REGULATED_REGIONS = RemoteKey.boolean(
        name = "ads_personalized_regulated_regions_enabled",
        default = true,
        description = "Allows personalized ads in regions that require consent (EEA, UK, Switzerland), " +
            "for users who consented. Users who declined never get them.",
    )

    /** Minimum time between two full-screen ads, in seconds. */
    val INTERSTITIAL_MIN_INTERVAL_SECONDS = RemoteKey.long(
        name = "ads_interstitial_min_interval_seconds",
        default = 45L,
        description = "Minimum time between two full-screen ads (interstitial and full-screen native), in seconds.",
    )

    /** Declared for the ads rules of the approved spec; not read yet. */
    val FIRST_SESSION_GRACE_SECONDS = RemoteKey.long(
        name = "ads_first_session_grace_seconds",
        default = 180L,
        description = "Time without full-screen ads at the start of the first session after install, in seconds.",
    )

    /** Declared for the ads rules of the approved spec; not read yet. */
    val INTERSTITIAL_MAX_PER_SESSION_COUNT = RemoteKey.long(
        name = "ads_interstitial_max_per_session_count",
        default = 4L,
        description = "Maximum number of full-screen ads shown in one session.",
    )

    /** Declared for the ads rules of the approved spec; not read yet. */
    val INLIST_EVERY_COUNT = RemoteKey.long(
        name = "ads_inlist_every_count",
        default = 8L,
        description = "Number of list items between two native ads inside a list.",
    )

    /** Declared for the ads rules of the approved spec; not read yet. */
    val NATIVE_LOAD_TIMEOUT_SECONDS = RemoteKey.long(
        name = "ads_native_load_timeout_seconds",
        default = 10L,
        description = "Time to wait for a native ad before its space is removed when no ad is available, in seconds.",
    )

    /** Off by default: a placement shows ads only once the product owner switches it on remotely (ADR-0014). */
    fun placementEnabled(placement: AdPlacement): RemoteKey<Boolean> = RemoteKey.boolean(
        name = "ads_${placement.key}_enabled",
        default = false,
        description = "Shows ads in the placement ${placement.key} (${placement.format.name.lowercase()}). " +
            "Off by default.",
    )

    /** Every key of the module: the keys above in declaration order, then one switch per [AdPlacement]. */
    fun all(): List<RemoteKey<*>> = listOf(
        ENABLED,
        PERSONALIZED_IN_REGULATED_REGIONS,
        INTERSTITIAL_MIN_INTERVAL_SECONDS,
        FIRST_SESSION_GRACE_SECONDS,
        INTERSTITIAL_MAX_PER_SESSION_COUNT,
        INLIST_EVERY_COUNT,
        NATIVE_LOAD_TIMEOUT_SECONDS,
    ) + AdPlacement.entries.map(::placementEnabled)
}

/**
 * Whether ad requests must ask for non-personalized ads. Outside regulated regions the consent form is not
 * required and personalization follows Google's defaults; inside them it also needs the remote switch.
 */
internal fun requestsNonPersonalizedAds(inRegulatedRegion: Boolean, personalizedInRegulatedRegions: Boolean) =
    inRegulatedRegion && !personalizedInRegulatedRegions
