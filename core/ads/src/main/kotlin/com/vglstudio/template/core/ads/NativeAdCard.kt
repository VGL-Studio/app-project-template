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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.core.designsystem.theme.Spacing
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Size of a native ad drawn inside a screen (ads-guide.md, section 4). [FULL] is the full native ad of
 * ads-business-rules.md 5.7; no ad format draws it yet (plan T-027, Q2). [CARD] is the portrait card of a grid, a list
 * or a pager page (`native_in_collection`, 5.7; plan T-028). [STEP] is the 250 dp card of a flow step (`native_step`,
 * 5.7; plan T-032).
 */
enum class NativeAdSize { SMALL, MEDIUM, FULL, CARD, STEP }

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
 * never draw it (ads-guide.md, section 2). [cardHeight] sets the height of a card ([NativeAdSize.CARD]) to that of
 * the app's card next to it; null keeps 300 dp, and other sizes ignore it.
 */
@Composable
fun NativeAdCard(
    assets: NativeAdAssets,
    size: NativeAdSize,
    modifier: Modifier = Modifier,
    ctaLarge: Boolean = false,
    cardHeight: Dp? = null,
) {
    NativeAdLayout(
        assets,
        size,
        PreviewNativeAdBinder,
        showAdChoicesMarker = true,
        modifier = modifier,
        ctaLarge = ctaLarge,
        cardHeight = cardHeight,
    )
}

/**
 * The loading state of a native ad: same frame and height as the ad, so nothing moves when the ad arrives (KB-1).
 * [animated] off keeps screenshots independent of time. [ctaLarge] is the medium ad with the large call to action.
 * The step card's placeholder is [NativeStepAdDefaults.height] high, like the card, at every font scale (plan T-045),
 * and small's [rememberSmallNativeHeight] (plan T-109).
 * [cardHeight] is the height of a card placeholder, as for [NativeAdCard].
 */
@Composable
fun NativeAdPlaceholder(
    size: NativeAdSize,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    ctaLarge: Boolean = false,
    cardHeight: Dp? = null,
) {
    NativeAdPlaceholderLayout(
        size = size,
        height = when (size) {
            NativeAdSize.CARD -> cardHeight ?: NativeAdDimens.CardHeight
            else -> nativeAdHeight(size, ctaLarge)
        },
        modifier = modifier,
        animated = animated,
        mediumCallToActionHeight = NativeAdDimens.mediumCallToActionHeight(ctaLarge),
    )
}

/**
 * Sizes of the native ad components, from docs/features/ads/design/context/ (design README, section 3) and
 * ads-business-rules.md 5.7.
 */
internal object NativeAdDimens {
    /**
     * Medium below this height is the low frame (plan T-065, question 7): a one-line body and 8 dp gaps, so a 320 dp
     * card keeps more than 120 dp of media. 350 dp is the least card where the design's layout still gives the media
     * 120 dp with the large call to action.
     */
    val MediumCompactBelow = 350.dp

    /**
     * Small (design T-108, `Native/Small`): its height at font scale 1.0, 6 + 44 + 6 + 44 + 6 dp; with larger text it
     * grows ([smallNativeHeight], plan T-109).
     */
    val SmallHeight = 106.dp

    /** The top and bottom padding of small and the gap between its two tiers; outside the design system scale. */
    val SmallTierGap = 6.dp

    /** The end margin of the top row of small inside the inner margin: room for AdChoices in the top end corner. */
    val SmallTopRowEnd = 16.dp
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

    /** Icon of small and medium (design T-064). */
    val SmallMediumIconSize = 44.dp
    val FullIconSize = 40.dp
    val AssetCorner = 8.dp
    val CallToActionHeight = 36.dp

    /** Call to action of small and medium (design T-064). */
    val MediumCallToActionHeight = 44.dp
    val LargeCallToActionHeight = 56.dp
    val LargeCallToActionTextSize = 17.sp

    /** The least width of a call to action. */
    val CallToActionMinWidth = 80.dp
    val MediaHeight = 196.dp
    val FullMediaHeight = 200.dp

    /** The least media of a native ad, 120 x 120 dp (KB-2, AdMob); the full native ad never shrinks it below this. */
    val MinMediaSize = 120.dp
    val BodyHeight = 40.dp

    /** The body frame of medium on the low frame: one line of 14/20 sp. */
    val CompactBodyHeight = 20.dp

    /** The gaps between the parts of medium on the low frame. */
    val CompactPartGap = 8.dp
    val AdChoicesSize = 16.dp
    val StarRowHeight = 16.dp
    val StarSize = StarRowHeight
    val StarGap = 2.dp

    /** Card (5.7): default height of the README example (180 x 300 dp); the cell of a grid may set another. */
    val CardHeight = 300.dp
    val CardPadding = 8.dp
    val CardIconSize = 32.dp
    val CardHeadlineTextSize = 13.sp

    /**
     * Card of a flow step (5.7, component `Native/Step` of the design, T-035): 250 dp at the default font scale
     * ([NativeStepAdDefaults.height] at others, plan T-045), 32 dp icon, 36 dp call to
     * action, 13 sp headline, 12 dp margin, 8 dp gaps, media filling the rest (the former 250 dp layout of
     * `Native/Collapsible`, plan T-032). Its own constants on purpose: `Native/Collapsible` follows the T-064 design
     * since T-065 and no longer shares these numbers.
     */
    val StepHeight = 250.dp
    val StepIconSize = 32.dp
    val StepCallToActionHeight = 36.dp
    val StepHeadlineTextSize = 13.sp

