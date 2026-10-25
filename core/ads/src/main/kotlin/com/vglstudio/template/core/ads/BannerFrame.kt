package com.vglstudio.template.core.ads

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** What the slot of a banner draws (B-01, B-04, B-06). */
internal enum class BannerFrame {
    /** Nothing, no space, no ad view. */
    NONE,

    /** The frame with the ad view, sending requests. */
    ACTIVE,

    /** The frame of a banner that had an ad, kept at its height while consent is withdrawn; the ad is hidden. */
    KEPT_EMPTY,
}

@Suppress("UNUSED_PARAMETER")
internal fun bannerFrame(gateOpen: Boolean, enabled: Boolean, unitId: String?, hadAd: Boolean): BannerFrame =
    BannerFrame.NONE

@Suppress("UNUSED_PARAMETER")
internal fun bannerShouldRun(visible: Boolean, frame: BannerFrame): Boolean = false

internal fun Lifecycle.resumedFlow(): Flow<Boolean> = flowOf(false)
