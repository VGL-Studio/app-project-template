package com.vglstudio.template.core.ads

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.core.designsystem.theme.Spacing
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Size of a native ad drawn inside a screen (ads-guide.md, section 4). [FULL] is the full native ad of
 * ads-business-rules.md 5.7; no ad format draws it yet (plan T-027, Q2).
 */
enum class NativeAdSize { SMALL, MEDIUM, FULL }

/**
 * What a native ad shows; the SDK fills it in the app, tests and previews use fixed values. [starRating] is the
 * store rating of the advertised app, when the ad has one.
 */
data class NativeAdAssets(
    val headline: String,
    val advertiser: String?,
    val body: String?,
    val callToAction: String?,
    val icon: ImageBitmap?,
    val hasMedia: Boolean,
    val starRating: Double? = null,
)

/**
 * A native ad with fixed content, for screenshots and previews of screens that show a native ad. The app shows real
 * ads with [NativeAdSlot], which uses the same layout. A 16 dp marker shows where the SDK draws AdChoices; real ads
 * never draw it (ads-guide.md, section 2).
 */
@Composable
fun NativeAdCard(assets: NativeAdAssets, size: NativeAdSize, modifier: Modifier = Modifier) {
    NativeAdLayout(assets, size, PreviewNativeAdBinder, showAdChoicesMarker = true, modifier = modifier)
}

/**
 * The loading state of a native ad: same frame and height as the ad, so nothing moves when the ad arrives (KB-1).
 * [animated] off keeps screenshots independent of time.
 */
@Composable
fun NativeAdPlaceholder(size: NativeAdSize, modifier: Modifier = Modifier, animated: Boolean = true) {
    NativeAdPlaceholderLayout(size, NativeAdDimens.heightOf(size), modifier, animated)
}

/**
 * Sizes of the native ad components, from docs/features/ads/design/context/ (design README, section 3) and
 * ads-business-rules.md 5.7.
 */
internal object NativeAdDimens {
    /** Small native: the drawn card, its placeholder and the space kept for the bottom slot (D-02, D-04). */
    val BottomSlotHeight = 80.dp
    val MediumHeight = 386.dp
    val BorderWidth = 1.dp

    /** Inner margin of the frame; outside the design system scale (design README, section 3). */
    val InnerPadding = 12.dp

    /** Gap between the parts of an ad; outside the design system scale. */
    val PartGap = 12.dp

    /** Gap between the headline and the line below it; outside the design system scale. */
    val HeadlineGap = 2.dp

    /** Gap between the "Ad" label and the text next to it; outside the design system scale. */
    val BadgeGap = 6.dp
    val BadgeCorner = 4.dp
    val IconSize = 40.dp
    val SmallIconSize = 36.dp
    val FullIconSize = 40.dp
    val AssetCorner = 8.dp
    val CallToActionHeight = 36.dp
    val SmallCallToActionHeight = 34.dp

    /** Short labels ("Install") still get the design's 80 dp, the width of the loading block. */
    val CallToActionMinWidth = 80.dp
    val MediaHeight = 196.dp
    val FullMediaHeight = 200.dp
    val BodyHeight = 40.dp
    val AdChoicesSize = 16.dp
    val StarRowHeight = 16.dp

    /**
     * Full native: fixed, so its placeholder matches it: 1 + 24 top, head 40, media 200, body 40 (two lines), call to
     * action 36, star row 16, four 12 dp gaps between these five parts, 12 + 1 bottom.
     */
    val FullHeight = 418.dp
    val FullPlaceholderHeight = FullHeight

    /** Loading bars for the two text lines, and the call-to-action block. */
    val LineHeight = 10.dp
    val LongLineWidth = 180.dp
    val ShortLineWidth = 120.dp
    val CallToActionPlaceholderWidth = CallToActionMinWidth

    fun heightOf(size: NativeAdSize): Dp = when (size) {
        NativeAdSize.SMALL -> BottomSlotHeight
        NativeAdSize.MEDIUM -> MediumHeight
        NativeAdSize.FULL -> FullPlaceholderHeight
    }
}

