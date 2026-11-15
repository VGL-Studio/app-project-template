package com.vglstudio.template.core.ads

/**
 * Platforms an interstitial of [placement] may load and show from, in order (KB-31, question 41; plan T-069): the
 * startup interstitial (`open_app_inter`) is AdMob only, like the startup app open ad; any other one follows [order].
 * A platform without its ad unit is dropped, like [bannerPlatforms].
 */
internal fun interstitialPlatforms(
    placement: AdPlacement,
    order: MediationOrder,
    hasAdmobUnit: Boolean,
    hasMaxUnit: Boolean,
): List<AdPlatform> {
    val wanted = if (placement == AdPlacement.OPEN_APP_INTER) listOf(AdPlatform.ADMOB) else order.platforms
    return wanted.filter {
        when (it) {
            AdPlatform.ADMOB -> hasAdmobUnit
            AdPlatform.MAX -> hasMaxUnit
        }
    }
}

/** The first of [platforms] holding an ad now, or null (ADR-0045, section 2.2). */
internal fun readyPlatform(platforms: List<AdPlatform>, admobReady: Boolean, maxReady: Boolean): AdPlatform? =
    platforms.firstOrNull {
        when (it) {
            AdPlatform.ADMOB -> admobReady
            AdPlatform.MAX -> maxReady
        }
    }

/** The load state of a turn: Ready when a platform holds an ad, else the state of the first platform; Idle when none. */
internal fun turnLoadState(
    platforms: List<AdPlatform>,
    admob: FullScreenLoadState,
    max: FullScreenLoadState,
): FullScreenLoadState {
    val states = platforms.map { if (it == AdPlatform.ADMOB) admob else max }
    return when {
        FullScreenLoadState.Ready in states -> FullScreenLoadState.Ready
        else -> states.firstOrNull() ?: FullScreenLoadState.Idle
    }
}

/** For the preload plan (S-22): Loading while any of [platforms] loads, else [turnLoadState]. */
internal fun preloadSettleState(
    platforms: List<AdPlatform>,
    admob: FullScreenLoadState,
    max: FullScreenLoadState,
): FullScreenLoadState {
    val loading = platforms.any { (if (it == AdPlatform.ADMOB) admob else max) == FullScreenLoadState.Loading }
    return if (loading) FullScreenLoadState.Loading else turnLoadState(platforms, admob, max)
}
