package com.vglstudio.template.adgallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vglstudio.template.core.ads.AdInspectorResult
import com.vglstudio.template.core.ads.AdsFlags
import com.vglstudio.template.core.ads.AdsGlobalFlags
import com.vglstudio.template.core.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Pages of the staging ad gallery. The page, the rewarded policy picked and the count of rewarded actions survive
 * rotation here. Logs page changes and the outcome of each button (ADR-0019), never ad content; no analytics event,
 * flag or stored data. [switches] follows the global ad switches for the "Ad status" page (ADR-0003).
 */
@HiltViewModel
class AdGalleryViewModel @Inject constructor(
    private val logger: AppLogger,
    adsFlags: AdsFlags,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AdGalleryUiState>(AdGalleryUiState.Default)
    val uiState: StateFlow<AdGalleryUiState> = _uiState.asStateFlow()

    /** The global ad switches, null until their first value. */
    val switches: StateFlow<AdsGlobalFlags?> =
        adsFlags.global.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private var rewardedPolicy = GalleryRewarded.BLOCK
    private var rewardedRuns = 0

    fun openInList() = open(AdGalleryUiState.InList(LIST_ITEM_COUNT), "in_list")

    fun openCollapsible() = open(AdGalleryUiState.Collapsible(LIST_ITEM_COUNT), "collapsible")

    fun openBanners() = open(AdGalleryUiState.Banners(LIST_ITEM_COUNT), "banners")

    fun openGrid() = open(AdGalleryUiState.Grid(LIST_ITEM_COUNT), "grid")

    fun openPager() = open(AdGalleryUiState.Pager(PAGER_PAGE_COUNT), "pager")

    fun openFlowSteps() = open(AdGalleryUiState.FlowSteps(FLOW_STEP_COUNT), "flow_steps")

    fun openRewarded() = open(rewardedState(), "rewarded")

    fun openStatus() = open(AdGalleryUiState.Status, "status")

    /** Back to the default page; false when already there, so the activity closes. */
    fun back(): Boolean {
        if (_uiState.value == AdGalleryUiState.Default) return false
        _uiState.value = AdGalleryUiState.Default
        logger.info(TAG, "page=default")
        return true
    }

    fun onFullScreenRequested(kind: GalleryFullScreen) {
        logger.info(TAG, "full_screen_requested kind=${kind.name.lowercase()}")
    }

    /** The policy of `gallery_rw_action` when no ad can be shown, for the next taps; the count stays. */
    fun selectRewardedPolicy(kind: GalleryRewarded) {
        rewardedPolicy = kind
        if (_uiState.value is AdGalleryUiState.Rewarded) _uiState.value = rewardedState()
        logger.info(TAG, "rewarded_policy kind=${kind.code}")
    }

    /** The tester tapped "Run action". */
    fun onRewardedRequested() {
        logger.info(TAG, "rewarded_requested kind=${rewardedPolicy.code}")
    }

    /** The action held behind the rewarded ad ran. */
    fun onRewardGranted() {
        rewardedRuns++
        if (_uiState.value is AdGalleryUiState.Rewarded) _uiState.value = rewardedState()
        logger.info(TAG, "rewarded_granted kind=${rewardedPolicy.code} count=$rewardedRuns")
    }

    fun onAdInspectorResult(result: AdInspectorResult) {
        val outcome = when (result) {
            AdInspectorResult.Closed -> "closed"
            is AdInspectorResult.Failed -> "failed code=${result.code}"
            AdInspectorResult.NotReady -> "not_ready"
        }
        logger.info(TAG, "ad_inspector result=$outcome")
    }

    private fun open(state: AdGalleryUiState, page: String) {
        _uiState.value = state
        logger.info(TAG, "page=$page")
    }

    private fun rewardedState() = AdGalleryUiState.Rewarded(rewardedPolicy, rewardedRuns)

    private val GalleryRewarded.code: String get() = name.lowercase()

    private companion object {
        const val TAG = "AdGallery"
        const val LIST_ITEM_COUNT = 30
        const val PAGER_PAGE_COUNT = 6
        const val FLOW_STEP_COUNT = 3
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