    /**
     * Full native, the height of its placeholder: 1 + 24 top, head 40, media 200, body 40 (two lines), call to action
     * 36, star row 16, four 12 dp gaps between these five parts, 12 + 1 bottom. With large text the media shrinks to
     * keep this height, down to [MinMediaSize]; only past that does the card grow ([fullMediaHeight], plan T-044).
     */
    val FullHeight = 418.dp
    val FullPlaceholderHeight = FullHeight

    /** Loading bars for the two text lines, and the call-to-action block. */
    val LineHeight = 10.dp
    val LongLineWidth = 180.dp
    val ShortLineWidth = 120.dp
    val CallToActionPlaceholderWidth = CallToActionMinWidth

    /**
     * The loading bars of small (design/context/SampleScreen_ad-loading_light.md, two tiers since T-108): 14 dp as
     * wide as the text column, then 12 x 120 dp; the call-to-action block spans the card.
     */
    val SmallPlaceholderTopLineHeight = 14.dp
    val SmallPlaceholderBottomLineHeight = 12.dp

    /**
     * The design height of [size] on a window of [screen], at the default font scale (KB-32): medium as the screen
     * height says ([mediumHeight]; the same with the large call to action, whose media is 12 dp lower). Medium keeps
     * this height while its media stays above 120 dp; past that it grows (on a 891 dp screen at font scale 2.0,
     * 406 dp; plan T-065). Full keeps 418 dp up to font scale 2.0. Small and the step card grow with larger text, so
     * composables read [nativeAdHeight] for them (plans T-045, T-109); small here is an [IllegalArgumentException].
     */
    @Suppress("UNUSED_PARAMETER")
    fun heightOf(size: NativeAdSize, ctaLarge: Boolean, screen: DpSize): Dp = when (size) {
        NativeAdSize.SMALL -> throw IllegalArgumentException(
            "Small follows the font scale: read nativeAdHeight(NativeAdSize.SMALL, ctaLarge) in a composable",
        )
        NativeAdSize.MEDIUM -> mediumHeight(screen.height)
        NativeAdSize.FULL -> FullPlaceholderHeight
        NativeAdSize.CARD -> CardHeight
        NativeAdSize.STEP -> StepHeight
    }

    /**
     * The open collapsible layer (KB-32 criterion 3, question 66; plan T-109): its media area and the small native
     * content below it add up to 250 dp on a window whose short edge is 320 dp to 400 dp at 600 dp, linear between,
     * kept outside: 271 dp at 360 dp, 299 dp at 411 dp. [collapsibleLayer] gives the media area what the content
     * leaves.
     */
    fun collapsibleTotalHeight(shortEdge: Dp): Dp = interpolated(shortEdge, NarrowScreen, WideScreen, 250.dp, 400.dp)

    /** The open collapsible layer: its media area (MediaView and its 12 dp top margin) over the small native content. */
    data class CollapsibleLayerSize(val mediaArea: Dp, val slot: Dp) {
        val height: Dp get() = mediaArea + slot
    }

    /** The share of the window height the open layer with its bottom margin may take (KB-32 criterion 3). */
    const val COLLAPSIBLE_WINDOW_SHARE = 0.5f

    /** The bottom margin of the layer, counted in its ceiling. */
    val CollapsibleLayerMargin = BottomAdBarDimens.SlotMargin

    /** The least MediaView of the layer (KB-2 criterion 1, AdMob). */
    val CollapsibleMediaFloor = MinMediaSize

    /** The least media area: the MediaView floor and its top margin, 132 dp. */
    val CollapsibleMediaAreaFloor = CollapsibleMediaFloor + CollapsibleNativeAdDimens.MediaMargin

    /**
     * The open collapsible layer on a window of [window] over a small native [content] (KB-4, KB-32 criterion 3,
     * question 66; plan T-109): the media area is what [collapsibleTotalHeight] of the short edge leaves over
     * [content], never below [CollapsibleMediaAreaFloor] (with large text the layer grows instead), and the layer with
     * its 16 dp bottom margin never past half the window height: past it the media area shrinks, to whole dp; below
     * [CollapsibleMediaAreaFloor] there is no room and this is null (no layer: a phone in landscape, a split screen).
     * With the 106 dp content of font scale 1.0: 299 dp at 411 x 891 dp, 284 dp at 411 x 600 dp, null below 411 x 508
     * dp.
     */
    fun collapsibleLayer(window: DpSize, content: Dp): CollapsibleLayerSize? {
        val wanted = max(collapsibleTotalHeight(window.shortEdge) - content, CollapsibleMediaAreaFloor)
        val room = floor(
            window.height.value * COLLAPSIBLE_WINDOW_SHARE - content.value - CollapsibleLayerMargin.value,
        ).dp
        val mediaArea = min(wanted, room)
        return if (mediaArea < CollapsibleMediaAreaFloor) null else CollapsibleLayerSize(mediaArea, content)
    }

    /** Medium (KB-32): 320 dp on a 640 dp high screen to 400 dp on a 891 dp one, linear between, kept outside. */
    fun mediumHeight(screenHeight: Dp): Dp = interpolated(screenHeight, LowScreen, HighScreen, 320.dp, 400.dp)

    /** Whether medium on a screen of [screenHeight] is the low frame ([MediumCompactBelow]). */
    fun mediumIsCompact(screenHeight: Dp): Boolean = mediumHeight(screenHeight) < MediumCompactBelow

    private val NarrowScreen = 320.dp
    private val WideScreen = 600.dp
    private val LowScreen = 640.dp
    private val HighScreen = 891.dp

