package com.vglstudio.template.adgallery

import com.vglstudio.template.core.ads.AdInspectorResult
import com.vglstudio.template.core.ads.AdIntervalGroup
import com.vglstudio.template.core.ads.AdPlacement
import com.vglstudio.template.core.ads.AdsFlags
import com.vglstudio.template.core.ads.AdsGlobalFlags
import com.vglstudio.template.core.ads.OpenAdType
import com.vglstudio.template.core.logging.LogEntry
import com.vglstudio.template.core.logging.LogLevel
import com.vglstudio.template.core.logging.RecordingAppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pages of the staging ad gallery and what it logs (plan T-014, ADR-0019). */
@OptIn(ExperimentalCoroutinesApi::class)
class AdGalleryViewModelTest {
    private val logger = RecordingAppLogger()
    private val adsFlags = FakeAdsFlags()
    private val mainDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: AdGalleryViewModel

    @Before
    fun setUp() {
        // :app has no MainDispatcherRule; the ViewModel may use viewModelScope.
        Dispatchers.setMain(mainDispatcher)
        viewModel = AdGalleryViewModel(logger, adsFlags)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun messages(): List<String> = logger.entries.map {
        assertEquals("AdGallery", it.tag)
        it.message
    }

    @Test
    fun startsOnDefault() {
        assertEquals(AdGalleryUiState.Default, viewModel.uiState.value)
    }

    @Test
    fun openInList_showsInListWith30Items() {
        viewModel.openInList()
        assertEquals(AdGalleryUiState.InList(30), viewModel.uiState.value)
    }

    @Test
    fun openCollapsible_showsCollapsibleWith30Items() {
        viewModel.openCollapsible()
        assertEquals(AdGalleryUiState.Collapsible(30), viewModel.uiState.value)
    }

    @Test
    fun backFromSubPage_returnsToDefault_andReturnsTrue() {
        viewModel.openInList()
        assertTrue(viewModel.back())
        assertEquals(AdGalleryUiState.Default, viewModel.uiState.value)

        viewModel.openCollapsible()
        assertTrue(viewModel.back())
        assertEquals(AdGalleryUiState.Default, viewModel.uiState.value)
    }

    @Test
    fun backOnDefault_returnsFalse() {
        assertFalse(viewModel.back())
        assertEquals(AdGalleryUiState.Default, viewModel.uiState.value)
    }

    @Test
    fun logsPageChanges() {
        viewModel.openInList()
        viewModel.back()
        viewModel.openCollapsible()
        viewModel.back()
        // Back on the default page changes nothing, so it logs nothing.
        viewModel.back()

        assertEquals(listOf("page=in_list", "page=default", "page=collapsible", "page=default"), messages())
        assertTrue(logger.entries.all { it.level == LogLevel.INFO })
    }

    @Test
    fun logsFullScreenRequests() {
        viewModel.onFullScreenRequested(GalleryFullScreen.INTERSTITIAL)
        viewModel.onFullScreenRequested(GalleryFullScreen.NATIVE_FULL_SCREEN)
        viewModel.onFullScreenRequested(GalleryFullScreen.APP_OPEN)

        assertEquals(
            listOf(
                "full_screen_requested kind=interstitial",
                "full_screen_requested kind=native_full_screen",
                "full_screen_requested kind=app_open",
            ),
            messages(),
        )
    }

    @Test
    fun openNewPages_showTheirState_andLog() {
        val pages = listOf(
            Triple(viewModel::openBanners, AdGalleryUiState.Banners(30), "page=banners"),
            Triple(viewModel::openGrid, AdGalleryUiState.Grid(30), "page=grid"),
            Triple(viewModel::openPager, AdGalleryUiState.Pager(6), "page=pager"),
            Triple(viewModel::openFlowSteps, AdGalleryUiState.FlowSteps(3), "page=flow_steps"),
            Triple(viewModel::openRewarded, AdGalleryUiState.Rewarded(GalleryRewarded.BLOCK, 0), "page=rewarded"),
            Triple(viewModel::openStatus, AdGalleryUiState.Status, "page=status"),
        )
        pages.forEach { (open, state, log) ->
            val before = logger.entries.size
            open()
            assertEquals(state, viewModel.uiState.value)
            assertEquals(listOf(log), messages().drop(before))
        }
    }

    @Test
    fun backFromNewPages_returnsToDefault() {
        listOf(
            viewModel::openBanners,
            viewModel::openGrid,
            viewModel::openPager,
            viewModel::openFlowSteps,
            viewModel::openRewarded,
            viewModel::openStatus,
        ).forEach { open ->
            open()
            // Control: the page did change.
            assertFalse(viewModel.uiState.value == AdGalleryUiState.Default)
            assertTrue(viewModel.back())
            assertEquals(AdGalleryUiState.Default, viewModel.uiState.value)
        }
    }

    @Test
    fun rewarded_selectPolicy_andCount() {
        viewModel.openRewarded()
        assertEquals(AdGalleryUiState.Rewarded(GalleryRewarded.BLOCK, 0), viewModel.uiState.value)

        var before = logger.entries.size
        viewModel.selectRewardedPolicy(GalleryRewarded.ALLOW_AFTER)
        assertEquals(AdGalleryUiState.Rewarded(GalleryRewarded.ALLOW_AFTER, 0), viewModel.uiState.value)
        assertEquals(listOf("rewarded_policy kind=allow_after"), messages().drop(before))

        before = logger.entries.size
        viewModel.onRewardedRequested()
        viewModel.onRewardGranted()
        viewModel.onRewardGranted()
        assertEquals(AdGalleryUiState.Rewarded(GalleryRewarded.ALLOW_AFTER, 2), viewModel.uiState.value)
        assertEquals(
            listOf(
                "rewarded_requested kind=allow_after",
                "rewarded_granted kind=allow_after count=1",
                "rewarded_granted kind=allow_after count=2",
            ),
            messages().drop(before),
        )

        // Back on the default page and in again: the policy and the count stay.
        viewModel.back()
        viewModel.openRewarded()
        assertEquals(AdGalleryUiState.Rewarded(GalleryRewarded.ALLOW_AFTER, 2), viewModel.uiState.value)

        // A new policy does not reset the count.
        viewModel.selectRewardedPolicy(GalleryRewarded.ALLOW)
        assertEquals(AdGalleryUiState.Rewarded(GalleryRewarded.ALLOW, 2), viewModel.uiState.value)
    }

    @Test
    fun switches_followAdsFlags() = runTest(mainDispatcher) {
        // Nothing read before a subscriber (WhileSubscribed): pending.
        assertNull(viewModel.switches.value)
        backgroundScope.launch { viewModel.switches.collect {} }
        assertEquals(false, viewModel.switches.value?.showNativeAfterInter)

        adsFlags.global.value = adsFlags.global.value.copy(showNativeAfterInter = true)
        assertEquals(true, viewModel.switches.value?.showNativeAfterInter)
        // Control: the value follows changes both ways, it is not read once.
        adsFlags.global.value = adsFlags.global.value.copy(showNativeAfterInter = false)
        assertEquals(false, viewModel.switches.value?.showNativeAfterInter)
    }

    @Test
    fun logsInspectorResults() {
        viewModel.onAdInspectorResult(AdInspectorResult.Closed)
        viewModel.onAdInspectorResult(AdInspectorResult.Failed(3))
        viewModel.onAdInspectorResult(AdInspectorResult.NotReady)

        assertEquals(
            listOf("ad_inspector result=closed", "ad_inspector result=failed code=3", "ad_inspector result=not_ready"),
            messages(),
        )
        assertEquals(LogEntry(LogLevel.INFO, "AdGallery", "ad_inspector result=closed"), logger.entries.first())
    }
}

/** The global ad switches of the gallery's tests, all off; the placement methods are not used by the gallery. */
class FakeAdsFlags : AdsFlags {
    override val global = MutableStateFlow(
        AdsGlobalFlags(
            adsEnabled = true,
            showOpenAds = false,
            showOpenAdsFirstOpen = false,
            openAdType = OpenAdType.APP_OPEN,
            showResumeAds = false,
            resumeAdType = OpenAdType.APP_OPEN,
            adsIntervalSeconds = 0,
            showNativeAfterInter = false,
            showNativeBottom = false,
            showNativeCollap = false,
            expandCollapAfterSeconds = 0,
            consentShortcutEnabled = false,
            logFullScreenEvents = false,
            logSlotEvents = false,
            closeNativeCountdownSeconds = 5,
        ),
    )

    override fun placementEnabled(placement: AdPlacement): Flow<Boolean> = flowOf(false)

    override fun intervalSeconds(group: AdIntervalGroup): Flow<Long> = flowOf(0)

    override fun nativeEvery(placement: AdPlacement): Flow<Long?> = flowOf(null)
}
