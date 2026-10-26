package com.vglstudio.template.core.ads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.core.designsystem.theme.Spacing
import kotlinx.coroutines.flow.Flow

/** Whether a collapsible ad is collapsed, and since when ([collapsedAtMillis], monotonic clock). */
@Stable
class CollapsibleAdState internal constructor(collapsed: Boolean, collapsedAtMillis: Long? = null) {
    var collapsed by mutableStateOf(collapsed)
        private set

    var collapsedAtMillis by mutableStateOf(collapsedAtMillis)
        private set

    fun collapse(nowMillis: Long) {
        collapsed = true
    }

    internal fun expand() {
    }

    internal companion object {
        val Saver: Saver<CollapsibleAdState, Boolean> = Saver(save = { it.collapsed }, restore = ::CollapsibleAdState)
    }
}

/**
 * The collapsed state of a collapsible ad, kept for the screen entry: a configuration change or coming back to the
 * screen from the back stack keeps it collapsed; a new entry of the screen starts expanded.
 */
@Composable
fun rememberCollapsibleAdState(): CollapsibleAdState =
    rememberSaveable(saver = CollapsibleAdState.Saver) { CollapsibleAdState(collapsed = false) }

/** A collapsible native ad with fixed content, for screenshots and previews. */
@Composable
fun CollapsibleNativeAdCard(assets: NativeAdAssets, onCollapse: () -> Unit, modifier: Modifier = Modifier) {
    CollapsibleNativeAdLayout(
        assets = assets,
        onCollapse = onCollapse,
        binder = PreviewNativeAdBinder,
        showAdChoicesMarker = true,
        modifier = modifier,
    )
}

/** Sizes of the expanded form, from design/context/AdGalleryScreen_collapsible-expanded_light.md. */
internal object CollapsibleNativeAdDimens {
    val ExpandedHeight = 342.dp
    val ExpandedMediaHeight = 160.dp
    val CollapseButtonSize = 48.dp
}

internal object CollapsibleNativeAdTestTags {
    const val AD = "collapsible_ad"
    const val MEDIA = "collapsible_ad_media"
    const val COLLAPSE = "collapsible_ad_collapse"
}

/** Top margin inside the border; the collapse bar's own height keeps the AdChoices corner free. */
private val ExpandedInnerTop = NativeAdDimens.BorderWidth + Spacing.sm

/** Bottom margin inside the border, so the parts add up to the 342 dp of the design. */
private val ExpandedInnerBottom = NativeAdDimens.BorderWidth + NativeAdDimens.InnerPadding

/** The layout of the open layer; each ad part goes through [binder]. */
@Composable
internal fun CollapsibleNativeAdLayout(
    assets: NativeAdAssets,
    onCollapse: () -> Unit,
    binder: NativeAdBinder,
    showAdChoicesMarker: Boolean,
    modifier: Modifier = Modifier,
) {
    ExpandedLayout(assets, onCollapse, binder, modifier)
}

/**
 * Expanded: collapse bar (button only, the end of the bar left to the SDK's AdChoices), header, media, call to
 * action. AdChoices is drawn by the SDK, never in Compose (design README, section 3).
 */
@Composable
private fun ExpandedLayout(assets: NativeAdAssets, onCollapse: () -> Unit, binder: NativeAdBinder, modifier: Modifier) {
    NativeAdFrame(
        modifier = modifier
            .fillMaxWidth()
            .height(CollapsibleNativeAdDimens.ExpandedHeight)
            .testTag(CollapsibleNativeAdTestTags.AD),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = NativeAdDimens.InnerPadding,
                    top = ExpandedInnerTop,
                    end = NativeAdDimens.InnerPadding,
                    bottom = ExpandedInnerBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(CollapsibleNativeAdDimens.CollapseButtonSize),
                contentAlignment = Alignment.CenterStart,
            ) {
                CollapseButton(onCollapse)
            }
            NativeAdHeader(assets, binder)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CollapsibleNativeAdDimens.ExpandedMediaHeight)
                    .testTag(CollapsibleNativeAdTestTags.MEDIA),
            ) {
                NativeAdMediaArea(binder, Modifier.fillMaxSize())
            }
            assets.callToAction?.let { NativeAdCallToAction(it, binder, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun CollapseButton(onCollapse: () -> Unit) {
    IconButton(
        onClick = onCollapse,
        modifier = Modifier
            .size(CollapsibleNativeAdDimens.CollapseButtonSize)
            .testTag(CollapsibleNativeAdTestTags.COLLAPSE),
    ) {
        Icon(
            painter = painterResource(R.drawable.ads_ic_collapse),
            contentDescription = stringResource(R.string.ads_collapse),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The collapsible layer of a bottom bar, testable without an ads controller. */
@Composable
internal fun CollapsibleNativeAdLayerHost(
    states: Flow<NativeAdState>,
    expandAfterSeconds: Long,
    clock: MonotonicClock,
    state: CollapsibleAdState,
    onCollapsed: () -> Unit,
    onReexpanded: () -> Unit,
    modifier: Modifier = Modifier,
    allowed: Boolean = true,
    renderAd: @Composable (ad: LoadedNativeAd, onCollapse: () -> Unit) -> Unit = { ad, onCollapse ->
        BoundNativeAd(ad) { binder ->
            CollapsibleNativeAdLayout(ad.assets, onCollapse, binder, showAdChoicesMarker = false)
        }
    },
) {
    if (!allowed) return
    val adState by states.collectAsState(initial = NativeAdState.Loading)
    val current = adState
    if (current is NativeAdState.Loaded) {
        Box(modifier = modifier) {
            key(current.ad) {
                renderAd(current.ad) {
                    state.collapse(clock.nowMillis())
                    onCollapsed()
                }
            }
        }
    }
}

/** The collapsible layer of the bottom bar of [placement]. */
@Composable
internal fun CollapsibleNativeAdLayer(placement: AdPlacement, expandAfterSeconds: Long, modifier: Modifier = Modifier) {
}

@Preview
@Composable
private fun CollapsibleNativeAdCardPreview() {
    val assets = NativeAdAssets(
        headline = "Headline of the ad",
        advertiser = "Advertiser",
        body = null,
        callToAction = "Install",
        icon = null,
        hasMedia = true,
    )
    AppTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            CollapsibleNativeAdCard(assets, onCollapse = {})
        }
    }
}