    /**
     * [low] at [from] to [high] at [to], linear between, kept outside; rounded to the nearest whole dp like the numbers
     * of the design (299 dp at 411 dp), so screenshots never fall between two pixels.
     */
    private fun interpolated(at: Dp, from: Dp, to: Dp, low: Dp, high: Dp): Dp {
        val fraction = ((at - from) / (to - from)).coerceIn(0f, 1f)
        return (low.value + (high.value - low.value) * fraction).roundToInt().dp
    }

    /** The call to action of medium: 44 dp, 56 dp when large. */
    fun mediumCallToActionHeight(ctaLarge: Boolean): Dp =
        if (ctaLarge) LargeCallToActionHeight else MediumCallToActionHeight
}

/** The short edge of a window: the native ads that follow the window (KB-32) read it, so landscape keeps portrait. */
internal val DpSize.shortEdge: Dp get() = min(width, height)

/**
 * The width and height of the app's window, from the configuration (as [AdSlot] reads its width; in multi-window mode
 * the window, not the display): the native ads that follow the window (KB-32) read it, never the width of their own
 * card.
 */
@Composable
@ReadOnlyComposable
internal fun nativeScreenSize(): DpSize {
    val configuration = LocalConfiguration.current
    return DpSize(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp)
}

/**
 * The height of a native ad of [size] at the current font scale, for the ad, its placeholder and the space kept when
 * empty: small [rememberSmallNativeHeight], the step card [NativeStepAdDefaults.height] (both grow with larger text),
 * the others [NativeAdDimens.heightOf] on this window. [ctaLarge] is the medium ad with the large call to action.
 */
@Composable
internal fun nativeAdHeight(size: NativeAdSize, ctaLarge: Boolean): Dp = when (size) {
    NativeAdSize.SMALL -> rememberSmallNativeHeight()
    NativeAdSize.STEP -> NativeStepAdDefaults.height()
    else -> NativeAdDimens.heightOf(size, ctaLarge, nativeScreenSize())
}

/**
 * The height of small at the current font scale ([smallNativeHeight]), the same on every window: 106 dp at the
 * default font scale. The card, its placeholder, the space kept for the bottom slot and the content of the collapsible
 * layer all take it (KB-1, KB-32, D-04; plan T-109).
 */
@Composable
internal fun rememberSmallNativeHeight(): Dp {
    val measurer = rememberTextMeasurer()
    val typography = MaterialTheme.typography
    val density = LocalDensity.current
    return remember(measurer, typography, density) { smallNativeHeight(measurer, typography, density) }
}

/**
 * The height of small (design T-108, two tiers) for one line of each of its texts, measured with [measurer] at
 * [density] (the same measuring `Text` does, so the non-linear font scale of Android 14 is in it, as
 * [stepCardHeight]): the headline row (the 16/22 sp headline or the 13 sp "Ad" label), 4 dp and the 13/18 sp line
 * below make the text column; the top row is the 44 dp icon or that column; the call to action 44 dp or one line of
 * its 18 sp text; 6 dp above, between and below. Added in pixels, then rounded up to a whole dp, never under 106 dp.
 */
internal fun smallNativeHeight(measurer: TextMeasurer, typography: Typography, density: Density): Dp = with(density) {
    fun line(style: TextStyle): Int =
        measurer.measure(SMALL_LINE_SAMPLE, style, maxLines = 1, density = density).size.height
    val headline = line(
        typography.titleSmall.merge(
            TextStyle(fontSize = NativeHeadlineFontSize, lineHeight = NativeHeadlineLineHeight),
        ),
    )
    val badge = line(typography.labelSmall.merge(TextStyle(fontSize = NativeBadgeFontSize)))
    val second = line(
        typography.bodySmall.merge(
            TextStyle(fontSize = NativeSecondLineFontSize, lineHeight = NativeSecondLineLineHeight),
        ),
    )
    val column = max(headline, badge) + SmallHeadlineBodyGap.roundToPx() + second
    val topRow = max(NativeAdDimens.SmallMediumIconSize.roundToPx(), column)
    val callToAction = max(
        NativeAdDimens.MediumCallToActionHeight.roundToPx(),
        line(typography.labelLarge.merge(TextStyle(fontSize = NativeCallToActionFontSize))),
    )
    val gap = NativeAdDimens.SmallTierGap.roundToPx()
    val card = ceil((gap + topRow + gap + callToAction + gap).toDp().value).dp
    if (card < NativeAdDimens.SmallHeight) NativeAdDimens.SmallHeight else card
}

// T-110 skeleton (step 3): replaced in step 4.
internal fun nativeContentHeight(
    measurer: TextMeasurer,
    typography: Typography,
    density: Density,
    ctaLarge: Boolean,
): Dp = 0.dp

internal fun mediumNativeHeight(measurer: TextMeasurer, typography: Typography, density: Density, ctaLarge: Boolean): Dp =
    0.dp

/** Text measured for one line; the height of a line depends on the style, not on the text. */
private const val SMALL_LINE_SAMPLE = "Ad"

/**
 * The open collapsible layer on this window at the current font scale ([NativeAdDimens.collapsibleLayer] over
 * [rememberSmallNativeHeight]); null when the window has no room. A change of font scale is a change of
 * configuration, so the screen is drawn again and this is worked out again, as when it turns (plan T-109).
 */
@Composable
internal fun rememberCollapsibleLayer(): NativeAdDimens.CollapsibleLayerSize? =
    NativeAdDimens.collapsibleLayer(nativeScreenSize(), rememberSmallNativeHeight())

