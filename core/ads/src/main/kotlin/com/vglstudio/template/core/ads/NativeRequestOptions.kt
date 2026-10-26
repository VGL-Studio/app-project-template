package com.vglstudio.template.core.ads

import com.google.android.gms.ads.MediaAspectRatio
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAdOptions

/**
 * What a native ad request asks the SDK for (O-06): video starts muted, no custom video controls, one image, and the
 * aspect of the media ([MediaAspectRatio]). The only place in the module that builds the SDK's NativeAdOptions, so
 * every native request goes through it (L-072).
 */
internal data class NativeRequestOptions(
    val startMuted: Boolean,
    val customControls: Boolean,
    val multipleImages: Boolean,
    val mediaAspect: Int,
    val adChoicesCorner: Int = NativeAdOptions.ADCHOICES_TOP_RIGHT,
)

/**
 * Options of a native ad drawn in a screen: landscape media where the layout draws media, portrait for the card of a
 * grid (O-06); small draws none.
 */
internal fun nativeRequestOptions(size: NativeAdSize): NativeRequestOptions = NativeRequestOptions(
    startMuted = true,
    customControls = false,
    multipleImages = false,
    mediaAspect = when (size) {
        NativeAdSize.SMALL -> MediaAspectRatio.ANY
        NativeAdSize.MEDIUM, NativeAdSize.FULL -> MediaAspectRatio.LANDSCAPE
        NativeAdSize.CARD -> MediaAspectRatio.PORTRAIT
    },
)

/**
 * Options of the native full-screen ad: as every native ad, but any media aspect, so the tall media area does not
 * narrow the ads it can get (plan T-027, question 1).
 */
internal fun fullScreenNativeRequestOptions(): NativeRequestOptions = NativeRequestOptions(
    startMuted = true,
    customControls = false,
    multipleImages = false,
    mediaAspect = MediaAspectRatio.ANY,
)

/** Options of the collapsible layer of a bottom bar. */
internal fun collapsibleNativeRequestOptions(): NativeRequestOptions = nativeRequestOptions(NativeAdSize.MEDIUM)

/** The SDK options of this request; AdChoices in the top right corner, which every layout keeps free. */
internal fun NativeRequestOptions.toNativeAdOptions(): NativeAdOptions = NativeAdOptions.Builder()
    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
    .setMediaAspectRatio(mediaAspect)
    .setRequestMultipleImages(multipleImages)
    .setVideoOptions(
        VideoOptions.Builder()
            .setStartMuted(startMuted)
            .setCustomControlsRequested(customControls)
            .build(),
    )
    .build()
