package com.vglstudio.template.core.ads

/** How an ad is shown. A placement has exactly one format. */
enum class AdFormat {
    /** Shown inside a screen with [AdSlot]. */
    BANNER,

    /** Covers the whole screen, shown at a natural break with [rememberShowInterstitial]. */
    INTERSTITIAL,

    /** Shown inside a screen with [NativeAdSlot], no media: icon, headline, "Ad" label, call to action. */
    NATIVE_SMALL,

    /** Shown inside a screen with [NativeAdSlot], with media (image or video); replaces MREC. */
    NATIVE_MEDIUM,

    /** Medium that the user collapses to small, shown with [CollapsibleNativeAdSlot]; never expands again. */
    NATIVE_COLLAPSIBLE,
}

/**
 * Every ad placement of the app, declared in one place (ADR-0004).
 * A placement is a business decision: it must come from an approved spec.
 * Adding, removing or moving a placement is a high-risk change (risk-scoring.md, blocking point 1).
 * Each placement shows ads only once the Remote Config key `ads_<key>_enabled` is switched on (off by default).
 * Production builds need a real ad unit for every placement (see [ConfiguredAdUnitIds]).
 */
enum class AdPlacement(val key: String, val format: AdFormat) {
    SAMPLE_NATIVE_SMALL("sample_native_small", AdFormat.NATIVE_SMALL),
}