/**
 * Text sizes of small and medium outside the type scale (design T-064, design/context/AdGalleryScreen_default_light.md):
 * the "Ad" label 13 sp (its line height kept from `labelSmall`, plan T-065 question 10), the headline 16/22 sp, the
 * line below (advertiser or body of small, advertiser of medium) 13/18 sp, the call-to-action text 18 sp.
 */
private val NativeBadgeFontSize = 13.sp
private val NativeHeadlineFontSize = 16.sp
private val NativeHeadlineLineHeight = 22.sp
private val NativeSecondLineFontSize = 13.sp
private val NativeSecondLineLineHeight = 18.sp
private val NativeCallToActionFontSize = 18.sp

/** Gap between the headline row and the line below it in small (design: 4 dp, xs); [NativeAdDimens.HeadlineGap] is 2. */
private val SmallHeadlineBodyGap = Spacing.xs

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
    const val PLACEHOLDER_CTA = "native_ad_placeholder_cta"
    const val PLACEHOLDER_ICON = "native_ad_placeholder_icon"
    const val SECOND_LINE = "native_ad_second_line"
    const val SMALL_TOP_ROW = "native_ad_small_top_row"
    const val PLACEHOLDER_LINE = "native_ad_placeholder_line"
    const val BODY = "native_ad_body"
    const val CONTENT = "native_ad_content"
    const val PLACEHOLDER_MEDIA = "native_ad_placeholder_media"
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

/** Bottom margin of medium inside the border, so the parts add up to the 400 dp of the design. */
private val MediumInnerBottom = NativeAdDimens.BorderWidth + NativeAdDimens.InnerPadding

/** Top margin of medium inside the border; it keeps the AdChoices corner free. */
private val MediumInnerTop = NativeAdDimens.BorderWidth + Spacing.lg

private const val PULSE_MIN_ALPHA = 0.5f
private const val PULSE_MILLIS = 800

/**
 * The layout of a native ad of [size]. Each ad part goes through [binder], so a fake ad and a real ad bound to the SDK
 * share this layout. A card takes the height a cell gives it; its own height is the default, or [cardHeight] when
 * set.
 */
@Composable
internal fun NativeAdLayout(
    assets: NativeAdAssets,
    size: NativeAdSize,
    binder: NativeAdBinder,
    showAdChoicesMarker: Boolean,
    modifier: Modifier = Modifier,
    ctaLarge: Boolean = false,
    cardHeight: Dp? = null,
) {
    val screen = nativeScreenSize()
    NativeAdFrame(
        modifier = modifier
            .fillMaxWidth()
            .then(
                when (size) {
                    // Medium and full grow past their design height only when their media reaches the floor
                    // (MediaShrinksFirstLayout, plans T-044, T-045).
                    NativeAdSize.MEDIUM, NativeAdSize.FULL ->
                        Modifier.heightIn(min = NativeAdDimens.heightOf(size, ctaLarge, screen))
                    // The step card: 250 dp, more with large text so its media keeps 120 dp (plan T-045).
                    NativeAdSize.STEP -> Modifier.height(NativeStepAdDefaults.height())
                    NativeAdSize.CARD -> Modifier.height(cardHeight ?: NativeAdDimens.CardHeight)
                    // Small: 106 dp at font scale 1.0 on every window, more with larger text (plan T-109).
                    NativeAdSize.SMALL -> Modifier.height(rememberSmallNativeHeight())
                },
            )
            .testTag(NativeAdTestTags.CARD),
    ) {
        when (size) {
            NativeAdSize.SMALL -> SmallLayout(assets, binder)
            NativeAdSize.MEDIUM -> MediumLayout(assets, binder, ctaLarge, screen)
            NativeAdSize.FULL -> FullLayout(assets, binder)
            NativeAdSize.CARD -> CardLayout(assets, binder)
            NativeAdSize.STEP -> StepLayout(assets, binder)
        }
        if (showAdChoicesMarker) AdChoicesMarker(Modifier.align(Alignment.TopEnd))
    }
}

/**
 * Small (design T-108, component `Native/Small`; the bottom slot, a slot of its own, a row of a list and the content
 * of the collapsible layer): two tiers, 12 dp side margins, 6 dp above, between and below. The top row, at least 44 dp
 * high with a 16 dp end margin that keeps the AdChoices corner free: the 44 dp icon and, 12 dp after it, the text
 * column ([SmallTexts]), centered. Below it a 44 dp call to action with 18 sp text as wide as the card. The card is
 * [rememberSmallNativeHeight] high (106 dp at font scale 1.0, more with larger text, plan T-109).
 */
@Composable
internal fun SmallLayout(assets: NativeAdAssets, binder: NativeAdBinder) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = InnerStart, vertical = NativeAdDimens.SmallTierGap),
        verticalArrangement = Arrangement.spacedBy(NativeAdDimens.SmallTierGap),
    ) {
        Row(
            modifier = Modifier
                .testTag(NativeAdTestTags.SMALL_TOP_ROW)
                .fillMaxWidth()
                .heightIn(min = NativeAdDimens.SmallMediumIconSize)
                .padding(end = NativeAdDimens.SmallTopRowEnd),
            horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NativeAdIcon(assets.icon, binder, size = NativeAdDimens.SmallMediumIconSize)
            SmallTexts(assets, binder, Modifier.weight(1f))
        }
        val callToAction = assets.callToAction
        if (callToAction != null) {
            NativeAdCallToAction(
                text = callToAction,
                binder = binder,
                modifier = Modifier.fillMaxWidth(),
                height = NativeAdDimens.MediumCallToActionHeight,
                fontSize = NativeCallToActionFontSize,
            )
        } else {
            // Keeps the two tiers where they are when the ad has no call to action.
            Spacer(Modifier.fillMaxWidth().height(NativeAdDimens.MediumCallToActionHeight))
        }
    }
}

