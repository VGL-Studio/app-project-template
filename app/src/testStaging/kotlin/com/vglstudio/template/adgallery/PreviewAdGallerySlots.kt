package com.vglstudio.template.adgallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vglstudio.template.core.ads.BottomAdBarDefaults
import com.vglstudio.template.core.ads.BottomAdBarLayout
import com.vglstudio.template.core.ads.CollapsibleNativeAdCard
import com.vglstudio.template.core.ads.NativeAdAssets
import com.vglstudio.template.core.ads.NativeAdCard
import com.vglstudio.template.core.ads.NativeAdSize
import com.vglstudio.template.core.ads.NativeFullScreenStepContent
import com.vglstudio.template.core.ads.inListAdPositions
import com.vglstudio.template.core.designsystem.theme.Spacing

/** Fixed ad content of the design's frames (design README, section 2): the SDK fills real ads. */
val previewAdAssets = NativeAdAssets(
    headline = "Headline of the ad",
    advertiser = "Advertiser or short body",
    body = "Body text of the ad, two lines at most so the call to action stays in view.",
    callToAction = "Install",
    icon = null,
    hasMedia = true,
)

/** Test tags of the fake ads, so tests find them without knowing the ads module's own tags. */
object PreviewAdTags {
    const val SMALL = "preview_ad_small"
    const val MEDIUM = "preview_ad_medium"
    const val COLLAPSIBLE = "preview_ad_collapsible"
    const val BOTTOM_SLOT = "preview_ad_bottom_slot"

    const val BANNER = "preview_ad_banner"
    const val COLLAPSIBLE_BANNER = "preview_ad_collapsible_banner"
    const val BANNER_MREC = "preview_ad_banner_mrec"
    const val COLLECTION_CARD = "preview_ad_collection_card"
    const val STEP_CARD = "preview_ad_step_card"
    const val STEP_FULL_PAGE = "preview_ad_step_full_page"
    const val PRIVACY = "preview_privacy_button"

    /** The [slot]-th ad of the in-list page; small for even slots, medium for odd ones. */
    fun inList(slot: Int): String = "preview_inlist_ad_$slot"

    /** The [slot]-th card of the grid page. */
    fun grid(slot: Int): String = "preview_grid_ad_$slot"
}

/** Height of the fake banners: a 60 dp box, about the height of an adaptive banner on a phone. */
private val PreviewBannerHeight = 60.dp

/**
 * Fake ads for tests and screenshots of [AdGalleryScreen]: fixed content drawn with the ads module's preview cards.
 * In the list, ads go where [inListAdPositions] puts them, 16 dp above and below, small and medium in turn, as the
 * real page does with two placements. [every] null means in-list ads are off.
 */
