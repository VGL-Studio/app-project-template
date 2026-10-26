package com.vglstudio.template.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.designsystem.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The layout of the page of a flow step at every height and font scale it takes an ad (plan T-041, KB-20, O-08): the
 * required parts of the ad inside the page, media never below 120 dp, media shrinking before the body, the compact
 * row below the column height, the column with no height bound, and the head of the ad growing with the text.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xxhdpi")
class NativeFlowStepLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var height by mutableStateOf(484.dp)
    private var assets by mutableStateOf(FakeNativeAdAssets.Design)
    private var fontScale by mutableFloatStateOf(1f)
    private var throughGate by mutableStateOf(false)

    private var metrics: FlowStepMetrics? = null
    private var badgeLine = 0.dp
    private var titleLine = 0.dp
    private val takes = mutableListOf<LoadedNativeAd?>()

    /** The page in a box of [height], or the gate of the page with an ad to take when [throughGate]. */
    private fun setPage() {
        composeRule.setContent {
            WithFontScale(fontScale) {
                AppTheme {
                    val density = LocalDensity.current
                    val typography = MaterialTheme.typography
                    metrics = flowStepMetrics(density, typography)
                    with(density) {
                        badgeLine = typography.labelSmall.lineHeight.toDp()
                        titleLine = typography.titleMedium.lineHeight.toDp()
                    }
                    Box(Modifier.fillMaxWidth().height(height).testTag(PAGE)) {
                        if (throughGate) {
                            key(height, fontScale) {
                                val states = remember { gateStates() }
                                NativeFullScreenStepGate(allowed = true, states = states)
                            }
                        } else {
                            NativeFullScreenStepContent(
                                assets = assets,
                                media = { FakeMedia() },
                                adChoices = { FakeAdChoices() },
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun gateStates(): Flow<NativeAdState> = fullScreenStepStates(
        take = { FakeLoadedNativeAd(FakeNativeAdAssets.Longest).also { takes += it } },
        ensureLoading = {},
        release = {},
    )

    private fun show(h: Dp, ad: NativeAdAssets = assets, scale: Float = fontScale) {
        height = h
        assets = ad
        fontScale = scale
        composeRule.waitForIdle()
    }

    private fun bounds(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun textBounds(text: String): DpRect =
        composeRule.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun exists(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun assertInside(name: String, part: DpRect, page: DpRect) {
        assertTrue("$name $part starts above the page $page", part.top.value >= page.top.value - TOLERANCE)
        assertTrue("$name $part ends below the page $page", part.bottom.value <= page.bottom.value + TOLERANCE)
        assertTrue("$name $part starts left of the page $page", part.left.value >= page.left.value - TOLERANCE)
        assertTrue("$name $part ends right of the page $page", part.right.value <= page.right.value + TOLERANCE)
    }

    private fun overlaps(a: DpRect, b: DpRect) =
        a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top

    private fun assertDp(message: String, expected: Float, actual: Dp) {
        assertEquals(message, expected, actual.value, TOLERANCE)
    }

    @Test
    fun requiredParts_insidePage_atEveryHeight() {
        setPage()
        listOf(FakeNativeAdAssets.Design, FakeNativeAdAssets.Longest).forEach { ad ->
            HEIGHTS.forEach { h ->
                show(h, ad)
                val name = "${ad.callToAction} at $h"
                val page = bounds(PAGE)
                val cta = bounds(NativeFullScreenAdTestTags.CTA)
                val headline = textBounds(ad.headline)
                val badge = bounds(NativeAdTestTags.BADGE)
                val adChoices = bounds(FAKE_AD_CHOICES_TAG)
                assertDp("CTA height, $name: $cta", 52f, cta.height)
                assertInside("CTA, $name", cta, page)
                assertInside("headline, $name", headline, page)
                assertInside("Ad label, $name", badge, page)
                assertInside("AdChoices, $name", adChoices, page)
                assertTrue("headline of one line, $name: $headline", headline.height.value > 1f)
                assertFalse("AdChoices $adChoices covers CTA $cta, $name", overlaps(adChoices, cta))
                assertFalse("AdChoices $adChoices covers headline $headline, $name", overlaps(adChoices, headline))
            }
        }
    }

    @Test
    fun media_neverBelow120dp() {
        setPage()
        listOf(FakeNativeAdAssets.Design, FakeNativeAdAssets.Longest).forEach { ad ->
            HEIGHTS.forEach { h ->
                show(h, ad)
                val media = bounds(NativeFullScreenAdTestTags.MEDIA)
                val name = "${ad.callToAction} at $h: $media"
                assertTrue("media height, $name", media.height.value >= 120f - TOLERANCE)
                assertTrue("media width, $name", media.width.value >= 120f - TOLERANCE)
            }
        }
    }

    @Test
    fun shrinkOrder_mediaFirst_thenBody() {
        setPage()
        val longest = FakeNativeAdAssets.Longest

        show(484.dp, longest)
        assertDp("body of 3 lines at 484 dp", 60f, bounds(NativeFullScreenAdTestTags.BODY).height)
        assertTrue("media above its floor at 484 dp", bounds(NativeFullScreenAdTestTags.MEDIA).height.value > 120.5f)

        show(436.dp, longest)
        assertDp("body of 3 lines at 436 dp", 60f, bounds(NativeFullScreenAdTestTags.BODY).height)
        assertDp("media at its floor at 436 dp", 120f, bounds(NativeFullScreenAdTestTags.MEDIA).height)

        show(400.dp, longest)
        val body = bounds(NativeFullScreenAdTestTags.BODY)
        assertTrue("body of 1 line at most at 400 dp: $body", body.height.value <= 20.5f)
        val media = bounds(NativeFullScreenAdTestTags.MEDIA)
        assertTrue("media at 400 dp: $media", media.height.value >= 120f - TOLERANCE)

        show(360.dp, longest)
        assertFalse("no body at 360 dp", exists(NativeFullScreenAdTestTags.BODY))
        assertDp("media at its floor at 360 dp", 120f, bounds(NativeFullScreenAdTestTags.MEDIA).height)
    }

    @Test
    fun compactRow_belowColumnMin() {
        setPage()
        show(255.dp, FakeNativeAdAssets.Longest)
        val page = bounds(PAGE)
        val media = bounds(NativeFullScreenAdTestTags.MEDIA)
        val cta = bounds(NativeFullScreenAdTestTags.CTA)
        assertTrue("media $media left of CTA $cta", media.right.value <= cta.left.value + TOLERANCE)
        assertDp("CTA at the bottom: $cta in $page", 16f, page.bottom - cta.bottom)

        // Control: the column at 360 dp, media above the call to action, where it starts after an interstitial.
        show(360.dp)
        val columnPage = bounds(PAGE)
        val columnMedia = bounds(NativeFullScreenAdTestTags.MEDIA)
        val columnCta = bounds(NativeFullScreenAdTestTags.CTA)
        assertTrue(
            "media $columnMedia above CTA $columnCta",
            columnMedia.bottom.value <= columnCta.top.value + TOLERANCE,
        )
        assertDp("media starts at 76 dp", 76f, columnMedia.top - columnPage.top)
    }

    @Test
    fun unboundedHeight_media120() {
        composeRule.setContent {
            AppTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    NativeFullScreenStepContent(
                        assets = FakeNativeAdAssets.Design,
                        modifier = Modifier.testTag(PAGE),
                        media = { FakeMedia() },
                        adChoices = { FakeAdChoices() },
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val media = bounds(NativeFullScreenAdTestTags.MEDIA)
        assertTrue("media of at least 120 dp: $media", media.height.value >= 120f - TOLERANCE)
        assertDp("media of 200 dp: $media", 200f, media.height)
        assertDp("CTA height", 52f, bounds(NativeFullScreenAdTestTags.CTA).height)
        assertTrue("the body is drawn", exists(NativeFullScreenAdTestTags.BODY))
    }

    @Test
    fun fontScale_requiredPartsKeepSize() {
        setPage()
        show(360.dp, FakeNativeAdAssets.Longest, 1f)
        val atOne = metrics!!
        assertDp("least height at 1.0", 200f, atOne.minHeight)
        assertDp("least column height at 1.0", 360f, atOne.columnMinHeight)
        assertDp("head of the ad at 1.0", 48f, atOne.headerHeight)

        listOf(1f, 1.3f, 2f).forEach { scale ->
            listOf(360.dp, 200.dp).forEach { h ->
                throughGate = false
                show(h, FakeNativeAdAssets.Longest, scale)
                val least = metrics!!.minHeight
                val name = "font scale $scale at $h (least $least)"
                if (h >= least) {
                    val page = bounds(PAGE)
                    val badge = bounds(NativeAdTestTags.BADGE)
                    assertDp("Ad label of one line of $badgeLine, $name: $badge", badgeLine.value, badge.height)
                    assertInside("Ad label, $name", badge, page)
                    val headline = textBounds(FakeNativeAdAssets.Longest.headline)
                    assertDp("headline of one line of $titleLine, $name: $headline", titleLine.value, headline.height)
                    assertInside("headline, $name", headline, page)
                    val media = bounds(NativeFullScreenAdTestTags.MEDIA)
                    assertTrue("media of 120 dp, $name: $media", media.height.value >= 120f - TOLERANCE)
                    val cta = bounds(NativeFullScreenAdTestTags.CTA)
                    assertDp("CTA height, $name", 52f, cta.height)
                    assertInside("CTA, $name", cta, page)
                } else {
                    takes.clear()
                    throughGate = true
                    composeRule.waitForIdle()
                    assertTrue("no ad taken, $name: $takes", takes.isEmpty())
                    assertFalse("no CTA, $name", exists(NativeFullScreenAdTestTags.CTA))
                }
            }
        }
        // Control (L-055): the page through the gate at 360 dp and font scale 1.0 takes its ad.
        takes.clear()
        throughGate = true
        show(360.dp, FakeNativeAdAssets.Longest, 1f)
        assertEquals(1, takes.size)
    }

    private companion object {
        const val PAGE = "page"
        const val TOLERANCE = 0.5f
        val HEIGHTS = listOf(484.dp, 435.dp, 400.dp, 360.dp, 359.dp, 255.dp, 204.dp, 200.dp)
    }
}