/**
 * The text column of small: the headline row ("Ad" before a one-line 16/22 sp headline), 4 dp, then the line below
 * (the advertiser, else the body; 13/18 sp, one line at every font scale, cut with "…"; question 9 of plan T-065,
 * question 2 of plan T-109); no line below without either.
 */
@Composable
private fun SmallTexts(assets: NativeAdAssets, binder: NativeAdBinder, modifier: Modifier = Modifier) {
    val advertiser = assets.advertiser
    val body = assets.body
    val secondLine = when {
        advertiser != null -> NativeAdAsset.ADVERTISER to advertiser
        body != null -> NativeAdAsset.BODY to body
        else -> null
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SmallHeadlineBodyGap)) {
        SmallTitleRow(assets.headline, binder, withBadge = true)
        if (secondLine != null) {
            SecondLineText(secondLine.first, secondLine.second, binder, maxLines = 1, Modifier.fillMaxWidth())
        }
    }
}

/** The "Ad" label (13 sp, when [withBadge]) before a one-line 16/22 sp headline: small and the head of medium. */
@Composable
private fun SmallTitleRow(headline: String, binder: NativeAdBinder, withBadge: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.BadgeGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (withBadge) NativeAdBadge(fontSize = NativeBadgeFontSize)
        NativeAdHeadline(
            headline,
            binder,
            Modifier.weight(1f),
            fontSize = NativeHeadlineFontSize,
            lineHeight = NativeHeadlineLineHeight,
        )
    }
}

@Composable
private fun SecondLineText(
    asset: NativeAdAsset,
    text: String,
    binder: NativeAdBinder,
    maxLines: Int,
    modifier: Modifier = Modifier,
) {
    binder.Asset(asset, modifier.testTag(NativeAdTestTags.SECOND_LINE)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.merge(
                TextStyle(fontSize = NativeSecondLineFontSize, lineHeight = NativeSecondLineLineHeight),
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Medium (design T-064): head ([MediumHeader], 60 dp with the star row), media over a token backdrop filling what
 * the card leaves (182 dp in a 400 dp card, 170 dp with [ctaLarge]), body of two lines, call to action of 44 dp with
 * 18 sp text (56 dp and 17 sp with [ctaLarge]; the text of the SDK is kept as it is), 12 dp apart. The card is
 * [NativeAdDimens.mediumHeight] of the [screen] height. On the low frame ([NativeAdDimens.mediumIsCompact], a card
 * below 350 dp) the body keeps one line in a 20 dp frame and the gaps are 8 dp, so a 320 dp card keeps 134 dp of
 * media (122 dp; question 7 of plan T-065). With larger text the media shrinks first, never below 120 dp; past that
 * the card grows ([MediaShrinksFirstLayout], plan T-045).
 */
@Composable
private fun MediumLayout(assets: NativeAdAssets, binder: NativeAdBinder, ctaLarge: Boolean, screen: DpSize) {
    val ctaHeight = NativeAdDimens.mediumCallToActionHeight(ctaLarge)
    val compact = NativeAdDimens.mediumIsCompact(screen.height)
    MediaShrinksFirstLayout(
        innerHeight = NativeAdDimens.mediumHeight(screen.height) - MediumInnerTop - MediumInnerBottom,
        gap = if (compact) NativeAdDimens.CompactPartGap else NativeAdDimens.PartGap,
        mediaIndex = 1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = InnerStart, top = MediumInnerTop, end = InnerStart, bottom = MediumInnerBottom),
    ) {
        MediumHeader(assets, binder)
        NativeAdMediaArea(binder, Modifier.fillMaxWidth(), backdrop = true)
        val bodyModifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) NativeAdDimens.CompactBodyHeight else NativeAdDimens.BodyHeight)
            .testTag(NativeAdTestTags.BODY)
        val body = assets.body
        if (body != null) {
            NativeAdBody(body, binder, maxLines = if (compact) 1 else 2, modifier = bodyModifier)
        } else {
            // A box, not a Spacer: the tagged node then reports the height it keeps.
            Box(bodyModifier)
        }
        val callToAction = assets.callToAction
        if (callToAction != null) {
            NativeAdCallToAction(
                text = callToAction,
                binder = binder,
                modifier = Modifier.fillMaxWidth(),
                height = ctaHeight,
                fontSize = if (ctaLarge) NativeAdDimens.LargeCallToActionTextSize else NativeCallToActionFontSize,
            )
        } else {
            // Keeps the media at its design size when the ad has no call to action.
            Spacer(Modifier.fillMaxWidth().height(ctaHeight))
        }
    }
}

/**
 * The head of medium (design T-064): the 44 dp icon next to the headline row ("Ad" before the 16/22 sp headline), the
 * advertiser line (13/18 sp) and the 16 dp star row, 2 dp apart: 60 dp. Without an advertiser its line keeps its
 * height, as the star row does without a rating (L-055), so the media never moves.
 */
