package com.vglstudio.template.core.ads

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The native full-screen ad screen (KB-5): a 48 dp close button visible from the first frame, in the top left corner,
 * away from AdChoices and the call to action; sizes from design/context/NativeFullScreenAdScreen_default_light.md.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeFullScreenAdScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var closes = 0

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun setScreen(assets: NativeAdAssets = FakeNativeAdAssets.Design) {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                NativeFullScreenAdScreen(
                    assets = assets,
                    onClose = { closes++ },
                    media = { FakeMedia() },
                    adChoices = { FakeAdChoices() },
                )
            }
        }
    }

    @Test
    fun closeButton_isDisplayedAtFirstFrame() {
        composeRule.mainClock.autoAdvance = false
        setScreen()
        composeRule.onNodeWithTag(CLOSE)
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertContentDescriptionEquals(context.getString(R.string.ads_close_full_screen))
        assertEquals("Close ad", context.getString(R.string.ads_close_full_screen))
    }

    @Test
    fun closeButton_isAtLeast48dp() {
        setScreen()
        composeRule.onNodeWithTag(CLOSE)
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun closeButton_isTopLeft_awayFromAdChoices() {
        setScreen()
        val close = composeRule.onNodeWithTag(CLOSE).getUnclippedBoundsInRoot()
        val adChoices = composeRule.onNodeWithTag(FAKE_AD_CHOICES_TAG).getUnclippedBoundsInRoot()
        val cta = composeRule.onNodeWithTag(CTA).getUnclippedBoundsInRoot()
        assertEquals(16f, close.left.value, TOLERANCE)
        assertEquals(24f, close.top.value, TOLERANCE)
        assertTrue("AdChoices too close: $close, $adChoices", adChoices.left.value - close.right.value >= 200f)
        assertTrue("CTA too close: $close, $cta", cta.top.value - close.bottom.value > 600f)
    }

    @Test
    fun tapClose_callsOnCloseOnce() {
        setScreen()
        composeRule.onNodeWithTag(CLOSE).performClick()
        composeRule.waitForIdle()
        assertEquals(1, closes)
    }

    @Test
    fun texts_followDesign() {
        setScreen()
        val design = FakeNativeAdAssets.Design
        composeRule.onNodeWithText(design.headline).assertIsDisplayed()
        composeRule.onNodeWithText(design.advertiser!!).assertIsDisplayed()
        composeRule.onNodeWithText(design.body!!).assertIsDisplayed()
        composeRule.onNodeWithText(design.callToAction!!).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.ads_native_badge)).assertIsDisplayed()
        composeRule.onNodeWithTag(CTA).assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithTag(MEDIA).assertHeightIsAtLeast(120.dp)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun smallScreen_closeAndCtaInside() {
        setScreen(FakeNativeAdAssets.Longest)
        listOf(CLOSE, CTA).forEach { tag ->
            val bounds = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            assertTrue("$tag outside the screen: $bounds", bounds.left.value >= 0f && bounds.top.value >= 0f)
            assertTrue(
                "$tag outside the screen: $bounds",
                bounds.right.value <= 360f + TOLERANCE && bounds.bottom.value <= 640f + TOLERANCE,
            )
        }
        composeRule.onNodeWithTag(MEDIA).assertHeightIsAtLeast(120.dp)
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(FakeNativeAdAssets.Longest.body!!)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action!!
            .invoke(layouts)
        assertTrue("body has ${layouts.single().lineCount} lines", layouts.single().lineCount <= 3)
    }

    private companion object {
        const val CLOSE = "nativeFullScreenClose"
        const val CTA = "nativeFullScreenCta"
        const val MEDIA = "nativeFullScreenMedia"
        const val TOLERANCE = 0.5f
    }
}
