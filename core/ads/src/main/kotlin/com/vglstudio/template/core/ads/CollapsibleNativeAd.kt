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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.core.designsystem.theme.Spacing

/** Whether a collapsible ad is collapsed. It can only collapse: there is no way to expand it again (ADR-0022). */
@Stable
class CollapsibleAdState internal constructor(collapsed: Boolean) {
    var collapsed by mutableStateOf(collapsed)
        private set

    fun collapse() {
        collapsed = true
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

/**
 * A collapsible native ad with fixed content, for screenshots and previews (the app shows real ads with
 * [CollapsibleNativeAdSlot]). Expanded: a 48 dp collapse button in the top start corner, then the ad as medium;
 * collapsed: the same as [NativeAdCard] small, with no way to expand again.
 */
@Composable
fun CollapsibleNativeAdCard(
    assets: NativeAdAssets,
    collapsed: Boolean,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CollapsibleNativeAdLayout(
        assets = assets,
        collapsed = collapsed,
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

/**
 * The layout of both forms. Each ad part goes through [binder], so a fake ad and a real ad bound to the SDK share it.
 * The collapse button is not an ad asset: a tap on it is not a click on the ad.
 */
@Composable
internal fun CollapsibleNativeAdLayout(
    assets: NativeAdAssets,
    collapsed: Boolean,
    onCollapse: () -> Unit,
    binder: NativeAdBinder,
    showAdChoicesMarker: Boolean,
    modifier: Modifier = Modifier,
) {
    if (collapsed) {
        Box(modifier = modifier.fillMaxWidth().testTag(CollapsibleNativeAdTestTags.AD)) {
            NativeAdLayout(assets, NativeAdSize.SMALL, binder, showAdChoicesMarker)
        }
    } else {
        ExpandedLayout(assets, onCollapse, binder, modifier)
    }
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

/** What a collapsible slot shows, independent of the SDK. */
internal sealed interface CollapsibleAdContent {
    data object Loading : CollapsibleAdContent
    data class Ready(val assets: NativeAdAssets) : CollapsibleAdContent
    data object Hidden : CollapsibleAdContent
}

/**
 * Draws [content] in the form [state] says. While loading, the placeholder has the height of the current form (342 dp
 * expanded, 72 dp collapsed), so nothing moves when the ad arrives. [onCollapsed] runs once, on the first tap.
 */
@Composable
internal fun CollapsibleNativeAdContent(
    content: CollapsibleAdContent,
    state: CollapsibleAdState,
    onCollapsed: () -> Unit,
    modifier: Modifier = Modifier,
    renderReady: @Composable (assets: NativeAdAssets, collapsed: Boolean, onCollapse: () -> Unit) -> Unit =
        { assets, collapsed, onCollapse -> CollapsibleNativeAdCard(assets, collapsed, onCollapse) },
) {
    Box(modifier = modifier) {
        when (content) {
            CollapsibleAdContent.Loading -> Box(modifier = Modifier.testTag(CollapsibleNativeAdTestTags.AD)) {
                if (state.collapsed) {
                    NativeAdPlaceholder(NativeAdSize.SMALL)
                } else {
                    NativeAdPlaceholderLayout(NativeAdSize.MEDIUM, CollapsibleNativeAdDimens.ExpandedHeight)
                }
            }
            is CollapsibleAdContent.Ready -> renderReady(content.assets, state.collapsed) {
                if (!state.collapsed) {
                    state.collapse()
                    onCollapsed()
                }
            }
            CollapsibleAdContent.Hidden -> Unit
        }
    }
}

/**
 * Shows the collapsible native ad of [placement] (format [AdFormat.NATIVE_COLLAPSIBLE]). It opens expanded; a tap on
 * its collapse button turns it into the small form, and it never expands again while the screen entry lives, even
 * when a new ad loads (KB-4, ADR-0022). The calling screen places it at the bottom; it only takes the width it is
 * given. Like [NativeAdSlot], it takes no space until ads are allowed, keeps its height while loading, gives the space
 * back with no ad, and destroys the ad when it leaves the screen.
 */
@Composable
fun CollapsibleNativeAdSlot(placement: AdPlacement, modifier: Modifier = Modifier) {
    require(placement.format == AdFormat.NATIVE_COLLAPSIBLE) {
        "${placement.format} is not drawn by CollapsibleNativeAdSlot"
    }
    // First, outside any branch or key that depends on the ad: a new ad never expands it again.
    val state = rememberCollapsibleAdState()
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
    // Collected while in composition; leaving it cancels the collection, which destroys the ad (like NativeAdSlot).
    val adState by remember(placement) { ads.nativeAd(placement) }.collectAsState(initial = NativeAdState.Loading)
    val current = adState
    val content = when (current) {
        NativeAdState.Loading -> CollapsibleAdContent.Loading
        is NativeAdState.Loaded -> CollapsibleAdContent.Ready(current.ad.assets)
        NativeAdState.Unavailable -> CollapsibleAdContent.Hidden
    }
    CollapsibleNativeAdContent(
        content = content,
        state = state,
        onCollapsed = { ads.onNativeCollapsed(placement) },
        modifier = modifier,
        renderReady = { assets, collapsed, onCollapse ->
            if (current is NativeAdState.Loaded) {
                // One NativeAdView per ad; collapsing keeps it and binds it again without the media view.
                key(current.ad) {
                    BoundNativeAd(current.ad) { binder ->
                        CollapsibleNativeAdLayout(assets, collapsed, onCollapse, binder, showAdChoicesMarker = false)
                    }
                }
            }
        },
    )
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
            CollapsibleNativeAdCard(assets, collapsed = false, onCollapse = {})
            CollapsibleNativeAdCard(assets, collapsed = true, onCollapse = {})
        }
    }
}