@Composable
private fun MediumHeader(assets: NativeAdAssets, binder: NativeAdBinder) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NativeAdIcon(assets.icon, binder, size = NativeAdDimens.SmallMediumIconSize)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NativeAdDimens.HeadlineGap),
        ) {
            SmallTitleRow(assets.headline, binder, withBadge = true)
            val advertiser = assets.advertiser
            if (advertiser != null) {
                SecondLineText(NativeAdAsset.ADVERTISER, advertiser, binder, maxLines = 1, Modifier.fillMaxWidth())
            } else {
                val lineHeight = with(LocalDensity.current) { NativeSecondLineLineHeight.toDp() }
                Spacer(Modifier.fillMaxWidth().height(lineHeight).testTag(NativeAdTestTags.SECOND_LINE))
            }
            NativeAdStarRow(assets.starRating, binder, Modifier.height(NativeAdDimens.StarRowHeight))
        }
    }
}

/**
 * The card of a collection (5.7, portrait): media fills the top (AdChoices sits on it, top right), then the 32 dp
 * icon with a one-line 13 sp headline and the "Ad" line, then a 36 dp call to action as wide as the card.
 */
@Composable
private fun CardLayout(assets: NativeAdAssets, binder: NativeAdBinder) {
    Column(
        modifier = Modifier.fillMaxSize().padding(NativeAdDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        NativeAdMediaArea(binder, Modifier.fillMaxWidth().weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NativeAdIcon(assets.icon, binder, size = NativeAdDimens.CardIconSize)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NativeAdDimens.HeadlineGap),
            ) {
                NativeAdHeadline(assets.headline, binder, fontSize = NativeAdDimens.CardHeadlineTextSize)
                NativeAdBadgeLine(assets, binder, bodyWhenNoAdvertiser = false)
            }
        }
        val ctaModifier = Modifier.fillMaxWidth().heightIn(min = NativeAdDimens.CallToActionHeight)
        val callToAction = assets.callToAction
        if (callToAction != null) NativeAdCallToAction(callToAction, binder, ctaModifier) else Spacer(ctaModifier)
    }
}

/**
 * The card of a flow step (5.7, 250 dp): media fills the top (AdChoices sits on it, top right, O-08), then the 32 dp
 * icon with a one-line 13 sp headline and the "Ad" line, then a 36 dp call to action as wide as the card; 12 dp
 * margin, 8 dp gaps. With larger text the media shrinks first, never below 120 dp; past that the card grows, to
 * [NativeStepAdDefaults.height] ([MediaShrinksFirstLayout], plan T-045).
 */
@Composable
private fun StepLayout(assets: NativeAdAssets, binder: NativeAdBinder) {
    MediaShrinksFirstLayout(
        innerHeight = NativeAdDimens.StepHeight - NativeAdDimens.InnerPadding * 2,
        gap = Spacing.sm,
        mediaIndex = 0,
        // Filling the card: the media takes what the card's height leaves (the card is set by the same numbers).
        modifier = Modifier.fillMaxSize().padding(NativeAdDimens.InnerPadding),
    ) {
        NativeAdMediaArea(binder, Modifier.fillMaxWidth())
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NativeAdIcon(assets.icon, binder, size = NativeAdDimens.StepIconSize)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NativeAdDimens.HeadlineGap),
            ) {
                NativeAdHeadline(assets.headline, binder, fontSize = NativeAdDimens.StepHeadlineTextSize)
                NativeAdBadgeLine(assets, binder, bodyWhenNoAdvertiser = false)
            }
        }
        val ctaModifier = Modifier.fillMaxWidth().heightIn(min = NativeAdDimens.StepCallToActionHeight)
        val callToAction = assets.callToAction
        if (callToAction != null) NativeAdCallToAction(callToAction, binder, ctaModifier) else Spacer(ctaModifier)
    }
}

/**
 * The full native ad (ads-business-rules.md 5.7): head, media, body of two lines, call to action, star row, 12 dp
 * apart. At font scale 1.0 the media is 200 dp and the card 418 dp, the height of its placeholder. When the text
 * grows (system font size), the media shrinks first, never below [NativeAdDimens.MinMediaSize] ([fullMediaHeight],
 * plan T-044): the call to action, the star row and the two body lines keep their size, and the card stays 418 dp up
 * to font scale 2.0. Only past the floor (font scales a device does not offer) does the card grow. The call to action
 * is 36 dp at least, as high as its line of text with large text (plan T-045).
 */
@Composable
private fun FullLayout(assets: NativeAdAssets, binder: NativeAdBinder) {
    MediaShrinksFirstLayout(
        innerHeight = NativeAdDimens.FullHeight - MediumInnerTop - MediumInnerBottom,
        gap = NativeAdDimens.PartGap,
        mediaIndex = 1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = InnerStart, top = MediumInnerTop, end = InnerStart, bottom = MediumInnerBottom),
    ) {
        NativeAdHeader(assets, binder, iconSize = NativeAdDimens.FullIconSize)
        NativeAdMediaArea(binder, Modifier.fillMaxWidth())
        val bodyModifier = Modifier.fillMaxWidth().heightIn(min = NativeAdDimens.BodyHeight)
        val body = assets.body
        if (body != null) {
            NativeAdBody(
                body,
                binder,
                maxLines = FULL_BODY_LINES,
                modifier = bodyModifier,
                minLines = FULL_BODY_LINES,
            )
        } else {
            Spacer(bodyModifier)
        }
        val ctaModifier = Modifier.fillMaxWidth().heightIn(min = NativeAdDimens.CallToActionHeight)
        val callToAction = assets.callToAction
        if (callToAction != null) NativeAdCallToAction(callToAction, binder, ctaModifier) else Spacer(ctaModifier)
        NativeAdStarRow(assets.starRating, binder, Modifier.fillMaxWidth().height(NativeAdDimens.StarRowHeight))
    }
}

