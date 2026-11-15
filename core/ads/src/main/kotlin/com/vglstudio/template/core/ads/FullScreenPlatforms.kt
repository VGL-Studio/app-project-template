package com.vglstudio.template.core.ads

/** Platforms an interstitial of [placement] may load and show from, in order (KB-31, question 41; plan T-069). */
internal fun interstitialPlatforms(
    placement: AdPlacement,
    order: MediationOrder,
    hasAdmobUnit: Boolean,
    hasMaxUnit: Boolean,
): List<AdPlatform> = emptyList()

/** The first of [platforms] holding an ad now, or null. */
internal fun readyPlatform(platforms: List<AdPlatform>, admobReady: Boolean, maxReady: Boolean): AdPlatform? = null

/** The load state of a turn: Ready when a platform holds an ad, else the state of the first platform; Idle when none. */
internal fun turnLoadState(
    platforms: List<AdPlatform>,
    admob: FullScreenLoadState,
    max: FullScreenLoadState,
): FullScreenLoadState = FullScreenLoadState.Idle

/** For the preload plan (S-22): Loading while any of [platforms] loads, else [turnLoadState]. */
internal fun preloadSettleState(
    platforms: List<AdPlatform>,
    admob: FullScreenLoadState,
    max: FullScreenLoadState,
): FullScreenLoadState = FullScreenLoadState.Idle
