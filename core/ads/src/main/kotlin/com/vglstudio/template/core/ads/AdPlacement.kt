package com.vglstudio.template.core.ads

/** How an ad is shown. A placement has exactly one format. */
enum class AdFormat {
    /** Shown inside a screen with [AdSlot]. */
    BANNER,

    /** Covers the whole screen, shown at a natural break with [rememberShowFullScreenAd]. */
    INTERSTITIAL,

    /** Shown inside a screen with [NativeAdSlot], no media: icon, headline, "Ad" label, call to action. */
    NATIVE_SMALL,

    /** Shown inside a screen with [NativeAdSlot], with media (image or video); replaces MREC. */
    NATIVE_MEDIUM,

    /** Medium that the user collapses to small, shown with [CollapsibleNativeAdSlot]; never expands again. */
    NATIVE_COLLAPSIBLE,

    /** Covers the whole screen at a natural break, shown with [rememberShowFullScreenAd]. */
    NATIVE_FULL_SCREEN,
}

/** Formats that cover the whole screen; they share the frequency rules of full-screen ads (T-009). */
internal val AdFormat.isFullScreen: Boolean
    get() = this == AdFormat.INTERSTITIAL || this == AdFormat.NATIVE_FULL_SCREEN

/**
 * Every ad placement of the app, declared in one place (ADR-0004).
 * A placement is a business decision: it must come from an approved spec.
 * Adding, removing or moving a placement is a high-risk change (risk-scoring.md, blocking point 1).
 * Each placement shows ads only once the Remote Config key `ads_<key>_enabled` is switched on (off by default).
 * Production builds need a real ad unit for every placement except the staging-only ones (see [ConfiguredAdUnitIds]).
 */
enum class AdPlacement(val key: String, val format: AdFormat) {
    SAMPLE_NATIVE_SMALL("sample_native_small", AdFormat.NATIVE_SMALL),

    // Staging ad gallery only (plan T-014): one placement per format, see [stagingOnly].
    GALLERY_NATIVE_SMALL("gallery_native_small", AdFormat.NATIVE_SMALL),
    GALLERY_NATIVE_MEDIUM("gallery_native_medium", AdFormat.NATIVE_MEDIUM),
    GALLERY_INLIST_SMALL("gallery_inlist_small", AdFormat.NATIVE_SMALL),
    GALLERY_INLIST_MEDIUM("gallery_inlist_medium", AdFormat.NATIVE_MEDIUM),
    GALLERY_NATIVE_COLLAPSIBLE("gallery_native_collapsible", AdFormat.NATIVE_COLLAPSIBLE),
    GALLERY_INTERSTITIAL("gallery_interstitial", AdFormat.INTERSTITIAL),
    GALLERY_NATIVE_FULL_SCREEN("gallery_native_full_screen", AdFormat.NATIVE_FULL_SCREEN),
    ;

    /**
     * Exists only for the staging ad gallery: production has no ad unit for it and never shows it.
     * scripts/release.sh skips the same prefix.
     */
    val stagingOnly: Boolean get() = key.startsWith(STAGING_ONLY_KEY_PREFIX)

    private companion object {
        const val STAGING_ONLY_KEY_PREFIX = "gallery_"
    }
}
