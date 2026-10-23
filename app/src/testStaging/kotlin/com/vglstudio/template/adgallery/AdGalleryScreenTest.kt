package com.vglstudio.template.adgallery

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.R
import com.vglstudio.template.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.vglstudio.template.core.ads.R as AdsR

/** The staging ad gallery with fake ads: pages, actions, design numbers and the low frame (plan T-014, section 4). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AdGalleryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val calls = mutableMapOf<String, Int>()

    private fun count(name: String): () -> Unit = { calls[name] = (calls[name] ?: 0) + 1 }

    private fun setScreen(uiState: AdGalleryUiState, slots: AdGallerySlots = PreviewAdGallerySlots()) {
        composeRule.setContent {
            AppTheme {
                AdGalleryScreen(
                    uiState = uiState,
                    slots = slots,
                    onOpenInList = count("inList"),
                    onOpenCollapsible = count("collapsible"),
                    onShowInterstitial = count("interstitial"),
                    onShowNativeFullScreen = count("nativeFullScreen"),
                    onOpenAdInspector = count("inspector"),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(tag).getBoundsInRoot()

    private fun textBounds(text: String): DpRect = composeRule.onNodeWithText(text).getBoundsInRoot()

    private fun rootHeight(): Dp = composeRule.onRoot().getBoundsInRoot().height

    private fun assertDp(message: String, expected: Dp, actual: Dp) {
        assertEquals(message, expected.value, actual.value, TOLERANCE_DP)
    }

    private fun assertInsideScreen(name: String, rect: DpRect) {
        assertTrue("$name starts above the screen: $rect", rect.top.value >= -TOLERANCE_DP)
        assertTrue("$name ends below the screen (${rootHeight()}): $rect", rect.bottom <= rootHeight() + TOLERANCE_DP.dp)
    }

    @Test
    fun default_showsSmallMediumAndButtons() {
        setScreen(AdGalleryUiState.Default)

        listOf("Native small", "Native medium", "Interstitial", "Native full screen", "Open Ad Inspector")
            .forEach { composeRule.onAllNodesWithText(it).assertCountEquals(1) }
        composeRule.onAllNodesWithTag(PreviewAdTags.SMALL).assertCountEquals(1)
        composeRule.onAllNodesWithTag(PreviewAdTags.MEDIUM).assertCountEquals(1)
        composeRule.onAllNodesWithTag(AdGalleryTestTags.DEFAULT).assertCountEquals(1)
    }

    @Test
    fun buttons_callTheirActions() {
        setScreen(AdGalleryUiState.Default)

        listOf(
            AdGalleryTestTags.INTERSTITIAL_BUTTON,
            AdGalleryTestTags.NATIVE_FULL_SCREEN_BUTTON,
            AdGalleryTestTags.INSPECTOR_BUTTON,
            AdGalleryTestTags.IN_LIST_BUTTON,
            AdGalleryTestTags.COLLAPSIBLE_BUTTON,
        ).forEach { composeRule.onNodeWithTag(it).performScrollTo().performClick() }

        assertEquals(
            mapOf("interstitial" to 1, "nativeFullScreen" to 1, "inspector" to 1, "inList" to 1, "collapsible" to 1),
            calls,
        )
    }

    @Test
    fun inList_insertsAdsAfterEveryNItems() {
        setScreen(AdGalleryUiState.InList(7), PreviewAdGallerySlots(every = 3))

        composeRule.onAllNodesWithText("In-list ads (every 3 items)").assertCountEquals(1)
        val firstAd = bounds(PreviewAdTags.inList(0))
        assertTrue("ad first in the list", textBounds("List item 1").top < firstAd.top)
        assertTrue(firstAd.top >= textBounds("List item 3").bottom)
        assertTrue(firstAd.bottom <= textBounds("List item 4").top)
        // Small: 72 dp and 16 dp above and below.
        assertDp("first ad row height", 104.dp, firstAd.height)

        composeRule.onNodeWithTag(AdGalleryTestTags.IN_LIST)
            .performScrollToNode(hasText("List item 7"))
        val secondAd = bounds(PreviewAdTags.inList(1))
        assertTrue(secondAd.top >= textBounds("List item 6").bottom)
        assertTrue(secondAd.bottom <= textBounds("List item 7").top)
        // Medium: 386 dp and 16 dp above and below.
        assertDp("second ad row height", 418.dp, secondAd.height)
    }

    @Test
    fun inList_withoutN_showsOffTitle() {
        setScreen(AdGalleryUiState.InList(7), PreviewAdGallerySlots(every = null))

        composeRule.onAllNodesWithText("In-list ads (off)").assertCountEquals(1)
        composeRule.onAllNodesWithText("List item 1").assertCountEquals(1)
        composeRule.onAllNodesWithTag(PreviewAdTags.inList(0)).assertCountEquals(0)
    }

    @Test
    fun collapsible_pinsAdAtBottom() {
        setScreen(AdGalleryUiState.Collapsible(6), PreviewAdGallerySlots(collapsed = false))

        composeRule.onAllNodesWithText("Collapsible ad").assertCountEquals(1)
        val ad = bounds(PreviewAdTags.COLLAPSIBLE)
        val root = composeRule.onRoot().getBoundsInRoot()
        assertDp("gap below the ad", 16.dp, root.bottom - ad.bottom)
        assertDp("start margin", 16.dp, ad.left - root.left)
        assertDp("end margin", 16.dp, root.right - ad.right)
        // Control: the ad is the expanded form, so it is pinned with its own height, not stretched.
        assertDp("expanded height", 342.dp, ad.height)
    }

    @Test
    fun default_layoutMatchesDesign() {
        setScreen(AdGalleryUiState.Default)

        val title = bounds(AdGalleryTestTags.TITLE)
        assertDp("title x", 16.dp, title.left)
        assertDp("title y", 20.dp, title.top)
        assertDp("label y", 72.dp, textBounds("Native small").top)

        val medium = bounds(PreviewAdTags.MEDIUM)
        val interstitial = bounds(AdGalleryTestTags.INTERSTITIAL_BUTTON)
        val nativeFullScreen = bounds(AdGalleryTestTags.NATIVE_FULL_SCREEN_BUTTON)
        assertDp("interstitial height", 52.dp, interstitial.height)
        assertDp("native full screen height", 52.dp, nativeFullScreen.height)
        assertDp("gap between the two buttons", 12.dp, nativeFullScreen.left - interstitial.right)
        assertDp("gap below medium", 16.dp, interstitial.top - medium.bottom)
        assertDp("same width", interstitial.width, nativeFullScreen.width)
        assertDp("inspector width", 379.dp, bounds(AdGalleryTestTags.INSPECTOR_BUTTON).width)
    }

    @Test
    fun texts_matchDesignTable() {
        assertEquals("Ad gallery (staging)", context.getString(R.string.ad_gallery_title))
        assertEquals("Native small", context.getString(R.string.ad_gallery_native_small))
        assertEquals("Native medium", context.getString(R.string.ad_gallery_native_medium))
        assertEquals("Interstitial", context.getString(R.string.ad_gallery_interstitial))
        assertEquals("Native full screen", context.getString(R.string.ad_gallery_native_full_screen))
        assertEquals("Open Ad Inspector", context.getString(R.string.ad_gallery_open_inspector))
        assertEquals("In-list ads (every 3 items)", context.getString(R.string.ad_gallery_inlist_title, 3))
        assertEquals("List item 1", context.getString(R.string.ad_gallery_list_item, 1))
        assertEquals("Collapsible ad", context.getString(R.string.ad_gallery_collapsible_title))
    }

    @Test
    fun buttons_haveTouchTargetOf48dp() {
        setScreen(AdGalleryUiState.Default)

        val buttons = composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .fetchSemanticsNodes()
            .filter { node -> node.config.contains(SemanticsProperties.TestTag) }
        // Control: the five buttons of the page are found.
        assertEquals(5, buttons.size)
        buttons.forEach { node ->
            val height = with(composeRule.density) { node.boundsInRoot.height.toDp() }
            assertTrue("${node.config} is $height high", height >= 48.dp - TOLERANCE_DP.dp)
        }
        composeRule.onNodeWithTag(AdGalleryTestTags.IN_LIST_BUTTON).assert(hasClickAction())
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun default_smallScreen_scrollsToEveryButton() {
        setScreen(AdGalleryUiState.Default)

        composeRule.onNodeWithTag(AdGalleryTestTags.INSPECTOR_BUTTON).performScrollTo()
        assertInsideScreen("Open Ad Inspector", bounds(AdGalleryTestTags.INSPECTOR_BUTTON))
        composeRule.onNodeWithTag(AdGalleryTestTags.IN_LIST_BUTTON).performScrollTo()
        assertInsideScreen("In-list ads", bounds(AdGalleryTestTags.IN_LIST_BUTTON))
        assertInsideScreen("Collapsible ad", bounds(AdGalleryTestTags.COLLAPSIBLE_BUTTON))
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun inList_smallScreen_scrollsToLastItem() {
        setScreen(AdGalleryUiState.InList(7), PreviewAdGallerySlots(every = 3))

        // Control: the last item is not composed before scrolling, so the scroll is what brings it.
        composeRule.onAllNodesWithText("List item 7").assertCountEquals(0)
        composeRule.onNodeWithTag(AdGalleryTestTags.IN_LIST).performScrollToNode(hasText("List item 7"))
        assertInsideScreen("List item 7", textBounds("List item 7"))
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun collapsibleExpanded_smallScreen_fitsAndScrolls() {
        setScreen(AdGalleryUiState.Collapsible(6), PreviewAdGallerySlots(collapsed = false))

        val collapse = composeRule.onNodeWithContentDescription(context.getString(AdsR.string.ads_collapse))
            .getBoundsInRoot()
        assertInsideScreen("collapse button", collapse)
        assertInsideScreen("call to action", textBounds("Install"))
        composeRule.onNodeWithTag(AdGalleryTestTags.COLLAPSIBLE_LIST).performScrollToNode(hasText("List item 6"))
        assertInsideScreen("List item 6", textBounds("List item 6"))
        // The list stays above the pinned ad.
        assertTrue(textBounds("List item 6").bottom <= bounds(PreviewAdTags.COLLAPSIBLE).top)
    }

    private companion object {
        const val TOLERANCE_DP = 1f
    }
}
