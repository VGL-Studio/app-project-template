package com.vglstudio.template.adgallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.vglstudio.template.core.ads.CollapsibleNativeAdCard
import com.vglstudio.template.core.ads.NativeAdAssets
import com.vglstudio.template.core.ads.NativeAdCard
import com.vglstudio.template.core.ads.NativeAdSize
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

    /** The [slot]-th ad of the in-list page; small for even slots, medium for odd ones. */
    fun inList(slot: Int): String = "preview_inlist_ad_$slot"
}

/**
 * Fake ads for tests and screenshots of [AdGalleryScreen]: fixed content drawn with the ads module's preview cards.
 * In the list, ads go where [inListAdPositions] puts them, 16 dp above and below, small and medium in turn, as the
 * real page does with two placements. [every] null means in-list ads are off.
 */
class PreviewAdGallerySlots(
    private val assets: NativeAdAssets = previewAdAssets,
    private val collapsed: Boolean = false,
    private val every: Int? = 3,
) : AdGallerySlots {
    @Composable
    override fun Small(modifier: Modifier) {
        NativeAdCard(assets, NativeAdSize.SMALL, modifier.testTag(PreviewAdTags.SMALL))
    }

    @Composable
    override fun Medium(modifier: Modifier) {
        NativeAdCard(assets, NativeAdSize.MEDIUM, modifier.testTag(PreviewAdTags.MEDIUM))
    }

    @Composable
    override fun Collapsible(modifier: Modifier) {
        Box(modifier.testTag(PreviewAdTags.COLLAPSIBLE)) {
            CollapsibleNativeAdCard(assets, collapsed = collapsed, onCollapse = {})
        }
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
}
