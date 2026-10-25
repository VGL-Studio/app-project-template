package com.vglstudio.template.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Shows the native ad of [placement] (small or medium) in a screen. Features call this, never an ads SDK directly.
 * It takes no space until ads are allowed (consent given, SDK started, placement switched on remotely). While the ad
 * loads, a placeholder keeps its height; with no ad after `ads_native_load_timeout_seconds`, the space is given back,
 * or kept empty when [keepSpaceWhenEmpty] (a placement next to an app button, so the button does not move). The ad is
 * destroyed when the slot leaves the screen. [onAdAvailabilityChange] tells the app whether the slot has an ad (D-02):
 * true when an ad arrives, false when none could be loaded, or when ads stop being allowed after an ad was shown.
 * Screenshots pass a [NativeAdCard] instead, since tests have no ads controller.
 */
@Composable
fun NativeAdSlot(
    placement: AdPlacement,
    modifier: Modifier = Modifier,
    keepSpaceWhenEmpty: Boolean = false,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
) {
    val size = requireNativeFormat(placement.format)
    val ads = LocalAdsController.current
    if (ads == null) {
        Box(modifier = modifier)
        return
    }
    val canRequestAds by ads.canRequestAds.collectAsStateWithLifecycle()
    val enabled by remember(placement) { ads.isEnabled(placement) }.collectAsStateWithLifecycle(initialValue = false)
    val unitId = remember(placement) { ads.unitId(placement) }
    NativeAdSlotGate(
        allowed = nativeSlotAllowed(canRequestAds, enabled, unitId),
        // A cold flow: nothing loads until the gate opens and the host collects it.
        states = remember(placement) { ads.nativeAd(placement) },
        size = size,
        keepSpaceWhenEmpty = keepSpaceWhenEmpty,
        onAdAvailabilityChange = onAdAvailabilityChange,
        modifier = modifier,
        adContent = { BoundNativeAd(it, size) },
    )
}

/** A native slot is drawn only with consent, the placement switched on remotely, and an ad unit (KB-1). */
internal fun nativeSlotAllowed(canRequestAds: Boolean, enabled: Boolean, unitId: String?): Boolean =
    canRequestAds && enabled && unitId != null

/**
 * Draws the slot while [allowed], nothing otherwise, and reports availability to the app: true on an ad, false on
 * no ad, never while loading; and false when [allowed] turns false after true was reported (L-061).
 */
@Composable
internal fun NativeAdSlotGate(
    allowed: Boolean,
    states: Flow<NativeAdState>,
    size: NativeAdSize,
    keepSpaceWhenEmpty: Boolean,
    onAdAvailabilityChange: (Boolean) -> Unit,
    modifier: Modifier,
    adContent: @Composable (LoadedNativeAd) -> Unit,
) {
    val currentOnChange by rememberUpdatedState(onAdAvailabilityChange)
    val reported = remember { AvailabilityReport() }
    if (!allowed) {
        SideEffect {
            if (reported.available) {
                reported.available = false
                currentOnChange(false)
            }
        }
        Box(modifier = modifier)
        return
    }
    NativeAdSlotHost(
        states = states,
        size = size,
        keepSpaceWhenEmpty = keepSpaceWhenEmpty,
        modifier = modifier,
        onStateChange = { state ->
            val available = when (state) {
                NativeAdState.Loading -> null
                is NativeAdState.Loaded -> true
                NativeAdState.Unavailable -> false
            }
            if (available != null) {
                reported.available = available
                currentOnChange(available)
            }
        },
        adContent = adContent,
    )
}

/** The last availability reported by [NativeAdSlotGate]; not state, nothing is drawn from it. */
private class AvailabilityReport {
    var available = false
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
 * again). Leaving the composition cancels the collection, which destroys the ad. [onStateChange] gets each new state.
 */
@Composable
internal fun NativeAdSlotHost(
    states: Flow<NativeAdState>,
    size: NativeAdSize,
    keepSpaceWhenEmpty: Boolean,
    modifier: Modifier,
    onStateChange: (NativeAdState) -> Unit = {},
    adContent: @Composable (LoadedNativeAd) -> Unit,
) {
    val state by states.collectAsState(initial = NativeAdState.Loading)
    val currentOnStateChange by rememberUpdatedState(onStateChange)
    // The value of this composition, not the delegate: the effect may run after a newer state arrived.
    val shown = state
    LaunchedEffect(shown) { currentOnStateChange(shown) }
    NativeAdSlotContent(shown, size, keepSpaceWhenEmpty, modifier, adContent)
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
            // A new NativeAdView for each ad: the binder registers a view once, so a swapped ad is bound only
            // in a fresh frame (refresh in place, plan T-040).
            is NativeAdState.Loaded -> key(state.ad) { adContent(state.ad) }
            NativeAdState.Unavailable ->
                if (keepSpaceWhenEmpty) Spacer(Modifier.fillMaxWidth().height(NativeAdDimens.heightOf(size)))
        }
    }
}