/** Text sizes of the small native outside the type scale (ads-business-rules.md 5.7, design README, T-035). */
private val SmallCallToActionFontSize = 13.sp
private val SmallBadgeFontSize = 10.sp

internal object NativeAdTestTags {
    const val CARD = "native_ad_card"
    const val PLACEHOLDER = "native_ad_placeholder"
    const val ICON = "native_ad_icon"
    const val HEADLINE = "native_ad_headline"
    const val BADGE = "native_ad_badge"
    const val MEDIA = "native_ad_media"
    const val CTA = "native_ad_cta"
    const val AD_CHOICES_AREA = "native_ad_choices_area"
    const val STAR_ROW = "native_ad_star_row"
    const val STAR = "native_ad_star"
}

/** Filled stars of [rating] (O-08): none without a real rating above 0, else rounded half up, at most 5. */
internal fun nativeStarCount(rating: Double?): Int {
    if (rating == null || rating.isNaN() || rating <= 0.0) return 0
    if (rating >= MAX_STARS) return MAX_STARS
    return rating.roundToInt().coerceIn(0, MAX_STARS)
}

private const val MAX_STARS = 5

/**
 * Side margins are measured from the outer edge, as the plan's table reads them (media 355 dp wide in a 379 dp card);
 * the design's export places side parts 1 dp further in, inside the border, which is within its 4 dp tolerance.
 */
private val InnerStart = NativeAdDimens.InnerPadding

/** End margin of small; it keeps the AdChoices corner free. */
private val SmallInnerEnd = Spacing.lg

/** Bottom margin of medium inside the border, so the parts add up to the 386 dp of the design. */
private val MediumInnerBottom = NativeAdDimens.BorderWidth + NativeAdDimens.InnerPadding

/** Top margin of medium inside the border; it keeps the AdChoices corner free. */
private val MediumInnerTop = NativeAdDimens.BorderWidth + Spacing.lg

private const val PULSE_MIN_ALPHA = 0.5f
private const val PULSE_MILLIS = 800

/**
 * The layout of a small or medium native ad. Each ad part goes through [binder], so a fake ad and a real ad bound to
 * the SDK share this layout.
 */
@Composable
internal fun NativeAdLayout(
    assets: NativeAdAssets,
    size: NativeAdSize,
    binder: NativeAdBinder,
    showAdChoicesMarker: Boolean,
    modifier: Modifier = Modifier,
) {
    NativeAdFrame(
        modifier = modifier.fillMaxWidth().height(NativeAdDimens.heightOf(size)).testTag(NativeAdTestTags.CARD),
    ) {
        when (size) {
            NativeAdSize.SMALL -> SmallLayout(assets, binder)
            NativeAdSize.MEDIUM -> MediumLayout(assets, binder)
            NativeAdSize.FULL -> FullLayout(assets, binder)
        }
        if (showAdChoicesMarker) AdChoicesMarker(Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun SmallLayout(assets: NativeAdAssets, binder: NativeAdBinder) {
    Row(
        modifier = Modifier.fillMaxSize().padding(start = InnerStart, end = SmallInnerEnd),
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NativeAdIcon(assets.icon, binder, size = NativeAdDimens.SmallIconSize)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NativeAdDimens.HeadlineGap),
        ) {
            NativeAdHeadline(assets.headline, binder)
            NativeAdBadgeLine(assets, binder, badgeFontSize = SmallBadgeFontSize)
        }
        assets.callToAction?.let {
            NativeAdCallToAction(
                text = it,
                binder = binder,
                height = NativeAdDimens.SmallCallToActionHeight,
                fontSize = SmallCallToActionFontSize,
            )
        }
    }
}

@Composable
private fun MediumLayout(assets: NativeAdAssets, binder: NativeAdBinder) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = InnerStart, top = MediumInnerTop, end = InnerStart, bottom = MediumInnerBottom),
        verticalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
    ) {
        NativeAdHeader(assets, binder)
        NativeAdMediaArea(binder, Modifier.fillMaxWidth().height(NativeAdDimens.MediaHeight))
        val bodyModifier = Modifier.fillMaxWidth().height(NativeAdDimens.BodyHeight)
        val body = assets.body
        if (body != null) NativeAdBody(body, binder, maxLines = 2, modifier = bodyModifier) else Spacer(bodyModifier)
        assets.callToAction?.let { NativeAdCallToAction(it, binder, Modifier.fillMaxWidth()) }
    }
}

