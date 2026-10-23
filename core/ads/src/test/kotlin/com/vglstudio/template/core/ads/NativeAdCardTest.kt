package com.vglstudio.template.core.ads

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
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

/** The stateless native ad card and its loading placeholder: parts, sizes, AdChoices corner (plan T-010). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeAdCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val badge: String
        get() = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.ads_native_badge)

    private fun setCard(size: NativeAdSize, assets: NativeAdAssets = sampleNativeAdAssets) {
        composeRule.setContent {
            AppTheme { NativeAdCard(assets = assets, size = size, modifier = Modifier.width(CARD_WIDTH)) }
        }
    }

    private fun bounds(tag: String) = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    @Test
    fun small_showsHeadlineAdBadgeAdvertiserAndCta() {
        setCard(NativeAdSize.SMALL)
        composeRule.onNodeWithText("Headline of the ad", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(badge, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Advertiser or short body", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Install", useUnmergedTree = true).assertExists()
    }

    @Test
    fun small_withoutAdvertiser_showsBody() {
        setCard(NativeAdSize.SMALL, sampleNativeAdAssets.copy(advertiser = null))
        composeRule.onNodeWithText(sampleNativeAdAssets.body!!, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(badge, useUnmergedTree = true).assertExists()
    }

    @Test
    fun small_leavesAdChoicesCornerFree() {
        setCard(NativeAdSize.SMALL)
        val card = bounds(NativeAdTestTags.CARD)
        val cta = bounds(NativeAdTestTags.CTA)
        assertEquals(CARD_WIDTH.value, (card.right - card.left).value, 0.5f)
        assertTrue("CTA ends ${card.right - cta.right} from the card edge", card.right - cta.right >= 24.dp)
    }

    @Test
    fun medium_leavesAdChoicesCornerFree() {
        setCard(NativeAdSize.MEDIUM)
        val card = bounds(NativeAdTestTags.CARD)
        val icon = bounds(NativeAdTestTags.ICON)
        assertTrue("Icon starts ${icon.top - card.top} below the card top", icon.top - card.top >= 24.dp)
    }

    @Test
    fun cta_is36dpHigh() {
        setCard(NativeAdSize.SMALL)
        composeRule.onNodeWithTag(NativeAdTestTags.CTA, useUnmergedTree = true).assertHeightIsEqualTo(36.dp)
    }

    @Test
    fun small_is72dpHigh() {
        setCard(NativeAdSize.SMALL)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(72.dp)
    }

    @Test
    fun medium_is386dpHigh() {
        setCard(NativeAdSize.MEDIUM)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(386.dp)
    }

    @Test
    fun placeholder_matchesCardHeight_forEachSize() {
        var size by mutableStateOf(NativeAdSize.SMALL)
        composeRule.setContent {
            AppTheme {
                Column {
                    NativeAdCard(sampleNativeAdAssets, size, Modifier.width(CARD_WIDTH))
                    NativeAdPlaceholder(size, Modifier.width(CARD_WIDTH), animated = false)
                }
            }
        }
        NativeAdSize.entries.forEach { current ->
            size = current
            composeRule.waitForIdle()
            val card = bounds(NativeAdTestTags.CARD)
            val placeholder = bounds(NativeAdTestTags.PLACEHOLDER)
            val expected = if (current == NativeAdSize.SMALL) 72.dp else 386.dp
            assertEquals(expected.value, (card.bottom - card.top).value, 0.5f)
            assertEquals(
                "$current placeholder height",
                (card.bottom - card.top).value,
                (placeholder.bottom - placeholder.top).value,
                0.5f,
            )
        }
    }

    @Test
    fun longTexts_keepHeight() {
        val long = sampleNativeAdAssets.copy(headline = "H".repeat(40) + " " + "e".repeat(39), body = "b ".repeat(150))
        var size by mutableStateOf(NativeAdSize.SMALL)
        composeRule.setContent {
            AppTheme { NativeAdCard(long.copy(advertiser = null), size, Modifier.width(CARD_WIDTH)) }
        }
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(72.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.HEADLINE, useUnmergedTree = true).assertHeightIsEqualTo(20.dp)

        size = NativeAdSize.MEDIUM
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(386.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.HEADLINE, useUnmergedTree = true).assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun medium_hasMediaOfAtLeast120dp() {
        setCard(NativeAdSize.MEDIUM)
        val card = bounds(NativeAdTestTags.CARD)
        val media = bounds(NativeAdTestTags.MEDIA)
        assertEquals(CARD_WIDTH.value, (card.right - card.left).value, 0.5f)
        assertTrue("Media is ${media.right - media.left} wide", media.right - media.left >= 355.dp - 0.5.dp)
        assertEquals(196f, (media.bottom - media.top).value, 0.5f)
        assertTrue(media.right - media.left >= 120.dp && media.bottom - media.top >= 120.dp)
    }

    @Test
    fun small_hasNoMedia() {
        setCard(NativeAdSize.SMALL)
        // Control: the card itself is there, so the missing media node is not a missing card.
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertExists()
        composeRule.onNodeWithTag(NativeAdTestTags.MEDIA, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun badgeText_isAd() {
        var text = ""
        var labelSmall: TextStyle? = null
        composeRule.setContent {
            AppTheme {
                text = stringResource(R.string.ads_native_badge)
                labelSmall = MaterialTheme.typography.labelSmall
                NativeAdCard(sampleNativeAdAssets, NativeAdSize.SMALL, Modifier.width(CARD_WIDTH))
            }
        }
        assertEquals("Ad", text)
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText("Ad", useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
        val style = layouts.single().layoutInput.style
        assertEquals(labelSmall!!.fontSize, style.fontSize)
        assertEquals(labelSmall!!.fontWeight, style.fontWeight)
        assertEquals(labelSmall!!.lineHeight, style.lineHeight)
        // Control: the headline next to it is not labelSmall, so the check above reads the right node.
        layouts.clear()
        composeRule.onNodeWithText("Headline of the ad", useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
        assertTrue(layouts.single().layoutInput.style.fontSize != labelSmall!!.fontSize)
    }

    private companion object {
        val CARD_WIDTH = 379.dp
    }
}