private const val FULL_BODY_LINES = 2

/**
 * Lays out the parts of a native ad from the top, [gap] apart, the part at [mediaIndex] being the media: the media
 * shrinks first (plans T-044, T-045). Every other part is measured at its own height (the width is fixed); the media
 * takes what [innerHeight] (or the least height the parent sets, when more) leaves after them
 * ([fullMediaHeight]), never below [NativeAdDimens.MinMediaSize]. Past that floor the layout grows instead. A `Column`
 * cannot hold this floor (a weighted child gets a fixed constraint), so the parts are measured and placed by hand.
 * Each child of [content] is one part.
 */
@Composable
internal fun MediaShrinksFirstLayout(
    innerHeight: Dp,
    gap: Dp,
    mediaIndex: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        check(mediaIndex in measurables.indices) { "No media at $mediaIndex among ${measurables.size} parts" }
        val width = constraints.maxWidth
        val free = Constraints(minWidth = width, maxWidth = width)
        val placeables = arrayOfNulls<Placeable>(measurables.size)
        measurables.forEachIndexed { index, part -> if (index != mediaIndex) placeables[index] = part.measure(free) }
        val gapPx = gap.roundToPx()
        val others = placeables.sumOf { it?.height ?: 0 } + gapPx * (measurables.size - 1)
        val inner = max(innerHeight.roundToPx(), constraints.minHeight)
        val mediaHeight = fullMediaHeight(inner, others, NativeAdDimens.MinMediaSize.roundToPx())
        placeables[mediaIndex] = measurables[mediaIndex].measure(Constraints.fixed(width, mediaHeight))
        layout(width, constraints.constrainHeight(others + mediaHeight)) {
            var y = 0
            placeables.forEach { part ->
                checkNotNull(part).placeRelative(0, y)
                y += part.height + gapPx
            }
        }
    }
}

/**
 * Height of the media of a native ad whose media shrinks first (full, medium, the step card, the collapsible layer;
 * plans T-044, T-045): what the [inner] height of the card leaves after the [others] (the other parts and their
 * gaps), never below [floor] (120 dp, KB-2). Past the floor the card grows instead.
 */
internal fun fullMediaHeight(inner: Int, others: Int, floor: Int): Int = max(inner - others, floor)

/**
 * Filled stars of the ad's rating (O-08); nothing when the ad has no rating above 0. The outer box keeps its height
 * either way. The stars are the ad's star rating view for the SDK (through [binder]), so a touch on them counts as a
 * touch on the ad (KB-2, plan T-060); with no stars nothing is registered.
 */
@Composable
private fun NativeAdStarRow(rating: Double?, binder: NativeAdBinder, modifier: Modifier = Modifier) {
    val count = nativeStarCount(rating)
    Box(modifier = modifier) {
        if (count > 0) {
            binder.Asset(NativeAdAsset.STAR_RATING, Modifier.height(NativeAdDimens.StarRowHeight)) {
                val description = stringResource(R.string.ads_native_star_rating, count)
                Row(
                    modifier = Modifier
                        .testTag(NativeAdTestTags.STAR_ROW)
                        .semantics { contentDescription = description },
                    horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.StarGap),
                ) {
                    val color = MaterialTheme.colorScheme.tertiary
                    repeat(count) { NativeAdStar(color, Modifier.size(NativeAdDimens.StarSize)) }
                }
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

/**
 * Icon, headline and the "Ad" line in one row: the head of the medium and full ads. With [starRow] (medium), the
 * 16 dp star row under the "Ad" line, kept empty when the ad has no rating, so the head stays 56 dp high.
 */
@Composable
internal fun NativeAdHeader(
    assets: NativeAdAssets,
    binder: NativeAdBinder,
    modifier: Modifier = Modifier,
    iconSize: Dp = NativeAdDimens.IconSize,
    starRow: Boolean = false,
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
            if (starRow) NativeAdStarRow(assets.starRating, binder, Modifier.height(NativeAdDimens.StarRowHeight))
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
internal fun NativeAdHeadline(
    text: String,
    binder: NativeAdBinder,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
) {
    binder.Asset(NativeAdAsset.HEADLINE, modifier.fillMaxWidth().testTag(NativeAdTestTags.HEADLINE)) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.merge(TextStyle(fontSize = fontSize, lineHeight = lineHeight)),
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
 * The call to action: tertiary container, at least [height] high (36 dp, 44 dp on small, 44 dp or 56 dp on medium);
 * with large text it is as high as one line of its text (plan T-045). 8 dp corners, unlike the app's PrimaryButton.
 * It is a plain box, not a Button, so a touch reaches the SDK's call-to-action view.
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
                .heightIn(min = height)
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

/**
 * The media of the ad (image or video), 8 dp corners. With [backdrop] (medium), an `outlineVariant` layer at 0.6
 * under the media fills what the image leaves free (the "Blurred backdrop" of the design, drawn as a token: plan
 * T-028, question 4).
 */
@Composable
internal fun NativeAdMediaArea(binder: NativeAdBinder, modifier: Modifier = Modifier, backdrop: Boolean = false) {
    val shape = RoundedCornerShape(NativeAdDimens.AssetCorner)
    if (!backdrop) {
        binder.Media(modifier.clip(shape).testTag(NativeAdTestTags.MEDIA))
        return
    }
    Box(modifier = modifier.clip(shape)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = MEDIA_BACKDROP_ALPHA)),
        )
        binder.Media(Modifier.matchParentSize().testTag(NativeAdTestTags.MEDIA))
    }
}

private const val MEDIA_BACKDROP_ALPHA = 0.6f

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
 * of another height (collapsible, full screen) reuses it; [mediumCallToActionHeight] is the call-to-action block of
 * medium (36 dp by default, the collapsible form; 44 dp or 56 dp for the medium ad).
 */
@Composable
internal fun NativeAdPlaceholderLayout(
    size: NativeAdSize,
    height: Dp,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    mediumCallToActionHeight: Dp = NativeAdDimens.CallToActionHeight,
) {
    val alpha = placeholderAlpha(animated)
    NativeAdFrame(modifier = modifier.fillMaxWidth().height(height).testTag(NativeAdTestTags.PLACEHOLDER)) {
        val pulse = Modifier.graphicsLayer { this.alpha = alpha.value }
        when (size) {
            NativeAdSize.SMALL -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = InnerStart, vertical = NativeAdDimens.SmallTierGap)
                    .then(pulse),
                verticalArrangement = Arrangement.spacedBy(NativeAdDimens.SmallTierGap),
            ) {
                SmallPlaceholderBlocks()
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
                MediumPlaceholderBlocks(mediumCallToActionHeight)
            }
            NativeAdSize.CARD -> Column(
                modifier = Modifier.fillMaxSize().padding(NativeAdDimens.CardPadding).then(pulse),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                CardPlaceholderBlocks()
            }
            NativeAdSize.STEP -> Column(
                modifier = Modifier.fillMaxSize().padding(NativeAdDimens.InnerPadding).then(pulse),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                CardPlaceholderBlocks(NativeAdDimens.StepIconSize, NativeAdDimens.StepCallToActionHeight)
            }
        }
    }
}

