package com.vglstudio.template.core.ads

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.core.logging.RecordingAppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The stateful native slot: gate, what each loading state draws, and destroying the ad (plan T-010). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeAdSlotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setSlotContent(state: NativeAdState, size: NativeAdSize, keepSpaceWhenEmpty: Boolean = false) {
        composeRule.setContent {
            AppTheme {
                NativeAdSlotContent(
                    state = state,
                    size = size,
                    keepSpaceWhenEmpty = keepSpaceWhenEmpty,
                    modifier = Modifier.width(CARD_WIDTH).testTag(SLOT),
                    adContent = { NativeAdCard(it.assets, size) },
                )
            }
        }
    }

    @Test
    fun loading_showsPlaceholder() {
        setSlotContent(NativeAdState.Loading, NativeAdSize.SMALL)
        composeRule.onNodeWithTag(NativeAdTestTags.PLACEHOLDER).assertHeightIsEqualTo(72.dp)
    }

    @Test
    fun unavailable_takesNoSpace() {
        var state by mutableStateOf<NativeAdState>(NativeAdState.Loading)
        composeRule.setContent {
            AppTheme {
                NativeAdSlotContent(
                    state = state,
                    size = NativeAdSize.SMALL,
                    keepSpaceWhenEmpty = false,
                    modifier = Modifier.width(CARD_WIDTH).testTag(SLOT),
                    adContent = { NativeAdCard(it.assets, NativeAdSize.SMALL) },
                )
            }
        }
        // Control: the same slot takes 72 dp while loading, so 0 dp below is the slot giving its space back.
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(72.dp)

        state = NativeAdState.Unavailable
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(0.dp)
    }

    @Test
    fun unavailable_keepsSpace_whenDeclaredNextToButton() {
        var size by mutableStateOf(NativeAdSize.SMALL)
        composeRule.setContent {
            AppTheme {
                NativeAdSlotContent(
                    state = NativeAdState.Unavailable,
                    size = size,
                    keepSpaceWhenEmpty = true,
                    modifier = Modifier.width(CARD_WIDTH).testTag(SLOT),
                    adContent = { NativeAdCard(it.assets, size) },
                )
            }
        }
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(72.dp)
        composeRule.onAllNodes(hasAnyText, useUnmergedTree = true).assertCountEquals(0)

        size = NativeAdSize.MEDIUM
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(386.dp)
        composeRule.onAllNodes(hasAnyText, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun buttonDoesNotMove_whenAdBecomesUnavailable_withKeptSpace() {
        var state by mutableStateOf<NativeAdState>(NativeAdState.Loading)
        composeRule.setContent {
            AppTheme {
                Column {
                    NativeAdSlotContent(
                        state = state,
                        size = NativeAdSize.SMALL,
                        keepSpaceWhenEmpty = true,
                        modifier = Modifier.width(CARD_WIDTH),
                        adContent = { NativeAdCard(it.assets, NativeAdSize.SMALL) },
                    )
                    Text("App button", modifier = Modifier.testTag(BUTTON))
                }
            }
        }
        val before = composeRule.onNodeWithTag(BUTTON).getUnclippedBoundsInRoot()
        // Control: the button sits below the 72 dp placeholder, so the comparison below is not 0 against 0.
        assertEquals(72f, before.top.value, 0.5f)

        state = NativeAdState.Unavailable
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(NativeAdTestTags.PLACEHOLDER).assertCountEquals(0)
        val after = composeRule.onNodeWithTag(BUTTON).getUnclippedBoundsInRoot()
        assertEquals(before.top.value, after.top.value, 0.5f)
    }

    @Test
    fun withoutAdsController_takesNoSpace() {
        composeRule.setContent {
            AppTheme {
                NativeAdSlot(AdPlacement.SAMPLE_NATIVE_SMALL, Modifier.width(CARD_WIDTH).testTag(SLOT))
            }
        }
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(0.dp)
    }

    @Test
    fun nonNativeFormat_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { requireNativeFormat(AdFormat.BANNER) }
        assertThrows(IllegalArgumentException::class.java) { requireNativeFormat(AdFormat.INTERSTITIAL) }
    }

    @Test
    fun nativeSize_followsPlacementFormat() {
        assertEquals(NativeAdSize.SMALL, requireNativeFormat(AdFormat.NATIVE_SMALL))
        assertEquals(NativeAdSize.MEDIUM, requireNativeFormat(AdFormat.NATIVE_MEDIUM))
        assertEquals(NativeAdSize.SMALL, requireNativeFormat(AdPlacement.SAMPLE_NATIVE_SMALL.format))
    }

    @Test
    fun nativeLoadSize_mapsFormats() {
        assertEquals(null, AdFormat.BANNER.nativeLoadSize())
        assertEquals(null, AdFormat.INTERSTITIAL.nativeLoadSize())
        assertEquals(NativeAdSize.SMALL, AdFormat.NATIVE_SMALL.nativeLoadSize())
        assertEquals(NativeAdSize.MEDIUM, AdFormat.NATIVE_MEDIUM.nativeLoadSize())
    }

    @Test
    fun leavingComposition_destroysAd() {
        val loader = FakeNativeAdLoader()
        val ad = FakeLoadedNativeAd()
        val states = nativeAdStates(loader, "unit", NativeAdSize.SMALL, 10_000, RecordingAppLogger(), "key")
        var shown by mutableStateOf(true)
        composeRule.setContent {
            AppTheme {
                if (shown) {
                    NativeAdSlotHost(
                        states = states,
                        size = NativeAdSize.SMALL,
                        keepSpaceWhenEmpty = false,
                        modifier = Modifier.width(CARD_WIDTH),
                        adContent = { NativeAdCard(it.assets, NativeAdSize.SMALL) },
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { loader.deliver(ad) }
        composeRule.onNodeWithText("Headline of the ad", useUnmergedTree = true).assertExists()
        assertEquals(0, ad.destroyCount)

        shown = false
        composeRule.waitForIdle()

        assertEquals(1, ad.destroyCount)
    }

    @Test
    fun boundNativeAd_withFakeAd_drawsCard() {
        composeRule.setContent {
            AppTheme {
                BoundNativeAd(FakeLoadedNativeAd(sampleNativeAdAssets), NativeAdSize.SMALL, Modifier.width(CARD_WIDTH))
            }
        }
        composeRule.onNodeWithText("Headline of the ad", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(72.dp)
    }

    private companion object {
        val CARD_WIDTH = 379.dp
        const val SLOT = "slot"
        const val BUTTON = "button"
        val hasAnyText = SemanticsMatcher("has text") { SemanticsProperties.Text in it.config }
    }
}
