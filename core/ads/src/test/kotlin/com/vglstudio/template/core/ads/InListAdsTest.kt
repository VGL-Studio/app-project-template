package com.vglstudio.template.core.ads

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.core.logging.RecordingAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** In-list native ads in a LazyColumn and a LazyVerticalGrid: order, label, spacing, keys (plan T-011, section 4). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class InListAdsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val badge: String
        get() = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.ads_native_badge)

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(tag).getBoundsInRoot()

    private fun assertDpEquals(message: String, expected: Dp, actual: Dp) {
        assertEquals(message, expected.value, actual.value, TOLERANCE_DP)
    }

    private fun adRows(prefix: String = AD_TAG_PREFIX) = SemanticsMatcher("test tag starts with $prefix") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }

    private fun inListAds(every: Int?, loader: InListAdLoader, scope: CoroutineScope) = InListAds(
        placement = AdPlacement.SAMPLE_NATIVE_SMALL,
        every = mutableStateOf(every),
        loader = loader,
        scope = scope,
        logger = RecordingAppLogger(),
    )

    private val loadedLoader = InListAdLoader { NativeAdState.Loaded(FakeLoadedNativeAd(sampleNativeAdAssets)) }

    /** Seven items through the public function, with the real ad rows of [loader], every 3 items. */
    private fun setPublicListContent(loader: InListAdLoader) {
        composeRule.setContent {
            AppTheme {
                val scope = rememberCoroutineScope()
                val ads = remember { inListAds(every = 3, loader = loader, scope = scope) }
                LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST)) {
                    itemsWithInListAds(items = (1..7).toList(), ads = ads) {
                        Text("List item $it", Modifier.testTag("item_$it"))
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun lazyColumn_sevenItemsEveryThree_adRowsAfterItems3And6() {
        composeRule.setContent {
            AppTheme {
                LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST)) {
                    itemsWithInListAds(
                        items = (1..7).toList(),
                        positions = inListAdPositions(itemCount = 7, every = 3),
                        placementKey = AdPlacement.SAMPLE_NATIVE_SMALL.key,
                        adContent = { slot ->
                            NativeAdCard(sampleNativeAdAssets, NativeAdSize.SMALL, Modifier.testTag("inlist_ad_$slot"))
                        },
                    ) {
                        Text("List item $it", Modifier.testTag("item_$it"))
                    }
                }
            }
        }

        val firstAd = bounds("inlist_ad_0")
        assertTrue(firstAd.top >= bounds("item_3").bottom)
        assertTrue(firstAd.bottom <= bounds("item_4").top)

        composeRule.onNodeWithTag(LIST).performScrollToNode(hasTestTag("inlist_ad_1"))
        val secondAd = bounds("inlist_ad_1")
        assertTrue(secondAd.top >= bounds("item_6").bottom)
        assertTrue(secondAd.bottom <= bounds("item_7").top)

        composeRule.onAllNodes(adRows()).assertCountEquals(2)
    }

    @Test
    fun lazyColumn_fewerItemsThanEvery_noAdRow() {
        var every by mutableStateOf<Int?>(3)
        composeRule.setContent {
            AppTheme {
                val scope = rememberCoroutineScope()
                val ads = remember {
                    InListAds(
                        placement = AdPlacement.SAMPLE_NATIVE_SMALL,
                        every = derivedStateOf { every },
                        loader = loadedLoader,
                        scope = scope,
                        logger = RecordingAppLogger(),
                    )
                }
                LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST)) {
                    itemsWithInListAds(items = (1..7).toList(), ads = ads) {
                        Text("List item $it", Modifier.testTag("item_$it"))
                    }
                }
            }
        }
        // Control: with N = 3 the same seven items get an ad row, so its absence below comes from N = 8.
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)

        every = 8
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(0)
        composeRule.onAllNodesWithText(badge).assertCountEquals(0)
    }

    @Test
    fun adRow_showsAdLabel() {
        setPublicListContent(loadedLoader)
        composeRule.onNode(hasText(badge) and hasAnyAncestor(hasTestTag("inlist_ad_0"))).assertExists()
    }

    @Test
    fun adRow_isSixteenDpFromItemsAboveAndBelow() {
        setPublicListContent(loadedLoader)

        val card = composeRule
            .onNode(hasTestTag(NativeAdTestTags.CARD) and hasAnyAncestor(hasTestTag("inlist_ad_0")))
            .getBoundsInRoot()
        assertDpEquals("gap above the ad", 16.dp, card.top - bounds("item_3").bottom)
        assertDpEquals("gap below the ad", 16.dp, bounds("item_4").top - card.bottom)
    }

    @Test
    fun unavailableSlot_takesNoHeight() {
        setPublicListContent(InListAdLoader { NativeAdState.Unavailable })

        // Control: the ad row is in the list, between item 3 and item 4, so the 0 dp below is the row collapsing.
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        assertDpEquals("ad row height", 0.dp, bounds("inlist_ad_0").height)
        assertDpEquals("gap between item 3 and item 4", 0.dp, bounds("item_4").top - bounds("item_3").bottom)
    }

    @Test
    fun loadingSlot_showsPlaceholderOfSmallHeight() {
        setPublicListContent(InListAdLoader { awaitCancellation() })

        assertDpEquals("ad row height", 72.dp + 16.dp * 2, bounds("inlist_ad_0").height)
        composeRule
            .onNode(hasTestTag(NativeAdTestTags.PLACEHOLDER) and hasAnyAncestor(hasTestTag("inlist_ad_0")))
            .assertExists()
    }

    @Test
    fun lazyGrid_twoColumnsEveryThree_adSpansFullWidthAfterItem4() {
        composeRule.setContent {
            AppTheme {
                val scope = rememberCoroutineScope()
                val ads = remember { inListAds(every = 3, loader = loadedLoader, scope = scope) }
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().testTag(GRID)) {
                    itemsWithInListAds(items = (1..7).toList(), ads = ads, columns = 2) {
                        Text("List item $it", Modifier.testTag("item_$it"))
                    }
                }
            }
        }
        composeRule.waitForIdle()

        val ad = bounds("inlist_ad_0")
        assertTrue(ad.top >= bounds("item_3").bottom)
        assertTrue(ad.top >= bounds("item_4").bottom)
        assertTrue(ad.bottom <= bounds("item_5").top)
        assertDpEquals("ad row width", bounds(GRID).width, ad.width)
    }

    @Test
    fun publicApi_withoutAdsController_addsNoAdRow() {
        var adsWithoutController: InListAds? = null
        composeRule.setContent {
            AppTheme {
                val ads = rememberInListAds(AdPlacement.SAMPLE_NATIVE_SMALL)
                adsWithoutController = ads
                val scope = rememberCoroutineScope()
                val allowedAds = remember { inListAds(every = 3, loader = loadedLoader, scope = scope) }
                Row(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(modifier = Modifier.weight(1f).testTag(LIST)) {
                        itemsWithInListAds(items = (1..20).toList(), ads = ads) {
                            Text("List item $it", Modifier.testTag("item_$it"))
                        }
                    }
                    // Control: the same items with ads allowed get ad rows.
                    LazyColumn(modifier = Modifier.weight(1f).testTag(CONTROL_LIST)) {
                        itemsWithInListAds(items = (1..20).toList(), ads = allowedAds) {
                            Text("List item $it", Modifier.testTag("control_item_$it"))
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasTestTag("inlist_ad_0") and hasAnyAncestor(hasTestTag(CONTROL_LIST))).assertExists()

        assertNull(adsWithoutController!!.every)
        composeRule.onAllNodes(adRows("item_") and hasAnyAncestor(hasTestTag(LIST))).assertCountEquals(20)
        composeRule.onAllNodes(adRows() and hasAnyAncestor(hasTestTag(LIST))).assertCountEquals(0)
        composeRule.onAllNodes(hasText(badge) and hasAnyAncestor(hasTestTag(LIST))).assertCountEquals(0)
    }

    @Test
    fun loadedSlots_destroyedWhenEveryBecomesNull_andLoadAgainWhenBack() {
        var every by mutableStateOf<Int?>(3)
        val loaded = mutableListOf<FakeLoadedNativeAd>()
        val loader = InListAdLoader {
            NativeAdState.Loaded(FakeLoadedNativeAd(sampleNativeAdAssets).also { loaded += it })
        }
        composeRule.setContent {
            AppTheme {
                val scope = rememberCoroutineScope()
                val ads = remember {
                    InListAds(
                        placement = AdPlacement.SAMPLE_NATIVE_SMALL,
                        every = derivedStateOf { every },
                        loader = loader,
                        scope = scope,
                        logger = RecordingAppLogger(),
                    )
                }
                LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST)) {
                    itemsWithInListAds(items = (1..7).toList(), ads = ads) {
                        Text("List item $it", Modifier.testTag("item_$it"))
                    }
                }
            }
        }
        composeRule.waitForIdle()
        // Control: two ads are loaded and none is destroyed while their rows are shown.
        assertEquals(listOf(0, 0), loaded.map { it.destroyCount })

        // Consent withdrawn or placement switched off while the screen is open.
        every = null
        composeRule.waitForIdle()
        composeRule.onAllNodes(adRows()).assertCountEquals(0)
        assertEquals(listOf(1, 1), loaded.map { it.destroyCount })

        // Allowed again: the rows come back with fresh ads.
        every = 3
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        assertEquals(4, loaded.size)
        assertEquals(listOf(0, 0), loaded.drop(2).map { it.destroyCount })
    }

    @Test
    fun adRowKeys_doNotClashWithItemKeys() {
        composeRule.setContent {
            AppTheme {
                val scope = rememberCoroutineScope()
                val ads = remember { inListAds(every = 3, loader = loadedLoader, scope = scope) }
                LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST)) {
                    itemsWithInListAds(items = (0..19).toList(), ads = ads, key = { it }) {
                        Text("List item $it", Modifier.testTag("item_$it"))
                    }
                }
            }
        }
        composeRule.waitForIdle()

        // Control: ad rows were added next to items keyed 0, 1, 2..., which would clash with a slot-number key.
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        composeRule.onAllNodesWithTag("inlist_ad_1").assertCountEquals(1)
    }

    private companion object {
        const val LIST = "list"
        const val CONTROL_LIST = "control_list"
        const val GRID = "grid"
        const val AD_TAG_PREFIX = "inlist_ad_"
        const val TOLERANCE_DP = 0.5f
    }
}
