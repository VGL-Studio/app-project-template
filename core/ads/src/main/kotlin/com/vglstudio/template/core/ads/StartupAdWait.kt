package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger

/** What [awaitStartupAd] reads at each tick; :core:ads binds it to [AdsController]. */
internal interface StartupAdProbe {
    fun nowMillis(): Long

    val consentFinished: Boolean

    val consentOpen: Boolean

    val sdkReady: Boolean

    /** The placement the keys choose now (re-read at every tick, L-061). */
    fun chosen(): AdPlacement?

    /** Reads only (F-04). */
    fun isReady(placement: AdPlacement): Boolean

    /** Loads if needed (F-04), records nothing (A-04). */
    fun ensureLoading(placement: AdPlacement)

    /** One `ad_opportunity` not_ready (A-06). */
    fun recordNotReady(placement: AdPlacement)
}

/** The limits of A-03, counted from the start of :core:ads, and the probe tick (question 4 of plan T-024). */
internal data class StartupAdLimits(
    val consentMillis: Long = 10_000,
    val sdkMillis: Long = 35_000,
    val adMillis: Long = 40_000,
    val tickMillis: Long = 250,
)

/** Returns the placement to show, or null. Deadlines count from [startedAtMillis] (question 4). */
@Suppress("UNUSED_PARAMETER")
internal suspend fun awaitStartupAd(
    probe: StartupAdProbe,
    startedAtMillis: Long,
    limits: StartupAdLimits = StartupAdLimits(),
    logger: AppLogger? = null,
): AdPlacement? = null
