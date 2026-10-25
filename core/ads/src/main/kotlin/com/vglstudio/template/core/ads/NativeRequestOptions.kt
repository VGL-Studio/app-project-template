package com.vglstudio.template.core.ads

import com.google.android.gms.ads.nativead.NativeAdOptions

/** Skeleton (plan T-027, step 3). */
internal data class NativeRequestOptions(
    val startMuted: Boolean,
    val customControls: Boolean,
    val multipleImages: Boolean,
    val mediaAspect: Int,
)

internal fun nativeRequestOptions(size: NativeAdSize): NativeRequestOptions =
    NativeRequestOptions(startMuted = false, customControls = true, multipleImages = true, mediaAspect = 0)

internal fun fullScreenNativeRequestOptions(): NativeRequestOptions =
    NativeRequestOptions(startMuted = false, customControls = true, multipleImages = true, mediaAspect = 0)

internal fun NativeRequestOptions.toNativeAdOptions(): NativeAdOptions = NativeAdOptions.Builder().build()
