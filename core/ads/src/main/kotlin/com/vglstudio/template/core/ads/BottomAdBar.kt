package com.vglstudio.template.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vglstudio.template.core.designsystem.theme.Spacing

/** What the bottom slot of a bottom bar draws (D-01). */
internal enum class BottomSlotKind { NATIVE, BANNER }

/** The layers of a bottom bar: its slot, and whether the collapsible layer floats above it (D-01, D-03). */
internal data class BottomAdBarLayers(val slot: BottomSlotKind, val collapsible: Boolean)

/** Whether the bar of a placement is built (D-04). */
internal fun bottomAdBarAllowed(canRequestAds: Boolean, enabled: Boolean, bannerHadAd: Boolean = false): Boolean =
    false

/** The layers of the bar (D-01, D-04). */
internal fun bottomAdBarLayers(allowed: Boolean, flags: AdsGlobalFlags): BottomAdBarLayers? = null

/** The space the content of the screen keeps at the bottom (D-04). */
internal fun bottomBarReservation(slot: BottomSlotKind?, slotHasAd: Boolean, bannerHeight: Dp): Dp = 0.dp

/** The first refresh of the native bottom slot (section 5.7). */
internal fun bottomSlotFirstRefreshDelayMillis(collapsible: Boolean): Long = NativeAdPolicy.REFRESH_INTERVAL_MILLIS

/** Sizes of the bottom bar, from design/context/AdGalleryScreen_collapsible-*.md. */
object BottomAdBarDimens {
    /** Start, end and bottom margin of the native slot and of the collapsible layer; the banner has none. */
    val SlotMargin: Dp = Spacing.md
}

/** Values for screens and tests that draw a bottom bar with fake ads. */
object BottomAdBarDefaults {
    /** The space kept for the native slot with an ad: the 80 dp slot and its 16 dp margin below. */
    val nativeReservation: Dp = 0.dp
}

internal object BottomAdBarTestTags {
    const val SLOT = "bottom_ad_bar_slot"
    const val LAYER = "bottom_ad_bar_layer"
}

/** Stateless bottom bar. */
@Composable
fun BottomAdBarLayout(
    reservation: Dp,
    slot: (@Composable (Modifier) -> Unit)?,
    layer: (@Composable (Modifier) -> Unit)?,
    modifier: Modifier = Modifier,
    slotMargin: Dp = BottomAdBarDimens.SlotMargin,
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        content(PaddingValues())
    }
}

/** The bottom bar of [placement]. */
@Composable
fun BottomAdBarScaffold(
    placement: AdPlacement,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    BottomAdBarLayout(reservation = 0.dp, slot = null, layer = null, modifier = modifier, content = content)
}

/** The bar for [layers], testable without an ads controller. */
@Composable
internal fun BottomAdBarHost(
    layers: BottomAdBarLayers?,
    nativeSlot: @Composable (firstRefreshDelayMillis: Long, onAdAvailabilityChange: (Boolean) -> Unit, Modifier) -> Unit,
    bannerSlot: @Composable (onAdAvailabilityChange: (Boolean) -> Unit, onHeight: (Dp) -> Unit, Modifier) -> Unit,
    layer: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    onLayersChange: (BottomAdBarLayers?) -> Unit = {},
    onSlotHasAdChange: (Boolean) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    BottomAdBarLayout(reservation = 0.dp, slot = null, layer = null, modifier = modifier, content = content)
}

/** The log line of a change of the layers of the bar of [placement]. */
internal fun bottomBarLayersLog(placement: AdPlacement, layers: BottomAdBarLayers?): String = ""