/**
 * The full native ad (ads-business-rules.md 5.7): head, 200 dp media, body of two lines, call to action, star row.
 * Every part has a fixed height, so the card keeps the height of its placeholder with any text.
 */
@Composable
private fun FullLayout(assets: NativeAdAssets, binder: NativeAdBinder) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = InnerStart, top = MediumInnerTop, end = InnerStart, bottom = MediumInnerBottom),
        verticalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
    ) {
        NativeAdHeader(assets, binder, iconSize = NativeAdDimens.FullIconSize)
        NativeAdMediaArea(binder, Modifier.fillMaxWidth().height(NativeAdDimens.FullMediaHeight))
        val bodyModifier = Modifier.fillMaxWidth().height(NativeAdDimens.BodyHeight)
        val body = assets.body
        if (body != null) {
            NativeAdBody(body, binder, maxLines = FULL_BODY_LINES, modifier = bodyModifier, minLines = FULL_BODY_LINES)
        } else {
            Spacer(bodyModifier)
        }
        val ctaModifier = Modifier.fillMaxWidth().height(NativeAdDimens.CallToActionHeight)
        val callToAction = assets.callToAction
        if (callToAction != null) NativeAdCallToAction(callToAction, binder, ctaModifier) else Spacer(ctaModifier)
        NativeAdStarRow(assets.starRating, Modifier.fillMaxWidth().height(NativeAdDimens.StarRowHeight))
    }
}

private const val FULL_BODY_LINES = 2

/** Filled stars of the ad's rating (O-08); nothing when the ad has no rating above 0. */
@Composable
private fun NativeAdStarRow(rating: Double?, modifier: Modifier = Modifier) {
    val count = nativeStarCount(rating)
    Box(modifier = modifier) {
        if (count > 0) {
            val description = stringResource(R.string.ads_native_star_rating, count)
            Row(
                modifier = Modifier
                    .testTag(NativeAdTestTags.STAR_ROW)
                    .semantics { contentDescription = description },
                horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.HeadlineGap),
            ) {
                val color = MaterialTheme.colorScheme.tertiary
                repeat(count) { NativeAdStar(color, Modifier.size(NativeAdDimens.StarRowHeight)) }
            }
        }
    }
}

/** One filled five-pointed star. */
@Composable
private fun NativeAdStar(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.testTag(NativeAdTestTags.STAR)) {
        val center = Offset(size.width / 2, size.height / 2)
        val outer = size.minDimension / 2
        val inner = outer * STAR_INNER_RATIO
        val path = Path()
        repeat(STAR_POINTS * 2) { i ->
            val radius = if (i % 2 == 0) outer else inner
            val angle = -PI / 2 + i * PI / STAR_POINTS
            val point = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        drawPath(path, color)
    }
}

private const val STAR_POINTS = 5
private const val STAR_INNER_RATIO = 0.45f

/** Icon, headline and the "Ad" line in one row: the head of the medium and full ads. */
@Composable
internal fun NativeAdHeader(
    assets: NativeAdAssets,
    binder: NativeAdBinder,
    modifier: Modifier = Modifier,
    iconSize: Dp = NativeAdDimens.IconSize,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NativeAdIcon(assets.icon, binder, size = iconSize)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NativeAdDimens.HeadlineGap),
        ) {
            NativeAdHeadline(assets.headline, binder)
            NativeAdBadgeLine(assets, binder, bodyWhenNoAdvertiser = false)
        }
    }
}

/** Background, 1 dp border and rounded corners that set a native ad apart from the app's content. */
@Composable
internal fun NativeAdFrame(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(NativeAdDimens.BorderWidth, MaterialTheme.colorScheme.outlineVariant, shape),
        content = content,
    )
}

@Composable
internal fun NativeAdIcon(
    icon: ImageBitmap?,
    binder: NativeAdBinder,
    modifier: Modifier = Modifier,
    size: Dp = NativeAdDimens.IconSize,
) {
    val shape = RoundedCornerShape(NativeAdDimens.AssetCorner)
    binder.Asset(NativeAdAsset.ICON, modifier.size(size).testTag(NativeAdTestTags.ICON)) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(shape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            )
        }
    }
}

