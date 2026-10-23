package com.example.ads

enum class AdPlacement(val key: String, val format: AdFormat) {
    SAMPLE_BOTTOM_BANNER("sample_bottom_banner", AdFormat.BANNER),
    SAMPLE_NATIVE_SMALL("sample_native_small", AdFormat.NATIVE_SMALL),
    HOME_FULL_SCREEN("home_full_screen", AdFormat.NATIVE_FULL_SCREEN),
    GALLERY_NATIVE_MEDIUM("gallery_native_medium", AdFormat.NATIVE_MEDIUM),
    GALLERY_INTERSTITIAL("gallery_interstitial", AdFormat.INTERSTITIAL),
}
