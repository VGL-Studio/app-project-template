package com.vglstudio.template.adgallery

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
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
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.R
import com.vglstudio.template.core.ads.ConsentPath
import com.vglstudio.template.core.ads.R as AdsR
import com.vglstudio.template.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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

    private val policies = mutableListOf<GalleryRewarded>()

    private fun setScreen(
        uiState: AdGalleryUiState,
        slots: AdGallerySlots = PreviewAdGallerySlots(),
        status: GalleryAdStatus = GalleryAdStatus.Unknown,
    ) {
        composeRule.setContent {
            AppTheme { Screen(uiState, slots, status) }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Screen(uiState: AdGalleryUiState, slots: AdGallerySlots, status: GalleryAdStatus) {
        AdGalleryScreen(
            uiState = uiState,
            slots = slots,
            onOpenInList = count("inList"),
            onOpenCollapsible = count("collapsible"),
            onShowInterstitial = count("interstitial"),
            onShowNativeFullScreen = count("nativeFullScreen"),
            onOpenAdInspector = count("inspector"),
            status = status,
            onShowAppOpen = count("appOpen"),
            onOpenBanners = count("banners"),
            onOpenGrid = count("grid"),
            onOpenPager = count("pager"),
            onOpenFlowSteps = count("flowSteps"),
            onOpenRewarded = count("rewarded"),
            onOpenStatus = count("status"),
            onStartRewarded = count("startRewarded"),
            onSelectRewardedPolicy = { policies += it },
        )
    }

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(tag).getBoundsInRoot()

    private fun textBounds(text: String): DpRect = composeRule.onNodeWithText(text).getBoundsInRoot()

    private fun rootHeight(): Dp = composeRule.onRoot().getBoundsInRoot().height

    private fun assertDp(message: String, expected: Dp, actual: Dp) {
        assertEquals(message, expected.value, actual.value, TOLERANCE_DP)
    }

    private fun assertInsideScreen(name: String, rect: DpRect) {
        assertTrue("$name starts above the screen: $rect", rect.top.value >= -TOLERANCE_DP)
        assertTrue(
            "$name ends below the screen (${rootHeight()}): $rect",
            rect.bottom <= rootHeight() + TOLERANCE_DP.dp,
        )
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
        // Small: 80 dp and 16 dp above and below.
        assertDp("first ad row height", 112.dp, firstAd.height)

        composeRule.onNodeWithTag(AdGalleryTestTags.IN_LIST)
            .performScrollToNode(hasText("List item 7"))
        val secondAd = bounds(PreviewAdTags.inList(1))
        assertTrue(secondAd.top >= textBounds("List item 6").bottom)
        assertTrue(secondAd.bottom <= textBounds("List item 7").top)
        // Medium: 410 dp and 16 dp above and below.
        assertDp("second ad row height", 442.dp, secondAd.height)
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
        // Control: the ad is the open layer, so it is pinned with its own height, not stretched.
        assertDp("open layer height", 250.dp, ad.height)
        // Design: the layer floats over the 80 dp slot, same bottom edge (y 625 to 875).
        assertDp("layer top", 625.dp, ad.top)
        assertDp("same bottom as the slot", bounds(PreviewAdTags.BOTTOM_SLOT).bottom, ad.bottom)
    }

    @Test
    fun collapsibleCollapsed_slotAt795() {
        setScreen(AdGalleryUiState.Collapsible(6), PreviewAdGallerySlots(collapsed = true))

        composeRule.onAllNodesWithTag(PreviewAdTags.COLLAPSIBLE).assertCountEquals(0)
        val slot = bounds(PreviewAdTags.BOTTOM_SLOT)
        assertDp("slot top", 795.dp, slot.top)
        assertDp("slot height", 80.dp, slot.height)
        assertDp("start margin", 16.dp, slot.left)
        // The list ends where the slot starts (D-04).
        assertDp("list bottom", 795.dp, bounds(AdGalleryTestTags.COLLAPSIBLE_LIST).bottom)
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

    private val planT014Buttons = setOf(
        AdGalleryTestTags.INTERSTITIAL_BUTTON,
        AdGalleryTestTags.NATIVE_FULL_SCREEN_BUTTON,
        AdGalleryTestTags.INSPECTOR_BUTTON,
        AdGalleryTestTags.IN_LIST_BUTTON,
        AdGalleryTestTags.COLLAPSIBLE_BUTTON,
    )

    @Test
    fun buttons_haveTouchTargetOf48dp() {
        setScreen(AdGalleryUiState.Default)

        val allTagged = composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .fetchSemanticsNodes()
            .filter { node -> node.config.contains(SemanticsProperties.TestTag) }
        // Control: every button of the page is found: five of plan T-014, seven of "More formats" (T-036).
        assertEquals(
            planT014Buttons + moreFormatButtons.toSet(),
            allTagged.map { it.config[SemanticsProperties.TestTag] }.toSet(),
        )
        assertEquals(12, allTagged.size)
        // Every button of the tree, scrolled into view first: one below the screen measures 0 dp (plan T-044).
        allTagged.map { it.config[SemanticsProperties.TestTag] }.forEach { tag ->
            composeRule.onNodeWithTag(tag).performScrollTo()
            val rect = bounds(tag)
            val height = rect.bottom - rect.top
            val width = rect.right - rect.left
            assertTrue("$tag is $height high", height >= 48.dp - TOLERANCE_DP.dp)
            assertTrue("$tag is $width wide", width >= 48.dp - TOLERANCE_DP.dp)
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
        moreFormatButtons.forEach { tag ->
            composeRule.onNodeWithTag(tag).performScrollTo()
            assertInsideScreen(tag, bounds(tag))
        }
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
        // The call to action of the layer and of the slot below it.
        val callsToAction = composeRule.onAllNodesWithText("Install").fetchSemanticsNodes()
        assertEquals(2, callsToAction.size)
        callsToAction.forEach { node ->
            val px = node.boundsInRoot
            val rect = with(composeRule.density) {
                DpRect(px.left.toDp(), px.top.toDp(), px.right.toDp(), px.bottom.toDp())
            }
            assertInsideScreen("call to action", rect)
        }
        composeRule.onNodeWithTag(AdGalleryTestTags.COLLAPSIBLE_LIST).performScrollToNode(hasText("List item 6"))
        assertInsideScreen("List item 6", textBounds("List item 6"))
        // The list scrolls to above the slot; the floating layer keeps no space (D-04).
        assertTrue(textBounds("List item 6").bottom <= bounds(PreviewAdTags.BOTTOM_SLOT).top + TOLERANCE_DP.dp)
    }

    // Plan T-036: "More formats" and the six pages.

    private val moreFormatButtons = listOf(
        AdGalleryTestTags.APP_OPEN_BUTTON,
        AdGalleryTestTags.BANNERS_BUTTON,
        AdGalleryTestTags.GRID_BUTTON,
        AdGalleryTestTags.PAGER_BUTTON,
        AdGalleryTestTags.FLOW_BUTTON,
        AdGalleryTestTags.REWARDED_BUTTON,
        AdGalleryTestTags.STATUS_BUTTON,
    )

    private fun click(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.waitForIdle()
    }

    private fun exists(tag: String): Boolean = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun moreFormats_buttonsCallTheirActions() {
        setScreen(AdGalleryUiState.Default)

        moreFormatButtons.forEach { composeRule.onNodeWithTag(it).performScrollTo().performClick() }

        assertEquals(
            mapOf(
                "appOpen" to 1,
                "banners" to 1,
                "grid" to 1,
                "pager" to 1,
                "flowSteps" to 1,
                "rewarded" to 1,
                "status" to 1,
            ),
            calls,
        )
    }

    @Test
    fun moreFormats_buttonsHaveTouchTargetOf48dp() {
        setScreen(AdGalleryUiState.Default)

        moreFormatButtons.forEach { tag ->
            composeRule.onNodeWithTag(tag).performScrollTo()
            val rect = bounds(tag)
            assertTrue("$tag is ${rect.height} high", rect.height >= 48.dp - TOLERANCE_DP.dp)
            assertTrue("$tag is ${rect.width} wide", rect.width >= 48.dp - TOLERANCE_DP.dp)
        }
    }

    @Test
    fun moreFormats_textsFromStrings() {
        val expected = mapOf(
            R.string.ad_gallery_more_formats to "More formats",
            R.string.ad_gallery_app_open to "App open",
            R.string.ad_gallery_open_banners to "Banners",
            R.string.ad_gallery_open_grid to "Grid ads",
            R.string.ad_gallery_open_pager to "Pager ads",
            R.string.ad_gallery_open_flow to "Flow steps",
            R.string.ad_gallery_open_rewarded to "Rewarded",
            R.string.ad_gallery_open_status to "Ad status",
            R.string.ad_gallery_banners_title to "Banners",
            R.string.ad_gallery_pager_title to "Pager ads",
            R.string.ad_gallery_flow_title to "Flow steps",
            R.string.ad_gallery_rewarded_title to "Rewarded",
            R.string.ad_gallery_status_title to "Ad status",
            R.string.ad_gallery_grid_title_off to "Grid ads (off)",
        )
        expected.forEach { (id, text) -> assertEquals(text, context.getString(id)) }
        assertEquals("Grid ads (every 4 items)", context.getString(R.string.ad_gallery_grid_title, 4))

        // The buttons show these strings.
        setScreen(AdGalleryUiState.Default)
        listOf(
            R.string.ad_gallery_more_formats,
            R.string.ad_gallery_app_open,
            R.string.ad_gallery_open_banners,
            R.string.ad_gallery_open_grid,
            R.string.ad_gallery_open_pager,
            R.string.ad_gallery_open_flow,
            R.string.ad_gallery_open_rewarded,
            R.string.ad_gallery_open_status,
        ).forEach { id -> composeRule.onNodeWithText(context.getString(id)).performScrollTo() }
    }

    @Test
    fun banners_fixedOnTop_collapsibleAtBottom() {
        setScreen(AdGalleryUiState.Banners(30))

        val title = bounds(AdGalleryTestTags.TITLE)
        val banner = bounds(PreviewAdTags.BANNER)
        val collapsible = bounds(PreviewAdTags.COLLAPSIBLE_BANNER)
        val list = bounds(AdGalleryTestTags.BANNERS_LIST)
        // Right below the title and its bottom gap, as the lists of the other pages.
        assertDp("banner below the title", 24.dp, banner.top - title.bottom)
        assertDp("banner full width", composeRule.onRoot().getBoundsInRoot().width, banner.width)
        assertDp("collapsible at the bottom of the screen", 891.dp, collapsible.bottom)
        assertTrue("list below the banner", list.top >= banner.bottom - TOLERANCE_DP.dp)
        assertTrue("list above the collapsible banner", list.bottom <= collapsible.top + TOLERANCE_DP.dp)

        composeRule.onNodeWithTag(AdGalleryTestTags.BANNERS_LIST).performScrollToNode(hasText("List item 30"))
        assertTrue(
            "last item not covered by the collapsible banner",
            textBounds("List item 30").bottom <= collapsible.top + TOLERANCE_DP.dp,
        )
    }

    @Test
    fun grid_insertsCardsAfterEveryNItems() {
        setScreen(AdGalleryUiState.Grid(12), PreviewAdGallerySlots(gridEvery = 4))

        composeRule.onAllNodesWithText("Grid ads (every 4 items)").assertCountEquals(1)
        val firstAd = bounds(PreviewAdTags.grid(0))
        // inListAdPositions(12, 4): the first card is the fifth cell, after "Item 4", in the third row.
        assertTrue("after Item 4", firstAd.top >= textBounds("Item 4").bottom)
        assertDp("one column wide", bounds(AdGalleryTestTags.gridItem(1)).width, firstAd.width)
        assertDp("in the first column", bounds(AdGalleryTestTags.gridItem(1)).left, firstAd.left)
    }

    @Test
    fun grid_withoutN_showsOffTitle_noCards() {
        setScreen(AdGalleryUiState.Grid(12), PreviewAdGallerySlots(gridEvery = null))

        composeRule.onAllNodesWithText("Grid ads (off)").assertCountEquals(1)
        composeRule.onAllNodesWithText("Item 1").assertCountEquals(1)
        composeRule.onAllNodesWithTag(PreviewAdTags.grid(0)).assertCountEquals(0)
    }

    @Test
    fun pager_cardViewedOnlyWhenSettled() {
        val slots = PreviewAdGallerySlots()
        setScreen(AdGalleryUiState.Pager(6), slots)

        composeRule.onAllNodesWithText("Swipe, page 1 of 6").assertCountEquals(1)
        assertTrue("not viewed on page 1: ${slots.viewedValues}", slots.viewedValues.none { it })

        repeat(2) {
            composeRule.onNodeWithTag(AdGalleryTestTags.PAGER).performTouchInput { swipeLeft() }
            composeRule.waitForIdle()
        }

        composeRule.onAllNodesWithText("Swipe, page 3 of 6").assertCountEquals(1)
        assertEquals(true, slots.viewedValues.lastOrNull())
        assertTrue(exists(PreviewAdTags.COLLECTION_CARD))
    }

    @Test
    fun flowSteps_cardOutsidePager_followsStep() {
        val slots = PreviewAdGallerySlots()
        setScreen(AdGalleryUiState.FlowSteps(3), slots)

        assertEquals(0, slots.stepValues.last())
        composeRule.onAllNodesWithText("Step 1 of 3").assertCountEquals(1)
        click(AdGalleryTestTags.FLOW_NEXT)
        assertEquals(1, slots.stepValues.last())
        composeRule.onAllNodesWithText("Step 2 of 3").assertCountEquals(1)

        composeRule.onNode(
            hasTestTag(PreviewAdTags.STEP_CARD) and hasAnyAncestor(hasTestTag(AdGalleryTestTags.FLOW_PAGER)),
        ).assertDoesNotExist()
        // Control: the card is there, below the pager.
        assertTrue(exists(PreviewAdTags.STEP_CARD))
        assertTrue(bounds(PreviewAdTags.STEP_CARD).top >= bounds(AdGalleryTestTags.FLOW_PAGER).bottom - TOLERANCE_DP.dp)
    }

    @Test
    fun flowSteps_fullPageOnlyWhenReady() {
        val slots = PreviewAdGallerySlots(stepFullPageReady = true)
        setScreen(AdGalleryUiState.FlowSteps(3), slots)

        assertTrue("not shown on step 1: ${slots.shownValues}", slots.shownValues.none { it })
        click(AdGalleryTestTags.FLOW_NEXT)
        assertTrue(exists(PreviewAdTags.STEP_FULL_PAGE))
        assertEquals(true, slots.shownValues.last())

        // Four pages: step 1, the ad page, steps 2 and 3, then back to step 1.
        click(AdGalleryTestTags.FLOW_NEXT)
        composeRule.onAllNodesWithText("Step 2 of 3").assertCountEquals(1)
        click(AdGalleryTestTags.FLOW_NEXT)
        composeRule.onAllNodesWithText("Step 3 of 3").assertCountEquals(1)
        click(AdGalleryTestTags.FLOW_NEXT)
        composeRule.onAllNodesWithText("Step 1 of 3").assertCountEquals(1)
    }

    @Test
    fun flowSteps_notReady_noFullPage() {
        // Control of the test above (L-055): no ad, three pages, the ad page never built.
        val slots = PreviewAdGallerySlots(stepFullPageReady = false)
        setScreen(AdGalleryUiState.FlowSteps(3), slots)

        click(AdGalleryTestTags.FLOW_NEXT)
        click(AdGalleryTestTags.FLOW_NEXT)
        composeRule.onAllNodesWithText("Step 3 of 3").assertCountEquals(1)
        assertFalse(exists(PreviewAdTags.STEP_FULL_PAGE))
        click(AdGalleryTestTags.FLOW_NEXT)
        composeRule.onAllNodesWithText("Step 1 of 3").assertCountEquals(1)
        assertTrue(slots.shownValues.isEmpty())
    }

    @Test
    fun flowSteps_restoredMidFlow_keepsStepAndPageCount() {
        // A rotation in the middle of the flow: the pager keeps its page, and the number of pages must not change
        // although the slot reads "not ready" in the first frame after the restore (T-032, question 4).
        val slots = PreviewAdGallerySlots(stepFullPageReady = true)
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            AppTheme { Screen(AdGalleryUiState.FlowSteps(3), slots, GalleryAdStatus.Unknown) }
        }
        composeRule.waitForIdle()
        click(AdGalleryTestTags.FLOW_NEXT)
        assertTrue(exists(PreviewAdTags.STEP_FULL_PAGE))
        click(AdGalleryTestTags.FLOW_NEXT)
        composeRule.onAllNodesWithText("Step 2 of 3").assertCountEquals(1)

        slots.readyNow = false
        restoration.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("Step 2 of 3").assertCountEquals(1)
        click(AdGalleryTestTags.FLOW_NEXT)
        composeRule.onAllNodesWithText("Step 3 of 3").assertCountEquals(1)
        click(AdGalleryTestTags.FLOW_NEXT)
        composeRule.onAllNodesWithText("Step 1 of 3").assertCountEquals(1)
        // Still four pages: Next from step 1 opens the ad page again.
        click(AdGalleryTestTags.FLOW_NEXT)
        assertTrue(exists(PreviewAdTags.STEP_FULL_PAGE))
    }

    @Test
    fun flowSteps_readyTurnsFalseMidFlow_keepsFourPages() {
        // The slot loses its ad while the flow runs: the pages stay as they were when the flow started.
        val slots = PreviewAdGallerySlots(stepFullPageReady = true)
        setScreen(AdGalleryUiState.FlowSteps(3), slots)
        click(AdGalleryTestTags.FLOW_NEXT)
        assertTrue(exists(PreviewAdTags.STEP_FULL_PAGE))

        slots.readyNow = false
        composeRule.waitForIdle()
        click(AdGalleryTestTags.FLOW_NEXT)
        composeRule.onAllNodesWithText("Step 2 of 3").assertCountEquals(1)
        click(AdGalleryTestTags.FLOW_NEXT)
        composeRule.onAllNodesWithText("Step 3 of 3").assertCountEquals(1)
        click(AdGalleryTestTags.FLOW_NEXT)
        composeRule.onAllNodesWithText("Step 1 of 3").assertCountEquals(1)
    }

    @Test
    fun flowSteps_fullPage_hidesCard() {
        val slots = PreviewAdGallerySlots(stepFullPageReady = true)
        setScreen(AdGalleryUiState.FlowSteps(3), slots)

        assertTrue(exists(PreviewAdTags.STEP_CARD))
        click(AdGalleryTestTags.FLOW_NEXT)
        assertTrue(exists(PreviewAdTags.STEP_FULL_PAGE))
        assertFalse("one native per step", exists(PreviewAdTags.STEP_CARD))

        click(AdGalleryTestTags.FLOW_NEXT)
        assertTrue(exists(PreviewAdTags.STEP_CARD))
        assertEquals(2, slots.stepValues.last())
    }

    @Test
    fun flowSteps_nextBounds_sameOnCardFullPageAndEmptyCard() {
        var slots by mutableStateOf<AdGallerySlots>(PreviewAdGallerySlots(stepFullPageReady = true))
        composeRule.setContent {
            AppTheme { Screen(AdGalleryUiState.FlowSteps(3), slots, GalleryAdStatus.Unknown) }
        }
        composeRule.waitForIdle()

        val onCard = bounds(AdGalleryTestTags.FLOW_NEXT)
        assertTrue(exists(PreviewAdTags.STEP_CARD))
        click(AdGalleryTestTags.FLOW_NEXT)
        val onFullPage = bounds(AdGalleryTestTags.FLOW_NEXT)
        assertTrue(exists(PreviewAdTags.STEP_FULL_PAGE))
        click(AdGalleryTestTags.FLOW_NEXT)
        slots = PreviewAdGallerySlots(stepFullPageReady = true, stepCardEmpty = true)
        composeRule.waitForIdle()
        assertFalse("the card draws nothing", exists(PreviewAdTags.STEP_CARD))
        val onEmptyCard = bounds(AdGalleryTestTags.FLOW_NEXT)

        listOf("full page" to onFullPage, "empty card" to onEmptyCard).forEach { (name, rect) ->
            assertEquals("$name top", onCard.top.value, rect.top.value, 0.5f)
            assertEquals("$name left", onCard.left.value, rect.left.value, 0.5f)
            assertEquals("$name bottom", onCard.bottom.value, rect.bottom.value, 0.5f)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun flowSteps_smallScreen_nextNotCovered() {
        setScreen(AdGalleryUiState.FlowSteps(3), PreviewAdGallerySlots(stepFullPageReady = true))

        // A step with the card: the 250 dp frame, Next below it.
        val onStep = bounds(AdGalleryTestTags.FLOW_NEXT)
        val frame = bounds(AdGalleryTestTags.FLOW_CARD_FRAME)
        assertInsideScreen("Next on step", onStep)
        assertTrue("Next below the card frame on step", onStep.top >= frame.bottom - TOLERANCE_DP.dp)
        assertDp("card frame height", 250.dp, frame.height)

        // The full page (plan T-041): no card frame, the page stops above Next.
        click(AdGalleryTestTags.FLOW_NEXT)
        val onFullPage = bounds(AdGalleryTestTags.FLOW_NEXT)
        val page = bounds(PreviewAdTags.STEP_FULL_PAGE)
        assertFalse("no card frame on the full page", exists(AdGalleryTestTags.FLOW_CARD_FRAME))
        assertInsideScreen("Next on full page", onFullPage)
        assertTrue("page $page stops above Next $onFullPage", page.bottom <= onFullPage.top + TOLERANCE_DP.dp)
    }

    private fun unmergedBounds(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getBoundsInRoot()

    private fun unmergedTextBounds(text: String): DpRect =
        composeRule.onNodeWithText(text, useUnmergedTree = true).getBoundsInRoot()

    private fun assertInside(name: String, part: DpRect, page: DpRect) {
        assertTrue("$name $part starts above the page $page", part.top.value >= page.top.value - TOLERANCE_DP)
        assertTrue("$name $part ends below the page $page", part.bottom.value <= page.bottom.value + TOLERANCE_DP)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun flowSteps_fullPage_fillsStepArea_small() {
        setScreen(AdGalleryUiState.FlowSteps(3), PreviewAdGallerySlots(stepFullPageReady = true))
        click(AdGalleryTestTags.FLOW_NEXT)

        val page = unmergedBounds(PreviewAdTags.STEP_FULL_PAGE)
        val next = bounds(AdGalleryTestTags.FLOW_NEXT)
        assertEquals("page top: $page", 72f, page.top.value, 0.5f)
        assertEquals("page bottom: $page, Next $next", (next.top - 16.dp).value, page.bottom.value, 0.5f)
        assertEquals("page height: $page", 484f, page.height.value, 0.5f)
        assertFalse("no card frame on the full page", exists(AdGalleryTestTags.FLOW_CARD_FRAME))
        val cta = unmergedTextBounds(previewAdAssets.callToAction!!)
        assertInside("CTA text", cta, page)
        assertEquals("CTA text height: $cta", 20f, cta.height.value, 0.5f)
    }

    private fun assertFullPageShowsCta() {
        setScreen(AdGalleryUiState.FlowSteps(3), PreviewAdGallerySlots(stepFullPageReady = true))
        click(AdGalleryTestTags.FLOW_NEXT)

        val page = unmergedBounds(PreviewAdTags.STEP_FULL_PAGE)
        assertTrue("page of at least 200 dp: $page", page.height.value >= 200f - 0.5f)
        assertInside("CTA text", unmergedTextBounds(previewAdAssets.callToAction!!), page)
    }

    @Test
    @Config(qualifiers = "w640dp-h360dp-xxhdpi")
    fun flowSteps_fullPage_landscape_showsCta() = assertFullPageShowsCta()

    @Test
    @Config(qualifiers = "w891dp-h411dp-xxhdpi")
    fun flowSteps_fullPage_landscapeWide_showsCta() = assertFullPageShowsCta()

    @Test
    fun flowSteps_pagerHeight_sameOnCardAndFullPage() {
        setScreen(AdGalleryUiState.FlowSteps(3), PreviewAdGallerySlots(stepFullPageReady = true))
        val onCard = bounds(AdGalleryTestTags.FLOW_PAGER)
        assertTrue(exists(PreviewAdTags.STEP_CARD))

        click(AdGalleryTestTags.FLOW_NEXT)
        assertTrue(exists(PreviewAdTags.STEP_FULL_PAGE))
        assertEquals("the pager does not jump between steps", onCard, bounds(AdGalleryTestTags.FLOW_PAGER))
    }

    @Test
    fun rewarded_selectorPicksPolicy_runStarts() {
        setScreen(AdGalleryUiState.Rewarded(GalleryRewarded.BLOCK, actionsRun = 2))

        composeRule.onNodeWithTag(AdGalleryTestTags.REWARDED_BLOCK).assertIsSelected()
        composeRule.onNodeWithTag(AdGalleryTestTags.REWARDED_ALLOW).assertIsNotSelected()
        composeRule.onNodeWithTag(AdGalleryTestTags.REWARDED_ALLOW_AFTER).assertIsNotSelected()

        click(AdGalleryTestTags.REWARDED_ALLOW_AFTER)
        assertEquals(listOf(GalleryRewarded.ALLOW_AFTER), policies)
        click(AdGalleryTestTags.REWARDED_RUN)
        assertEquals(1, calls["startRewarded"])
        composeRule.onAllNodesWithText("Action ran 2 times").assertCountEquals(1)
        composeRule.onAllNodesWithText("Placement: gallery_rw_action").assertCountEquals(1)

        listOf(
            AdGalleryTestTags.REWARDED_BLOCK,
            AdGalleryTestTags.REWARDED_ALLOW,
            AdGalleryTestTags.REWARDED_ALLOW_AFTER,
        ).forEach { tag ->
            assertTrue("$tag row", bounds(tag).height >= 48.dp - TOLERANCE_DP.dp)
        }
    }

    @Test
    fun status_showsPrivacyButton() {
        setScreen(AdGalleryUiState.Status)

        composeRule.onNodeWithTag(PreviewAdTags.PRIVACY).performScrollTo().assertHasClickAction().performClick()
    }

    @Test
    fun default_hasNoPrivacyButton() {
        setScreen(AdGalleryUiState.Default)

        composeRule.onAllNodesWithTag(PreviewAdTags.PRIVACY).assertCountEquals(0)
        // Control: the page is the default one.
        composeRule.onAllNodesWithTag(AdGalleryTestTags.DEFAULT).assertCountEquals(1)
    }

    @Test
    fun status_showsConsentAndSwitches() {
        setScreen(
            AdGalleryUiState.Status,
            status = GalleryAdStatus(
                consentPath = ConsentPath.ERROR_ALLOWED,
                adsAllowed = true,
                temporaryRight = true,
                privacyOptionsRequired = false,
                sdkReady = true,
                consentShortcutRemote = true,
                nativeAfterInter = true,
                nativeBottom = false,
                nativeCollapsible = null,
            ),
        )

        listOf(
            "Consent path: ERROR_ALLOWED",
            "Ads allowed now: yes",
            "Temporary right: yes",
            "Privacy options required: no",
            "SDK ready: yes",
            "Consent shortcut (remote): on",
            "Native after interstitial: on",
            "Native bottom slot: off",
            "Collapsible layer: pending",
        ).forEach { text ->
            composeRule.onAllNodesWithText(text).assertCountEquals(1)
            composeRule.onNodeWithText(text).performScrollTo()
        }
    }

    @Test
    fun status_unknown_isPending() {
        setScreen(AdGalleryUiState.Status, status = GalleryAdStatus.Unknown)

        composeRule.onAllNodesWithText("Consent path: pending").assertCountEquals(1)
        composeRule.onAllNodesWithText("Consent path: ERROR_ALLOWED").assertCountEquals(0)
    }

    private companion object {
        const val TOLERANCE_DP = 1f
    }
}
