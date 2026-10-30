package com.vglstudio.template.core.ads

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AdSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The size of each banner format (plan T-067, ADR-0038): a fixed banner at the anchored adaptive size of JBase
 * (B-01, KB-18, KB-32), a collapsible banner at the large anchored size (B-03), and the height the banner slot of a
 * bottom bar reports for the space it keeps (KB-17).
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BannerAdSizeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val activity get() = composeRule.activity

    @Test
    fun fixedBanner_kindIsAnchored() {
        assertEquals(BannerSizeKind.ANCHORED, bannerSizeKind(AdFormat.BANNER))
    }

    @Test
    fun collapsibleBanner_kindStaysLarge() {
        assertEquals(BannerSizeKind.LARGE_ANCHORED, bannerSizeKind(AdFormat.BANNER_COLLAPSIBLE))
    }

    @Test
    fun nonBannerFormat_throws() {
        BANNER_FORMATS.forEach { format -> bannerSizeKind(format) }
        (AdFormat.entries - BANNER_FORMATS).forEach { format ->
            assertThrows(format.name, IllegalArgumentException::class.java) { bannerSizeKind(format) }
        }
    }

    @Test
    fun sdkSizes_anchoredAndLarge_measured() {
        assertEquals(56, anchoredSize(360).height)
        assertEquals(64, anchoredSize(411).height)
        assertEquals(113, AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, 360).height)
        assertEquals(128, AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, 411).height)
    }

    @Test
    fun bannerAdSize_fixed_matchesAnchored() {
        WIDTHS_DP.forEach { width ->
            val expected = anchoredSize(width)
            val actual = bannerAdSize(activity, AdFormat.BANNER, width)
            assertEquals("width at $width dp", expected.width, actual.width)
            assertEquals("height at $width dp", expected.height, actual.height)
        }
        assertEquals(64, bannerAdSize(activity, AdFormat.BANNER, 411).height)
    }

    @Test
    fun bannerAdSize_collapsible_matchesLarge() {
        WIDTHS_DP.forEach { width ->
            val expected = AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, width)
            val actual = bannerAdSize(activity, AdFormat.BANNER_COLLAPSIBLE, width)
            assertEquals("width at $width dp", expected.width, actual.width)
            assertEquals("height at $width dp", expected.height, actual.height)
        }
    }

    @Test
    fun bottomBannerSlot_reportsAnchoredHeight() {
        val reported = mutableListOf<Dp>()
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides testAdsController()) {
                Box(Modifier.width(411.dp)) {
                    BottomBannerSlot(
                        AdPlacement.SAMPLE_NATIVE_SMALL,
                        onAdAvailabilityChange = {},
                        onHeight = { reported += it },
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val anchored = anchoredSize(411).height.dp
        val large = AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, 411).height.dp
        assertNotEquals(large, anchored)
        assertEquals(anchored, reported.last())
    }

    /** The SDK marks the anchored adaptive size deprecated with no replacement for it (plan T-067, section 2). */
    @Suppress("DEPRECATION")
    private fun anchoredSize(widthDp: Int): AdSize =
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp)

    private companion object {
        /** The formats drawn at a banner size; T-062 adds AdFormat.BANNER_MREC here. */
        val BANNER_FORMATS = setOf(AdFormat.BANNER, AdFormat.BANNER_COLLAPSIBLE)

        val WIDTHS_DP = listOf(320, 360, 411, 600)
    }
}
