package com.vglstudio.template.adgallery

import androidx.lifecycle.ViewModel
import com.vglstudio.template.core.ads.AdInspectorResult
import com.vglstudio.template.core.ads.AdsFlags
import com.vglstudio.template.core.ads.AdsGlobalFlags
import com.vglstudio.template.core.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pages of the staging ad gallery. The page survives rotation here. Logs page changes and the outcome of each
 * button (ADR-0019), never ad content; no analytics event, flag or stored data.
 */
@HiltViewModel
class AdGalleryViewModel @Inject constructor(
    private val logger: AppLogger,
    private val adsFlags: AdsFlags,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AdGalleryUiState>(AdGalleryUiState.Default)
    val uiState: StateFlow<AdGalleryUiState> = _uiState.asStateFlow()

    // SKELETON (plan T-036, step 3): bodies come in step 4.
    val switches: StateFlow<AdsGlobalFlags?> = MutableStateFlow(null)

    fun openBanners() = Unit

    fun openGrid() = Unit

    fun openPager() = Unit

    fun openFlowSteps() = Unit

    fun openRewarded() = Unit

    fun openStatus() = Unit

    fun selectRewardedPolicy(kind: GalleryRewarded) = Unit

    fun onRewardedRequested() = Unit

    fun onRewardGranted() = Unit

    fun openInList() {
        _uiState.value = AdGalleryUiState.InList(LIST_ITEM_COUNT)
        logger.info(TAG, "page=in_list")
    }

    fun openCollapsible() {
        _uiState.value = AdGalleryUiState.Collapsible(LIST_ITEM_COUNT)
        logger.info(TAG, "page=collapsible")
    }

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

    fun onAdInspectorResult(result: AdInspectorResult) {
        val outcome = when (result) {
            AdInspectorResult.Closed -> "closed"
            is AdInspectorResult.Failed -> "failed code=${result.code}"
            AdInspectorResult.NotReady -> "not_ready"
        }
        logger.info(TAG, "ad_inspector result=$outcome")
    }

    private companion object {
        const val TAG = "AdGallery"
        const val LIST_ITEM_COUNT = 30
    }
}
