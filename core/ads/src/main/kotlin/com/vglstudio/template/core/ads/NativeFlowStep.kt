package com.vglstudio.template.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// TODO(T-032): skeleton for the tests written first; the code comes in the next commit.

internal object NativeFlowStepTestTags {
    const val FULL_PAGE = "nativeFlowStepFullPage"
}

internal fun stepAdStates(
    pool: PooledNativeAds,
    holder: AdPlacement,
    steps: Flow<Any>,
    logger: AppLogger,
    placementKey: String,
): Flow<NativeAdState> = TODO()

internal fun stepFullScreenTake(canRequestAds: Boolean, take: () -> LoadedNativeAd?): LoadedNativeAd? = TODO()

internal fun fullScreenStepStates(
    take: () -> LoadedNativeAd?,
    ensureLoading: () -> Unit,
    release: (LoadedNativeAd) -> Unit,
): Flow<NativeAdState> = TODO()

internal fun requireStepFormat(format: AdFormat) = Unit

internal fun requireFullScreenStepFormat(format: AdFormat) = Unit

@Composable
fun NativeStepAd(
    placement: AdPlacement,
    step: Any,
    modifier: Modifier = Modifier,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
) {
    Box(modifier)
}

@Composable
internal fun NativeStepAdHost(
    allowed: Boolean,
    step: Any,
    newStates: (steps: Flow<Any>) -> Flow<NativeAdState>,
    modifier: Modifier = Modifier,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
    adContent: @Composable (LoadedNativeAd) -> Unit = { BoundNativeAd(it, NativeAdSize.STEP) },
) {
    Box(modifier)
}

@Composable
fun NativeFullScreenStep(
    placement: AdPlacement,
    modifier: Modifier = Modifier,
    shown: Boolean = true,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
) {
    Box(modifier)
}

@Composable
internal fun NativeFullScreenStepGate(
    allowed: Boolean,
    states: Flow<NativeAdState>,
    modifier: Modifier = Modifier,
    shown: Boolean = true,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
) {
    Box(modifier)
}

@Composable
internal fun rememberStepAllowed(
    placement: AdPlacement,
    canRequestAds: StateFlow<Boolean>,
    enabled: (AdPlacement) -> Flow<Boolean>,
    unitId: (AdPlacement) -> String?,
): Boolean = false

@Composable
internal fun rememberStepReady(allowed: Boolean, ready: () -> Boolean): Boolean = false

@Composable
fun rememberNativeFullScreenStepReady(placement: AdPlacement): Boolean = true

@Composable
fun NativeFullScreenStepContent(
    assets: NativeAdAssets,
    modifier: Modifier = Modifier,
    media: @Composable BoxScope.() -> Unit = {},
    adChoices: @Composable () -> Unit = {},
) {
    NativeFullScreenAdLayout(assets, null, PreviewNativeAdBinder, modifier, media, adChoices)
}
