package com.vglstudio.template.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Skeleton for the tests written first (plan T-092, step 3): no behaviour yet.

internal fun isInContentPlacement(placement: AdPlacement): Boolean = false

internal fun inContentBlock(anyOn: Boolean?, blockedThisEntry: Boolean): Boolean? = null

internal fun mergeInContentBlocks(outer: Boolean?, own: Boolean?): Boolean? = null

internal fun isFloatingBanner(format: AdFormat, inBottomBar: Boolean): Boolean = false

internal fun inContentBlockLog(placements: List<AdPlacement>): String = ""

internal fun AdsController.inContentAdsOn(placements: List<AdPlacement>): Flow<Boolean> = flowOf(false)

internal val LocalInContentAdBlock = compositionLocalOf<Boolean?> { false }

@Composable
fun InContentAds(placements: List<AdPlacement>, content: @Composable () -> Unit) {
    content()
}