@Composable
internal fun NativeAdHeadline(text: String, binder: NativeAdBinder, modifier: Modifier = Modifier) {
    binder.Asset(NativeAdAsset.HEADLINE, modifier.fillMaxWidth().testTag(NativeAdTestTags.HEADLINE)) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The "Ad" label and, next to it, the advertiser. Without an advertiser it shows the body when
 * [bodyWhenNoAdvertiser] is true (small has no other place for the body). [badgeFontSize] overrides the label's
 * size where the design sets one outside the type scale.
 */
@Composable
internal fun NativeAdBadgeLine(
    assets: NativeAdAssets,
    binder: NativeAdBinder,
    modifier: Modifier = Modifier,
    bodyWhenNoAdvertiser: Boolean = true,
    badgeFontSize: TextUnit = TextUnit.Unspecified,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.BadgeGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NativeAdBadge(fontSize = badgeFontSize)
        val advertiser = assets.advertiser
        val body = assets.body
        when {
            advertiser != null -> BadgeLineText(NativeAdAsset.ADVERTISER, advertiser, binder)
            bodyWhenNoAdvertiser && body != null -> BadgeLineText(NativeAdAsset.BODY, body, binder)
        }
    }
}

@Composable
private fun BadgeLineText(asset: NativeAdAsset, text: String, binder: NativeAdBinder) {
    binder.Asset(asset, Modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The "Ad" label required on every native ad. It belongs to the app, not to the ad's assets. */
@Composable
internal fun NativeAdBadge(modifier: Modifier = Modifier, fontSize: TextUnit = TextUnit.Unspecified) {
    Box(
        modifier = modifier
            .testTag(NativeAdTestTags.BADGE)
            .clip(RoundedCornerShape(NativeAdDimens.BadgeCorner))
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(horizontal = Spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.ads_native_badge),
            style = MaterialTheme.typography.labelSmall.merge(TextStyle(fontSize = fontSize)),
            color = MaterialTheme.colorScheme.onTertiary,
            maxLines = 1,
        )
    }
}

@Composable
internal fun NativeAdBody(
    text: String,
    binder: NativeAdBinder,
    maxLines: Int,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
) {
    binder.Asset(NativeAdAsset.BODY, modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
            minLines = minLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The call to action: tertiary container, [height] high (36 dp, 34 dp on small), 8 dp corners, unlike the app's
 * PrimaryButton. It is a plain box, not a Button, so a touch reaches the SDK's call-to-action view.
 */
@Composable
internal fun NativeAdCallToAction(
    text: String,
    binder: NativeAdBinder,
    modifier: Modifier = Modifier,
    height: Dp = NativeAdDimens.CallToActionHeight,
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    binder.Asset(NativeAdAsset.CALL_TO_ACTION, modifier.testTag(NativeAdTestTags.CTA)) {
        Box(
            modifier = Modifier
                .height(height)
                .widthIn(min = NativeAdDimens.CallToActionMinWidth)
                .clip(RoundedCornerShape(NativeAdDimens.AssetCorner))
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(horizontal = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.merge(TextStyle(fontSize = fontSize)),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The media of the ad (image or video), 8 dp corners. */
@Composable
internal fun NativeAdMediaArea(binder: NativeAdBinder, modifier: Modifier = Modifier) {
    binder.Media(modifier.clip(RoundedCornerShape(NativeAdDimens.AssetCorner)).testTag(NativeAdTestTags.MEDIA))
}

/** Where the SDK draws AdChoices; drawn on fake ads only, so screenshots match the design. */
@Composable
private fun AdChoicesMarker(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = Spacing.xs, end = Spacing.xs)
            .size(NativeAdDimens.AdChoicesSize)
            .clip(RoundedCornerShape(NativeAdDimens.BadgeCorner))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .testTag(NativeAdTestTags.AD_CHOICES_AREA),
    )
}

/**
 * The loading blocks of [size] in a frame of [height]. The media block of medium takes the height left, so a layout
 * of another height (collapsible, full screen) reuses it.
 */
@Composable
internal fun NativeAdPlaceholderLayout(
    size: NativeAdSize,
    height: Dp,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val alpha = placeholderAlpha(animated)
    NativeAdFrame(modifier = modifier.fillMaxWidth().height(height).testTag(NativeAdTestTags.PLACEHOLDER)) {
        val pulse = Modifier.graphicsLayer { this.alpha = alpha.value }
        when (size) {
            NativeAdSize.SMALL -> Row(
                modifier = Modifier.fillMaxSize().padding(start = InnerStart, end = SmallInnerEnd).then(pulse),
                horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaceholderBlock(Modifier.size(NativeAdDimens.SmallIconSize))
                PlaceholderLines(Modifier.weight(1f))
                PlaceholderBlock(
                    Modifier.size(NativeAdDimens.CallToActionPlaceholderWidth, NativeAdDimens.SmallCallToActionHeight),
                )
            }
            NativeAdSize.FULL -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = InnerStart, top = MediumInnerTop, end = InnerStart, bottom = MediumInnerBottom)
                    .then(pulse),
                verticalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
            ) {
                FullPlaceholderBlocks()
            }
            NativeAdSize.MEDIUM -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = InnerStart, top = MediumInnerTop, end = InnerStart, bottom = MediumInnerBottom)
                    .then(pulse),
                verticalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
            ) {
                MediumPlaceholderBlocks()
            }
        }
    }
}

@Composable
private fun ColumnScope.MediumPlaceholderBlocks() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaceholderBlock(Modifier.size(NativeAdDimens.IconSize))
        PlaceholderLines(Modifier.weight(1f))
    }
    PlaceholderBlock(Modifier.fillMaxWidth().weight(1f))
    PlaceholderLines(Modifier.fillMaxWidth().height(NativeAdDimens.BodyHeight))
    PlaceholderBlock(Modifier.fillMaxWidth().height(NativeAdDimens.CallToActionHeight))
}

