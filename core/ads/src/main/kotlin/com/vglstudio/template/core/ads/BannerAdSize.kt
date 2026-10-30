package com.vglstudio.template.core.ads

import android.content.Context
import com.google.android.gms.ads.AdSize

/** The kind of adaptive size a banner format is drawn at (B-01, B-03). */
internal enum class BannerSizeKind {
    ANCHORED,
    LARGE_ANCHORED,
}

/** Skeleton (plan T-067, step 3): every banner format keeps the large anchored size of today. */
internal fun bannerSizeKind(format: AdFormat): BannerSizeKind = when (format) {
    AdFormat.BANNER, AdFormat.BANNER_COLLAPSIBLE -> BannerSizeKind.LARGE_ANCHORED
    else -> throw IllegalArgumentException("$format is not a banner format")
}

/** Skeleton (plan T-067, step 3): the large anchored size for every banner format, as today. */
internal fun bannerAdSize(context: Context, format: AdFormat, widthDp: Int): AdSize {
    bannerSizeKind(format)
    return AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp)
}
