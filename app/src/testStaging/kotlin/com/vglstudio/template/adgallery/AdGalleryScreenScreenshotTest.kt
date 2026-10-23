package com.vglstudio.template.adgallery

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.template.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The staging ad gallery with fake ads, named as the design frames `AdGalleryScreen_*` (ADR-0020), plus the low
 * frame of each page (L-028). Nothing animates on these pages (fake ads, no loading state), so the screen settles
 * (L-048). Measurements of the low frame are in AdGalleryScreenTest.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AdGalleryScreenScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(uiState: AdGalleryUiState, slots: AdGallerySlots, darkTheme: Boolean = false) {
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
                )
            }
        }
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
