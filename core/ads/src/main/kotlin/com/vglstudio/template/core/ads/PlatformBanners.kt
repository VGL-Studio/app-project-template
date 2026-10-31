package com.vglstudio.template.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

// Stubs of T-071 (step 3 of docs/auto-run.md): the tests compile and fail on their assertions.

internal fun bannerPlatforms(
    format: AdFormat,
    order: MediationOrder,
    admobUnit: String?,
    maxUnit: String?,
): List<AdPlatform> = emptyList()

internal fun bannerFrameHeightDp(platforms: List<AdPlatform>, admobHeightDp: Int, maxHeightDp: Int): Int = 0

internal fun bannerSlotUnitKey(platforms: List<AdPlatform>, admobUnit: String?, maxUnit: String?): String? = null

internal const val MAX_NOT_READY = -9000

internal class OrderedBannerAdView(
    private val children: List<Pair<AdPlatform, BannerAdView>>,
    private val maxReady: () -> Boolean,
    private val logger: AppLogger,
    private val placementKey: String,
) : BannerAdView {
    override val results: Flow<BannerLoadResult> = MutableSharedFlow()

    override fun send() = Unit

    override fun pause() = Unit

    override fun resume() = Unit

    override fun destroy() = Unit

    @Composable
    override fun Content(showAd: Boolean, modifier: Modifier) = Unit
}
