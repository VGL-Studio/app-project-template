package com.vglstudio.template.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Shows the native ad of [placement] (small or medium) in a screen. Features call this, never an ads SDK directly.
 * It takes no space until ads are allowed (consent given, SDK started, placement switched on remotely). While the ad
 * loads, a placeholder keeps its height; with no ad after `ads_native_load_timeout_seconds`, the space is given back,
 * or kept empty when [keepSpaceWhenEmpty] (a placement next to an app button, so the button does not move). The ad is
 * destroyed when the slot leaves the screen. Screenshots pass a [NativeAdCard] instead, since tests have no ads
 * controller.
 */
@Composable
fun NativeAdSlot(placement: AdPlacement, modifier: Modifier = Modifier, keepSpaceWhenEmpty: Boolean = false) {
    val size = requireNativeFormat(placement.format)
    val ads = LocalAdsController.current
    if (ads == null) {
        Box(modifier = modifier)
        return
    }
    val canRequestAds by ads.canRequestAds.collectAsStateWithLifecycle()
    val enabled by remember(placement) { ads.isEnabled(placement) }.collectAsStateWithLifecycle(initialValue = false)
    val unitId = remember(placement) { ads.unitId(placement) }
    if (!canRequestAds || !enabled || unitId == null) {
        Box(modifier = modifier)
        return
    }
    NativeAdSlotHost(
        states = remember(placement) { ads.nativeAd(placement) },
        size = size,
        keepSpaceWhenEmpty = keepSpaceWhenEmpty,
        modifier = modifier,
        adContent = { BoundNativeAd(it, size) },
    )
}

/** The size a native placement is drawn at; other formats are not drawn by [NativeAdSlot]. */
internal fun requireNativeFormat(format: AdFormat): NativeAdSize = when (format) {
    AdFormat.NATIVE_SMALL -> NativeAdSize.SMALL
    AdFormat.NATIVE_MEDIUM -> NativeAdSize.MEDIUM
    // Not exhaustive on purpose: new formats (collapsible, full screen) do not belong in this slot.
    else -> throw IllegalArgumentException("$format is not drawn by NativeAdSlot")
}

/**
 * Collects [states] while in composition (not by lifecycle: the ad loads again only when the screen is entered
 * again). Leaving the composition cancels the collection, which destroys the ad.
 */
@Composable
internal fun NativeAdSlotHost(
    states: Flow<NativeAdState>,
    size: NativeAdSize,
    keepSpaceWhenEmpty: Boolean,
    modifier: Modifier,
    adContent: @Composable (LoadedNativeAd) -> Unit,
) {
    val state by states.collectAsState(initial = NativeAdState.Loading)
    NativeAdSlotContent(state, size, keepSpaceWhenEmpty, modifier, adContent)
}

/** What the slot draws for [state]. */
@Composable
internal fun NativeAdSlotContent(
    state: NativeAdState,
    size: NativeAdSize,
    keepSpaceWhenEmpty: Boolean,
    modifier: Modifier,
    adContent: @Composable (LoadedNativeAd) -> Unit,
) {
    Box(modifier = modifier) {
        when (state) {
            NativeAdState.Loading -> NativeAdPlaceholder(size)
            is NativeAdState.Loaded -> adContent(state.ad)
            NativeAdState.Unavailable ->
                if (keepSpaceWhenEmpty) Spacer(Modifier.fillMaxWidth().height(NativeAdDimens.heightOf(size)))
        }
    }
}
