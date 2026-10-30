package com.vglstudio.template.core.ads

import android.content.Context
import com.google.android.gms.ads.AdSize

/** The adaptive size a banner format is drawn at; the one place that picks a banner's size (plan T-067). */
internal enum class BannerSizeKind {
    /** The anchored adaptive size of the current orientation, as JBase (B-01, ADR-0038). */
    ANCHORED,

    /** The large anchored adaptive size (B-03). */
    LARGE_ANCHORED,
}

/**
 * [AdFormat.BANNER] is drawn at the anchored adaptive size (B-01, ADR-0038), [AdFormat.BANNER_COLLAPSIBLE] at the
 * large one (B-03); other formats throw.
 */
internal fun bannerSizeKind(format: AdFormat): BannerSizeKind = when (format) {
    AdFormat.BANNER -> BannerSizeKind.ANCHORED
    AdFormat.BANNER_COLLAPSIBLE -> BannerSizeKind.LARGE_ANCHORED
    else -> throw IllegalArgumentException("$format is not a banner format")
}

/**
 * The SDK size of a banner of [format] in a slot [widthDp] wide, from [bannerSizeKind].
 *
 * The anchored size is the one JBase uses (`AdLayoutUtils.getAdaptiveBannerSize`). play-services-ads 25.5.0 marks
 * `getCurrentOrientationAnchoredAdaptiveBannerAdSize` deprecated with no replacement for this size: the replacement
 * Google names is the large size. If an SDK upgrade removes the function, B-01 has to be decided again.
 */
@Suppress("DEPRECATION")
internal fun bannerAdSize(context: Context, format: AdFormat, widthDp: Int): AdSize = when (bannerSizeKind(format)) {
    BannerSizeKind.ANCHORED -> AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    BannerSizeKind.LARGE_ANCHORED -> AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp)
}
