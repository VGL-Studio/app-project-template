package com.vglstudio.template.core.ads

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.core.designsystem.theme.Spacing

/**
 * A native ad that covers the whole screen (KB-5), as in design/screens/NativeFullScreenAdScreen_default_*.png: a
 * 48 dp close button in the top left corner, visible and enabled from the first frame (no countdown), AdChoices in the
 * top right corner ([adChoices]; the SDK draws it on real ads), a large [media] area, then the head of the ad, the
 * body and a wide call to action. The app shows it with [rememberShowFullScreenAd]; the gallery screen (T-014) and
 * tests draw it with fixed content.
 */
@Composable
fun NativeFullScreenAdScreen(
    assets: NativeAdAssets,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    media: @Composable BoxScope.() -> Unit = {},
    adChoices: @Composable () -> Unit = {},
) {
    NativeFullScreenAdLayout(assets, onClose, PreviewNativeAdBinder, modifier, media, adChoices)
}

/** Sizes of the native full-screen ad, from design/context/NativeFullScreenAdScreen_default_light.md. */
internal object NativeFullScreenAdDimens {
    val CloseButtonSize = 48.dp
    val CloseIconSize = 24.dp
    val TopBarHeight = 48.dp
    val HeaderHeight = 48.dp
    val IconSize = 48.dp
    val CallToActionHeight = 48.dp

    /** The media shrinks first on short screens, never below the SDK's minimum. */
    val MediaMinHeight = 120.dp
}

internal object NativeFullScreenAdTestTags {
    const val CLOSE = "nativeFullScreenClose"
    const val MEDIA = "nativeFullScreenMedia"
    const val CTA = "nativeFullScreenCta"
}

private const val BODY_MAX_LINES = 3

/**
 * The layout of [NativeFullScreenAdScreen]. Each ad part goes through [binder], so a fake ad and a real ad bound to
 * the SDK share it; a real ad puts the SDK's MediaView in [media].
 */
@Composable
internal fun NativeFullScreenAdLayout(
    assets: NativeAdAssets,
    onClose: () -> Unit,
    binder: NativeAdBinder,
    modifier: Modifier = Modifier,
    media: @Composable BoxScope.() -> Unit = {},
    adChoices: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = Spacing.md, top = Spacing.lg, end = Spacing.md, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(NativeFullScreenAdDimens.TopBarHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CloseButton(onClose)
            adChoices()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = NativeFullScreenAdDimens.MediaMinHeight)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .testTag(NativeFullScreenAdTestTags.MEDIA),
            content = media,
        )
        AdHeader(assets, binder)
        assets.body?.let { body ->
            NativeAdBody(body, binder, maxLines = BODY_MAX_LINES, modifier = Modifier.fillMaxWidth())
        }
        assets.callToAction?.let { AdCallToAction(it, binder) }
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
    IconButton(
        onClick = onClose,
        modifier = Modifier
            .size(NativeFullScreenAdDimens.CloseButtonSize)
            .clip(CircleShape)
            .testTag(NativeFullScreenAdTestTags.CLOSE),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_ads_close),
            contentDescription = stringResource(R.string.ads_close_full_screen),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(NativeFullScreenAdDimens.CloseIconSize),
        )
    }
}

/** Icon, headline, then the "Ad" label with the advertiser. */
@Composable
private fun AdHeader(assets: NativeAdAssets, binder: NativeAdBinder) {
    Row(
        modifier = Modifier.fillMaxWidth().height(NativeFullScreenAdDimens.HeaderHeight),
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AdIcon(assets.icon, binder)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            binder.Asset(NativeAdAsset.HEADLINE, Modifier.fillMaxWidth()) {
                Text(
                    text = assets.headline,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            NativeAdBadgeLine(assets, binder, bodyWhenNoAdvertiser = false)
        }
    }
}

@Composable
private fun AdIcon(icon: ImageBitmap?, binder: NativeAdBinder) {
    val iconModifier = Modifier.size(NativeFullScreenAdDimens.IconSize).clip(MaterialTheme.shapes.small)
    binder.Asset(NativeAdAsset.ICON, Modifier.size(NativeFullScreenAdDimens.IconSize)) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, contentScale = ContentScale.Crop, modifier = iconModifier)
        } else {
            Box(modifier = iconModifier.background(MaterialTheme.colorScheme.secondaryContainer))
        }
    }
}

/** A plain box, not a Button, so a touch reaches the SDK's call-to-action view. */
@Composable
private fun AdCallToAction(text: String, binder: NativeAdBinder) {
    binder.Asset(
        NativeAdAsset.CALL_TO_ACTION,
        Modifier
            .fillMaxWidth()
            .height(NativeFullScreenAdDimens.CallToActionHeight)
            .testTag(NativeFullScreenAdTestTags.CTA),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(horizontal = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Composable
private fun NativeFullScreenAdScreenPreview() {
    AppTheme {
        NativeFullScreenAdScreen(
            assets = NativeAdAssets(
                headline = "Headline of the ad",
                advertiser = "Advertiser",
                body = "Body text of the ad. Up to three lines; the ad decides the words, the app decides the style.",
                callToAction = "Install",
                icon = null,
                hasMedia = true,
            ),
            onClose = {},
        )
    }
}
