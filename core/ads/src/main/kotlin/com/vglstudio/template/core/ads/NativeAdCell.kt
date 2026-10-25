package com.vglstudio.template.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow

/** Skeleton (T-028, step 3). */
@Composable
fun NativeAdCell(
    placement: AdPlacement,
    modifier: Modifier = Modifier,
    viewed: Boolean = true,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
) {
    Box(modifier = modifier)
}

/** Skeleton (T-028, step 3). */
internal fun requireCellFormat(format: AdFormat): NativeAdSize = NativeAdSize.MEDIUM

/** Skeleton (T-028, step 3). */
@Composable
internal fun NativeAdCellHost(
    states: Flow<NativeAdState>,
    size: NativeAdSize,
    viewed: Boolean,
    keepSpaceWhenEmpty: Boolean,
    onOpportunity: (result: String) -> Unit,
    modifier: Modifier = Modifier,
    ctaLarge: Boolean = false,
    placeholderWhenEmpty: Boolean = true,
    contentModifier: Modifier = Modifier,
    onStateChange: (NativeAdState) -> Unit = {},
    adContent: @Composable (LoadedNativeAd) -> Unit,
) {
    Box(modifier = modifier)
}

/** Skeleton (T-028, step 3). */
fun PagerState.isPageSettled(page: Int): Boolean = false
