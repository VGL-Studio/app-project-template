package com.vglstudio.template.adgallery

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.template.core.ads.ConsentPath
import com.vglstudio.template.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The staging ad gallery with fake ads, named as the design frames `AdGalleryScreen_*` (ADR-0020), plus the low
 * frame of each page (L-028). Nothing animates on these pages (fake ads, no loading state), so the screen settles
 * (L-048). Measurements of the low frame are in AdGalleryScreenTest. The collapsible page draws the bottom bar
 * with fake ads ([PreviewAdGallerySlots.BottomBar], plan T-030).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AdGalleryScreenScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(
        uiState: AdGalleryUiState,
        slots: AdGallerySlots,
        darkTheme: Boolean = false,
        status: GalleryAdStatus = GalleryAdStatus.Unknown,
    ) {
        composeRule.setContent {
            AppTheme(darkTheme = darkTheme) {
                AdGalleryScreen(
                    uiState = uiState,
                    slots = slots,
                    onOpenInList = {},
                    onOpenCollapsible = {},
                    onShowInterstitial = {},
                    onShowNativeFullScreen = {},
                    onOpenAdInspector = {},
                    status = status,
                )
            }
        }
    }

    /** Swipes the pager page to the third page, the one with the card. */
    private fun swipeToCardPage() {
        repeat(2) {
            composeRule.onNodeWithTag(AdGalleryTestTags.PAGER).performTouchInput { swipeLeft() }
            composeRule.waitForIdle()
        }
    }

    private fun next() {
        composeRule.onNodeWithTag(AdGalleryTestTags.FLOW_NEXT).performClick()
        composeRule.waitForIdle()
    }

    /** The "Ad status" page of the screenshots: a temporary right after a consent error (KB-11). */
    private val temporaryStatus = GalleryAdStatus(
        consentPath = ConsentPath.ERROR_ALLOWED,
        adsAllowed = true,
        temporaryRight = true,
        privacyOptionsRequired = true,
        sdkReady = true,
        consentShortcutRemote = false,
        nativeAfterInter = true,
        nativeBottom = true,
        nativeCollapsible = true,
    )

    // Plan T-036: the pages of the new formats, with no design frame (question 1); these images are the reference.

    @Test
    fun defaultMore_light() {
        setScreen(AdGalleryUiState.Default, PreviewAdGallerySlots())
        composeRule.onNodeWithTag(AdGalleryTestTags.STATUS_BUTTON).performScrollTo()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_default-more_light.png")
    }

    @Test
    fun banners_light() {
        setScreen(AdGalleryUiState.Banners(30), PreviewAdGallerySlots())
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_banners_light.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun banners_small() {
        setScreen(AdGalleryUiState.Banners(30), PreviewAdGallerySlots())
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_banners_small.png")
    }

    @Test
    fun grid_light() {
        setScreen(AdGalleryUiState.Grid(30), PreviewAdGallerySlots(gridEvery = 4))
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_grid_light.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun grid_small() {
        setScreen(AdGalleryUiState.Grid(30), PreviewAdGallerySlots(gridEvery = 4))
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_grid_small.png")
    }

    @Test
    fun pager_light() {
        setScreen(AdGalleryUiState.Pager(6), PreviewAdGallerySlots())
        swipeToCardPage()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_pager_light.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun pager_small() {
        setScreen(AdGalleryUiState.Pager(6), PreviewAdGallerySlots())
        swipeToCardPage()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_pager_small.png")
    }

    @Test
    fun flowCard_light() {
        setScreen(AdGalleryUiState.FlowSteps(3), PreviewAdGallerySlots())
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_flow-card_light.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun flowCard_small() {
        setScreen(AdGalleryUiState.FlowSteps(3), PreviewAdGallerySlots())
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_flow-card_small.png")
    }

    @Test
    fun flowFullPage_light() {
        setScreen(AdGalleryUiState.FlowSteps(3), PreviewAdGallerySlots(stepFullPageReady = true))
        next()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_flow-full-page_light.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun flowFullPage_small() {
        setScreen(AdGalleryUiState.FlowSteps(3), PreviewAdGallerySlots(stepFullPageReady = true))
        next()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_flow-full-page_small.png")
    }

    @Test
    @Config(qualifiers = "w640dp-h360dp-xxhdpi")
    fun flowFullPage_landscape() {
        setScreen(AdGalleryUiState.FlowSteps(3), PreviewAdGallerySlots(stepFullPageReady = true))
        next()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_flow-full-page_landscape.png")
    }

    @Test
    fun rewarded_light() {
        setScreen(AdGalleryUiState.Rewarded(GalleryRewarded.ALLOW_AFTER, actionsRun = 3), PreviewAdGallerySlots())
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_rewarded_light.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun rewarded_small() {
        setScreen(AdGalleryUiState.Rewarded(GalleryRewarded.ALLOW_AFTER, actionsRun = 3), PreviewAdGallerySlots())
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_rewarded_small.png")
    }

    @Test
    fun status_light() {
        setScreen(AdGalleryUiState.Status, PreviewAdGallerySlots(), status = temporaryStatus)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_status_light.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun status_small() {
        setScreen(AdGalleryUiState.Status, PreviewAdGallerySlots(), status = temporaryStatus)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_status_small.png")
    }

    @Test
    fun default_light() {
        setScreen(AdGalleryUiState.Default, PreviewAdGallerySlots())
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_default_light.png")
    }

    @Test
    fun default_dark() {
        setScreen(AdGalleryUiState.Default, PreviewAdGallerySlots(), darkTheme = true)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_default_dark.png")
    }

    @Test
    fun inlist_light() {
        setScreen(AdGalleryUiState.InList(7), PreviewAdGallerySlots(every = 3))
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_inlist_light.png")
    }

    @Test
    fun collapsibleExpanded_light() {
        setScreen(AdGalleryUiState.Collapsible(6), PreviewAdGallerySlots(collapsed = false))
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_collapsible-expanded_light.png")
    }

    @Test
    fun collapsibleCollapsed_light() {
        setScreen(AdGalleryUiState.Collapsible(6), PreviewAdGallerySlots(collapsed = true))
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_collapsible-collapsed_light.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun default_small() {
        setScreen(AdGalleryUiState.Default, PreviewAdGallerySlots())
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_default_small.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun inlist_small() {
        setScreen(AdGalleryUiState.InList(7), PreviewAdGallerySlots(every = 3))
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_inlist_small.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun collapsibleExpanded_small() {
        setScreen(AdGalleryUiState.Collapsible(6), PreviewAdGallerySlots(collapsed = false))
        composeRule.onRoot().captureRoboImage("src/test/screenshots/AdGalleryScreen_collapsible-expanded_small.png")
    }
}
