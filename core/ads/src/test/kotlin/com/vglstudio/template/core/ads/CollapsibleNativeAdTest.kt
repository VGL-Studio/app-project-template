package com.vglstudio.template.core.ads

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAdView
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.core.designsystem.theme.Spacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A fake collapsible ad: headline, advertiser, "Install", a fake icon and media (plan T-012, section 4). */
private fun collapsibleTestAd(headline: String = "Headline of the ad") = NativeAdAssets(
    headline = headline,
    advertiser = "Advertiser",
    body = null,
    callToAction = "Install",
    icon = ImageBitmap(40, 40),
    hasMedia = true,
)

/**
 * The collapsible native ad (KB-4): opens expanded, collapses on a tap of its 48 dp button in the top start corner,
 * and never expands again, whatever the slot loads next. Sizes from design/context/, 1 dp tolerance.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CollapsibleNativeAdTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var content by mutableStateOf<CollapsibleAdContent>(CollapsibleAdContent.Ready(collapsibleTestAd()))
    private var collapsedReports = 0

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val badge: String get() = context.getString(R.string.ads_native_badge)
    private val collapseLabel: String get() = context.getString(R.string.ads_collapse)

    @Composable
    private fun Host() {
        AppTheme(darkTheme = false) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md).testTag(HOST)) {
                CollapsibleNativeAdContent(
                    content = content,
                    state = rememberCollapsibleAdState(),
                    onCollapsed = { collapsedReports++ },
                )
            }
        }
    }

    private fun setHost() {
        composeRule.setContent { Host() }
        composeRule.waitForIdle()
    }

    private fun node(tag: String) = composeRule.onNodeWithTag(tag, useUnmergedTree = true)

    private fun bounds(tag: String) = node(tag).getUnclippedBoundsInRoot()

    private fun assertAbsent(tag: String) =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).assertCountEquals(0)

    private fun assertHeight(tag: String, expected: Float) {
        val b = bounds(tag)
        assertEquals("height of $tag", expected, (b.bottom - b.top).value, TOLERANCE)
    }

    private fun collapse() {
        node(COLLAPSE).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun firstShown_isExpanded() {
        setHost()
        node(MEDIA).assertExists()
        node(COLLAPSE).assertExists()
        assertHeight(AD, 342f)
        composeRule.onNodeWithText(badge, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithContentDescription("Collapse ad", useUnmergedTree = true).assertExists()
    }

    @Test
    fun collapseButton_isAtLeast48dp() {
        setHost()
        val button = bounds(COLLAPSE)
        val width = (button.right - button.left).value
        val height = (button.bottom - button.top).value
        assertTrue("button is $width dp wide", width >= 48f - TOLERANCE)
        assertTrue("button is $height dp high", height >= 48f - TOLERANCE)
    }

    @Test
    fun collapseButton_isInTopStartCorner() {
        setHost()
        val ad = bounds(AD)
        val button = bounds(COLLAPSE)
        assertTrue("button starts ${button.left - ad.left} from the start", button.left - ad.left <= 16.dp)
        assertTrue("button ends after the centre", button.right < ad.left + (ad.right - ad.left) / 2)
        assertTrue("button starts ${button.top - ad.top} from the top", button.top - ad.top <= 12.dp)

        // Nothing clickable in the 24 x 24 dp top end corner, where the SDK draws AdChoices.
        val corner = with(composeRule.density) {
            DpRect(ad.right - 24.dp, ad.top, ad.right, ad.top + 24.dp).toRect()
        }
        val clickable = composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("the collapse button is clickable", clickable.isNotEmpty())
        clickable.forEach { clickableNode ->
            assertFalse(
                "a clickable node ${clickableNode.boundsInRoot} overlaps the AdChoices corner $corner",
                clickableNode.boundsInRoot.overlaps(corner),
            )
        }
    }

    @Test
    fun expandedMedia_isAtLeast120dp() {
        setHost()
        val media = bounds(MEDIA)
        assertTrue("media is ${media.right - media.left} wide", (media.right - media.left).value >= 120f)
        assertHeight(MEDIA, 160f)
    }

    @Test
    fun tapCollapse_showsSmallLayout() {
        setHost()
        collapse()
        assertAbsent(MEDIA)
        assertAbsent(COLLAPSE)
        assertHeight(AD, 80f)
        composeRule.onNodeWithText("Headline of the ad", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(badge, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Install", useUnmergedTree = true).assertExists()
    }

    @Test
    fun tapCollapse_reportsOnce() {
        setHost()
        collapse()
        assertEquals(1, collapsedReports)
    }

    @Test
    fun collapsed_hasNoExpandControl() {
        setHost()
        collapse()
        composeRule.onAllNodesWithContentDescription(collapseLabel, useUnmergedTree = true).assertCountEquals(0)
        composeRule.onNodeWithText("Headline of the ad", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        assertHeight(AD, 80f)
        assertAbsent(MEDIA)
    }

    @Test
    fun collapsed_newAdLoaded_staysCollapsed() {
        setHost()
        collapse()
        content = CollapsibleAdContent.Ready(collapsibleTestAd("Second ad"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Second ad", useUnmergedTree = true).assertExists()
        assertHeight(AD, 80f)
        assertAbsent(MEDIA)
    }

    @Test
    fun collapsed_reloading_keepsSmallPlaceholderThenStaysCollapsed() {
        setHost()
        collapse()
        content = CollapsibleAdContent.Loading
        composeRule.waitForIdle()
        assertHeight(AD, 80f)

        content = CollapsibleAdContent.Ready(collapsibleTestAd("Second ad"))
        composeRule.waitForIdle()
        assertHeight(AD, 80f)
        assertAbsent(MEDIA)
    }

    @Test
    fun collapsed_survivesStateRestoration() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { Host() }
        composeRule.waitForIdle()
        collapse()

        restoration.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertHeight(AD, 80f)
        assertAbsent(MEDIA)
    }

    @Test
    fun collapse_unregistersMediaAndBindsAgain() {
        var binds = 0
        var adView: NativeAdView? = null
        lateinit var state: CollapsibleAdState
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                state = rememberCollapsibleAdState()
                NativeAdViewHost(
                    bind = {
                        binds++
                        adView = it
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
                ) { binder ->
                    CollapsibleNativeAdLayout(
                        collapsibleTestAd("Headline of the ad"),
                        state.collapsed,
                        { state.collapse() },
                        binder,
                        showAdChoicesMarker = false,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val view = checkNotNull(adView) { "bind never ran" }
        assertTrue(view.mediaView is MediaView)
        assertNotNull(view.headlineView)
        assertNotNull(view.callToActionView)
        assertNotNull(view.iconView)
        val bindsBefore = binds

        collapse()

        assertNull(view.mediaView)
        assertNotNull(view.headlineView)
        assertNotNull(view.callToActionView)
        assertTrue("bind ran $binds times, $bindsBefore before collapsing", binds > bindsBefore)
    }

    @Test
    fun collapse_keepsIconAndAdvertiserRegistered() {
        var adView: NativeAdView? = null
        lateinit var state: CollapsibleAdState
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                state = rememberCollapsibleAdState()
                NativeAdViewHost(
                    bind = { adView = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
                ) { binder ->
                    CollapsibleNativeAdLayout(
                        collapsibleTestAd("Headline of the ad"),
                        state.collapsed,
                        { state.collapse() },
                        binder,
                        showAdChoicesMarker = false,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val view = checkNotNull(adView) { "bind never ran" }
        // Control: both are registered while expanded.
        assertNotNull("icon registered before collapsing", view.iconView)
        assertNotNull("advertiser registered before collapsing", view.advertiserView)

        collapse()

        val icon = checkNotNull(view.iconView) { "icon not registered after collapsing" }
        val advertiser = checkNotNull(view.advertiserView) { "advertiser not registered after collapsing" }
        // The registered views are the ones on screen, not views released with the expanded layout.
        assertTrue("registered icon is attached", icon.isAttachedToWindow)
        assertTrue("registered advertiser is attached", advertiser.isAttachedToWindow)
    }

    @Test
    fun stateHolder_onlyCollapses() {
        val state = CollapsibleAdState(collapsed = false)
        assertFalse(state.collapsed)
        state.collapse()
        state.collapse()
        assertTrue(state.collapsed)
    }

    @Test
    fun loadingExpanded_reservesExpandedHeight() {
        content = CollapsibleAdContent.Loading
        setHost()
        assertHeight(AD, 342f)
    }

    @Test
    fun hidden_takesNoSpace() {
        // Control: the same host has the ad's height while an ad is shown.
        setHost()
        assertHeight(HOST, 342f)

        content = CollapsibleAdContent.Hidden
        composeRule.waitForIdle()
        assertAbsent(AD)
        node(HOST).assertHeightIsEqualTo(0.dp)
    }

    @Test
    fun collapsibleFormat_loadsAsMedium() {
        assertEquals(NativeAdSize.MEDIUM, AdFormat.NATIVE_COLLAPSIBLE.nativeLoadSize())
    }

    @Test
    fun slot_rejectsNonCollapsiblePlacement() {
        val error = runCatching {
            composeRule.setContent { CollapsibleNativeAdSlot(AdPlacement.SAMPLE_NATIVE_SMALL) }
            composeRule.waitForIdle()
        }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun expanded_smallScreen_fitsOnScreen() {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    CollapsibleNativeAdCard(
                        assets = collapsibleTestAd(),
                        collapsed = false,
                        onCollapse = {},
                        modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val screen = Rect(0f, 0f, 360f, 640f)
        listOf(COLLAPSE, NativeAdTestTags.CTA).forEach { tag ->
            val b = bounds(tag)
            val inside = b.left.value >= screen.left - TOLERANCE &&
                b.right.value <= screen.right + TOLERANCE &&
                b.top.value >= screen.top - TOLERANCE &&
                b.bottom.value <= screen.bottom + TOLERANCE
            assertTrue("$tag at $b is not inside the 360 x 640 dp screen", inside)
        }
        assertHeight(MEDIA, 160f)
    }

    private companion object {
        const val AD = "collapsible_ad"
        const val MEDIA = "collapsible_ad_media"
        const val COLLAPSE = "collapsible_ad_collapse"
        const val HOST = "host"
        const val TOLERANCE = 1f
    }
}