/**
 * Small (design/context/SampleScreen_ad-loading_light.md, two tiers since T-108): the top row (44 dp icon, a 14 dp bar
 * as wide as the text column and a 12 x 120 dp bar, 8 dp apart; 16 dp end margin), then a 44 dp call-to-action block
 * as wide as the card. The top row takes what the call to action leaves, so with larger text the block keeps its
 * 6 dp to the bottom.
 */
@Composable
private fun ColumnScope.SmallPlaceholderBlocks() {
    Row(
        modifier = Modifier.fillMaxWidth().weight(1f).padding(end = NativeAdDimens.SmallTopRowEnd),
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaceholderBlock(Modifier.size(NativeAdDimens.SmallMediumIconSize).testTag(NativeAdTestTags.PLACEHOLDER_ICON))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            val bar = RoundedCornerShape(NativeAdDimens.LineHeight / 2)
            PlaceholderBlock(
                Modifier
                    .fillMaxWidth()
                    .height(NativeAdDimens.SmallPlaceholderTopLineHeight)
                    .testTag(NativeAdTestTags.PLACEHOLDER_LINE),
                bar,
            )
            PlaceholderBlock(
                Modifier
                    .size(NativeAdDimens.ShortLineWidth, NativeAdDimens.SmallPlaceholderBottomLineHeight)
                    .testTag(NativeAdTestTags.PLACEHOLDER_LINE),
                bar,
            )
        }
    }
    PlaceholderBlock(
        Modifier
            .fillMaxWidth()
            .height(NativeAdDimens.MediumCallToActionHeight)
            .testTag(NativeAdTestTags.PLACEHOLDER_CTA),
    )
}

@Composable
private fun ColumnScope.MediumPlaceholderBlocks(callToActionHeight: Dp) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaceholderBlock(Modifier.size(NativeAdDimens.SmallMediumIconSize))
        PlaceholderLines(Modifier.weight(1f))
    }
    PlaceholderBlock(Modifier.fillMaxWidth().weight(1f))
    PlaceholderLines(Modifier.fillMaxWidth().height(NativeAdDimens.BodyHeight))
    PlaceholderBlock(
        Modifier.fillMaxWidth().height(callToActionHeight).testTag(NativeAdTestTags.PLACEHOLDER_CTA),
    )
}

@Composable
private fun ColumnScope.CardPlaceholderBlocks(
    iconSize: Dp = NativeAdDimens.CardIconSize,
    callToActionHeight: Dp = NativeAdDimens.CallToActionHeight,
) {
    PlaceholderBlock(Modifier.fillMaxWidth().weight(1f))
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaceholderBlock(Modifier.size(iconSize))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            val bar = RoundedCornerShape(NativeAdDimens.LineHeight / 2)
            PlaceholderBlock(Modifier.fillMaxWidth().height(NativeAdDimens.LineHeight), bar)
            PlaceholderBlock(Modifier.fillMaxWidth(fraction = CARD_SHORT_LINE).height(NativeAdDimens.LineHeight), bar)
        }
    }
    PlaceholderBlock(
        Modifier.fillMaxWidth().height(callToActionHeight).testTag(NativeAdTestTags.PLACEHOLDER_CTA),
    )
}

private const val CARD_SHORT_LINE = 0.6f

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

/** A soft pulse of the loading blocks; full opacity, fixed, when not [animated]. Shared with the full-screen one. */
@Composable
internal fun placeholderAlpha(animated: Boolean): State<Float> {
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
            NativeAdCard(assets.copy(starRating = 4.0), NativeAdSize.MEDIUM)
            NativeAdCard(assets.copy(starRating = 4.0), NativeAdSize.FULL)
            NativeAdCard(assets, NativeAdSize.CARD, Modifier.size(180.dp, 300.dp))
            NativeAdCard(assets, NativeAdSize.STEP)
        }
    }
}