@Composable
private fun ColumnScope.FullPlaceholderBlocks() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaceholderBlock(Modifier.size(NativeAdDimens.FullIconSize))
        PlaceholderLines(Modifier.weight(1f))
    }
    PlaceholderBlock(Modifier.fillMaxWidth().height(NativeAdDimens.FullMediaHeight))
    PlaceholderLines(Modifier.fillMaxWidth().height(NativeAdDimens.BodyHeight))
    PlaceholderBlock(Modifier.fillMaxWidth().weight(1f))
}

@Composable
private fun PlaceholderLines(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        val bar = RoundedCornerShape(NativeAdDimens.LineHeight / 2)
        PlaceholderBlock(Modifier.size(NativeAdDimens.LongLineWidth, NativeAdDimens.LineHeight), bar)
        PlaceholderBlock(Modifier.size(NativeAdDimens.ShortLineWidth, NativeAdDimens.LineHeight), bar)
    }
}

@Composable
private fun PlaceholderBlock(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(NativeAdDimens.AssetCorner),
) {
    Box(modifier = modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest))
}

/** A soft pulse of the loading blocks; full opacity, fixed, when not [animated]. */
@Composable
private fun placeholderAlpha(animated: Boolean): State<Float> {
    if (!animated) return remember { mutableFloatStateOf(1f) }
    return rememberInfiniteTransition(label = "nativeAdPlaceholder").animateFloat(
        initialValue = 1f,
        targetValue = PULSE_MIN_ALPHA,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
        label = "alpha",
    )
}

@Preview
@Composable
private fun NativeAdCardPreview() {
    val assets = NativeAdAssets(
        headline = "Headline of the ad",
        advertiser = "Advertiser",
        body = "Body text of the ad.",
        callToAction = "Install",
        icon = null,
        hasMedia = true,
    )
    AppTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            NativeAdCard(assets, NativeAdSize.SMALL)
            NativeAdPlaceholder(NativeAdSize.SMALL, animated = false)
            NativeAdCard(assets, NativeAdSize.MEDIUM)
            NativeAdCard(assets.copy(starRating = 4.0), NativeAdSize.FULL)
        }
    }
}
