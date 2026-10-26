package com.vglstudio.template.feature.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.ads.BottomAdBarDefaults
import com.vglstudio.template.core.ads.BottomAdBarLayout
import com.vglstudio.template.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** UI behaviour of the stateless screen, run on the JVM with Robolectric (no emulator needed). */
@RunWith(AndroidJUnit4::class)
class SampleScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    @Test
    fun whenReady_thenShowsCount() {
        composeRule.setContent {
            AppTheme { SampleScreen(uiState = SampleUiState.Ready(count = 7), onIncrement = {}) }
        }
        composeRule.onNodeWithTag(SampleTestTags.COUNT).assertTextEquals("7")
    }

    @Test
    fun whenTapButton_thenCallsOnIncrement() {
        var taps = 0
        composeRule.setContent {
            AppTheme { SampleScreen(uiState = SampleUiState.Ready(count = 0), onIncrement = { taps++ }) }
        }
        composeRule.onNodeWithText(string(R.string.sample_increment)).performClick()
        assertEquals(1, taps)
    }

    @Test
    fun whenSaveFailed_thenShowsErrorMessage() {
        composeRule.setContent {
            AppTheme { SampleScreen(uiState = SampleUiState.Ready(count = 2, saveFailed = true), onIncrement = {}) }
        }
        composeRule.onNodeWithTag(SampleTestTags.SAVE_ERROR).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.sample_save_failed)).assertIsDisplayed()
    }

    @Test
    fun whenSaveSucceeded_thenNoErrorMessage() {
        composeRule.setContent {
            AppTheme { SampleScreen(uiState = SampleUiState.Ready(count = 2), onIncrement = {}) }
        }
        composeRule.onNodeWithTag(SampleTestTags.SAVE_ERROR).assertDoesNotExist()
    }

    @Test
    fun whenDisabled_thenShowsNotAvailableMessage() {
        composeRule.setContent {
            AppTheme { SampleScreen(uiState = SampleUiState.Disabled, onIncrement = {}) }
        }
        composeRule.onNodeWithText(string(R.string.sample_disabled)).assertIsDisplayed()
    }

    /**
     * Stands in for the bottom bar (tests have no ads controller): the real layout of the bar with an 80 dp box as
     * the native slot, the content keeping the space of a slot with an ad, and with [layer] a 250 dp box floating
     * over the slot like the open collapsible layer.
     */
    private fun fakeBar(layer: Boolean = false): @Composable (@Composable (PaddingValues) -> Unit) -> Unit =
        { content ->
            BottomAdBarLayout(
                reservation = BottomAdBarDefaults.nativeReservation,
                slot = { Box(modifier = it.height(80.dp).testTag(AD)) },
                layer = if (layer) {
                    { Box(modifier = it.height(250.dp).testTag(LAYER)) }
                } else {
                    null
                },
                content = content,
            )
        }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun whenReady_thenAdIsPinnedToBottom() {
        composeRule.setContent {
            AppTheme { SampleScreen(uiState = SampleUiState.Ready(count = 3), onIncrement = {}, bottomBar = fakeBar()) }
        }
        val screen = composeRule.onNodeWithTag(SampleTestTags.SCREEN).getUnclippedBoundsInRoot()
        val ad = composeRule.onNodeWithTag(AD).getUnclippedBoundsInRoot()
        assertEquals(16f, (screen.bottom - ad.bottom).value, 0.5f)
        assertEquals(379f, (ad.right - ad.left).value, 0.5f)
        assertEquals(80f, (ad.bottom - ad.top).value, 0.5f)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun whenReady_thenBarContentEndsAtSlotTop() {
        composeRule.setContent {
            AppTheme { SampleScreen(uiState = SampleUiState.Ready(count = 3), onIncrement = {}, bottomBar = fakeBar()) }
        }
        val content = composeRule.onNodeWithTag(SampleTestTags.CONTENT).getUnclippedBoundsInRoot()
        val ad = composeRule.onNodeWithTag(AD).getUnclippedBoundsInRoot()
        // D-04: the scrolling content ends where the slot starts, no gap and no overlap.
        assertEquals(ad.top.value, content.bottom.value, 1f)
        assertEquals(795f, ad.top.value, 1f)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun whenReady_thenAdIsAtLeast16dpBelowButton() {
        composeRule.setContent {
            AppTheme { SampleScreen(uiState = SampleUiState.Ready(count = 3), onIncrement = {}, bottomBar = fakeBar()) }
        }
        val button = composeRule.onNodeWithText(string(R.string.sample_increment)).getUnclippedBoundsInRoot()
        val ad = composeRule.onNodeWithTag(AD).getUnclippedBoundsInRoot()
        assertTrue("Ad starts ${ad.top - button.bottom} below the button", ad.top - button.bottom >= 16.dp)
    }

    @Test
    fun whenDisabled_thenNoAd() {
        assertNoAdOnceStateIs(SampleUiState.Disabled)
    }

    @Test
    fun whenLoading_thenNoAd() {
        assertNoAdOnceStateIs(SampleUiState.Loading)
    }

    /** Starts Ready (control: the bar is built), then moves to [state] and checks the bar is never built again. */
    private fun assertNoAdOnceStateIs(state: SampleUiState) {
        var uiState by mutableStateOf<SampleUiState>(SampleUiState.Ready(count = 3))
        var barCalls = 0
        val bar = fakeBar()
        composeRule.setContent {
            AppTheme {
                SampleScreen(uiState = uiState, onIncrement = {}, bottomBar = { content ->
                    barCalls++
                    bar(content)
                })
            }
        }
        composeRule.onNodeWithTag(AD).assertExists()
        assertTrue(barCalls > 0)

        barCalls = 0
        uiState = state
        composeRule.waitForIdle()

        assertEquals(0, barCalls)
        composeRule.onNodeWithTag(AD).assertDoesNotExist()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun whenSmallScreen_thenButtonAndAdDoNotOverlap() {
        composeRule.setContent {
            AppTheme {
                SampleScreen(
                    uiState = SampleUiState.Ready(count = 3, saveFailed = true),
                    onIncrement = {},
                    bottomBar = fakeBar(),
                )
            }
        }
        val buttonNode = composeRule.onNodeWithText(string(R.string.sample_increment))
        buttonNode.assertIsDisplayed()
        val button = buttonNode.getUnclippedBoundsInRoot()
        val ad = composeRule.onNodeWithTag(AD).getUnclippedBoundsInRoot()
        assertTrue("Ad starts ${ad.top - button.bottom} below the button", ad.top - button.bottom >= 16.dp)

        composeRule.onNodeWithTag(SampleTestTags.SAVE_ERROR).performScrollTo().assertIsDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun whenSmallScreen_thenOpenLayerDoesNotCoverButton() {
        composeRule.setContent {
            AppTheme {
                SampleScreen(
                    uiState = SampleUiState.Ready(count = 3, saveFailed = true),
                    onIncrement = {},
                    bottomBar = fakeBar(layer = true),
                )
            }
        }
        val button = composeRule.onNodeWithText(string(R.string.sample_increment)).getUnclippedBoundsInRoot()
        val layer = composeRule.onNodeWithTag(LAYER).getUnclippedBoundsInRoot()
        // L-028: the 250 dp layer floats over the bottom of the content, below the main button.
        assertEquals(374f, layer.top.value, 1f)
        assertTrue("layer starts ${layer.top - button.bottom} below the button", layer.top >= button.bottom)
    }

    private companion object {
        const val AD = "fake_ad"
        const val LAYER = "fake_layer"
    }
}
