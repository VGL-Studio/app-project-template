package com.vglstudio.template.adgallery

import com.vglstudio.template.core.ads.AdInspectorResult
import com.vglstudio.template.core.logging.LogEntry
import com.vglstudio.template.core.logging.LogLevel
import com.vglstudio.template.core.logging.RecordingAppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pages of the staging ad gallery and what it logs (plan T-014, ADR-0019). */
@OptIn(ExperimentalCoroutinesApi::class)
class AdGalleryViewModelTest {
    private val logger = RecordingAppLogger()
    private lateinit var viewModel: AdGalleryViewModel

    @Before
    fun setUp() {
        // :app has no MainDispatcherRule; the ViewModel may use viewModelScope.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = AdGalleryViewModel(logger)
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

        assertEquals(
            listOf("full_screen_requested kind=interstitial", "full_screen_requested kind=native_full_screen"),
            messages(),
        )
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