class PreviewAdGallerySlots(
    private val assets: NativeAdAssets = previewAdAssets,
    private val collapsed: Boolean = false,
    private val every: Int? = 3,
    /** N of the grid page, or null while its ads are off. */
    private val gridEvery: Int? = 4,
    /** Whether the page of a flow step has an ad (plan T-032). */
    stepFullPageReady: Boolean = false,
    /** The card of a flow step draws nothing, as a card with no ad. */
    private val stepCardEmpty: Boolean = false,
) : AdGallerySlots {
    /** What [stepFullPageReady] answers now; a test may change it while the screen shows. */
    var readyNow by mutableStateOf(stepFullPageReady)

    /** Every `viewed` the pager card got, in order. */
    val viewedValues = mutableListOf<Boolean>()

    /** Every `step` the step card got, in order. */
    val stepValues = mutableListOf<Int>()

    /** Every `shown` the page of a flow step got, in order. */
    val shownValues = mutableListOf<Boolean>()

    @Composable
    override fun Small(modifier: Modifier) {
        NativeAdCard(assets, NativeAdSize.SMALL, modifier.testTag(PreviewAdTags.SMALL))
    }

    @Composable
    override fun Medium(modifier: Modifier) {
        NativeAdCard(assets, NativeAdSize.MEDIUM, modifier.testTag(PreviewAdTags.MEDIUM))
    }

    /**
     * The bottom bar of the collapsible page: the real layout of the bar with the native small card as its slot and,
     * unless [collapsed], the open collapsible layer over it; the content keeps the space of a slot with an ad.
     */
    @Composable
    override fun BottomBar(content: @Composable (PaddingValues) -> Unit) {
        BottomAdBarLayout(
            reservation = BottomAdBarDefaults.nativeReservation,
            slot = { NativeAdCard(assets, NativeAdSize.SMALL, it.testTag(PreviewAdTags.BOTTOM_SLOT)) },
            layer = if (collapsed) {
                null
            } else {
                {
                    CollapsibleNativeAdCard(
                        assets.copy(advertiser = "Advertiser", body = null),
                        onCollapse = {},
                        modifier = it.testTag(PreviewAdTags.COLLAPSIBLE),
                    )
                }
            },
            content = content,
        )
    }

    @Composable
    override fun inListEvery(): Int? = every

    override fun LazyListScope.listWithAds(items: List<Int>, itemContent: @Composable LazyItemScope.(Int) -> Unit) {
        val positions = every?.let { inListAdPositions(items.size, it) }.orEmpty()
        var itemIndex = 0
        for (row in 0 until items.size + positions.size) {
            val slot = positions.indexOf(row)
            if (slot >= 0) {
                item(key = "ad_$slot") {
                    val size = if (slot % 2 == 0) NativeAdSize.SMALL else NativeAdSize.MEDIUM
                    Box(Modifier.fillMaxWidth().testTag(PreviewAdTags.inList(slot)).padding(vertical = Spacing.md)) {
                        NativeAdCard(assets, size)
                    }
                }
            } else {
                val item = items[itemIndex++]
                item(key = item) { itemContent(item) }
            }
        }
    }

    @Composable
    override fun Banner(modifier: Modifier) {
        PreviewBanner("Banner", modifier.testTag(PreviewAdTags.BANNER))
    }

    @Composable
    override fun CollapsibleBanner(modifier: Modifier) {
        PreviewBanner("Collapsible banner", modifier.testTag(PreviewAdTags.COLLAPSIBLE_BANNER))
    }

    /** A 300 x 250 dp box at the top centre of [modifier], as the real MREC banner in its frame (plan T-062). */
    @Composable
    override fun MediumRectangleBanner(modifier: Modifier) {
        Box(modifier, contentAlignment = Alignment.TopCenter) {
            Box(
                modifier = Modifier
                    .size(width = 300.dp, height = 250.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .testTag(PreviewAdTags.BANNER_MREC),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "MREC banner",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    override fun gridEvery(): Int? = gridEvery

    override fun LazyGridScope.gridWithAds(items: List<Int>, itemContent: @Composable LazyGridItemScope.(Int) -> Unit) {
        val positions = gridEvery?.let { inListAdPositions(items.size, it) }.orEmpty()
        var itemIndex = 0
        for (cell in 0 until items.size + positions.size) {
            val slot = positions.indexOf(cell)
            if (slot >= 0) {
                item(key = "ad_$slot") {
                    NativeAdCard(assets, NativeAdSize.CARD, Modifier.testTag(PreviewAdTags.grid(slot)))
                }
            } else {
                val item = items[itemIndex++]
                item(key = item) { itemContent(item) }
            }
        }
    }

    @Composable
    override fun CollectionCard(viewed: Boolean, onScreen: Boolean, modifier: Modifier) {
        viewedValues += viewed
        NativeAdCard(assets, NativeAdSize.CARD, modifier.testTag(PreviewAdTags.COLLECTION_CARD))
    }

    @Composable
    override fun StepCard(step: Int, modifier: Modifier) {
        stepValues += step
        if (stepCardEmpty) return
        NativeAdCard(assets, NativeAdSize.STEP, modifier.testTag(PreviewAdTags.STEP_CARD))
    }

    @Composable
    override fun stepFullPageReady(): Boolean = readyNow

    @Composable
    override fun StepFullPage(shown: Boolean, modifier: Modifier) {
        shownValues += shown
        NativeFullScreenStepContent(assets, modifier.testTag(PreviewAdTags.STEP_FULL_PAGE))
    }

    @Composable
    override fun PrivacyButton(modifier: Modifier) {
        TextButton(onClick = {}, modifier = modifier.testTag(PreviewAdTags.PRIVACY)) {
            Text("Privacy options")
        }
    }
}

/** A fake banner: a box of the banner's height with its name, in the colors of an empty slot. */
@Composable
private fun PreviewBanner(label: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PreviewBannerHeight)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
