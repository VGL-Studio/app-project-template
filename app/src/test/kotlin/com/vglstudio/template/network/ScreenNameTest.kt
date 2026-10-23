package com.vglstudio.template.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The screen name of network_lost_shown and the screen reported to the network dialog (spec network). */
class ScreenNameTest {
    @Test
    fun sampleDestination_isSample() {
        assertEquals("sample", screenNameOf(SAMPLE_ROUTE))
    }

    @Test
    fun loadingDestination_isLoading() {
        assertEquals("loading", screenNameOf(LOADING_ROUTE))
    }

    @Test
    fun camelCase_becomesSnakeCase() {
        assertEquals("ad_gallery", screenNameOf("com.x.AdGalleryDestination"))
    }

    @Test
    fun routeWithArguments_keepsOnlyTheName() {
        assertEquals("detail", screenNameOf("com.x.DetailDestination/{id}?tab={tab}"))
    }

    @Test
    fun nullOrEmpty_isUnknown() {
        assertEquals("unknown", screenNameOf(null))
        assertEquals("unknown", screenNameOf(""))
        // Control next to it: a real route is not unknown, so the two checks above can fail.
        assertEquals("sample", screenNameOf(SAMPLE_ROUTE))
    }

    @Test
    fun longName_isCutTo100Characters() {
        assertEquals("x".repeat(100), screenNameOf("com.x." + "x".repeat(300)))
    }

    @Test
    fun reportedScreen_nullBeforeFirstEntryAndOnLoading() {
        assertNull(reportedScreenOf(null))
        assertNull(reportedScreenOf(LOADING_ROUTE))
        assertEquals("sample", reportedScreenOf(SAMPLE_ROUTE))
    }

    // AppNavHost reports reportedScreenOf(route) only for the routes where reportsScreen(route) is true.
    private fun reported(routes: List<String?>): List<String?> = routes.filter(::reportsScreen).map(::reportedScreenOf)

    @Test
    fun activityRecreated_firstFrameWithoutEntry_isNotReported() {
        // Rotation, dark mode or locale recreate the activity: the first frame has no back stack entry yet, then
        // the restored destination comes back. The dialog controller must not see a passage through loading.
        assertEquals(listOf("sample"), reported(listOf(null, SAMPLE_ROUTE)))
    }

    @Test
    fun coldStart_reportsLoadingThenSample() {
        // Cold start: no entry yet, then loading, then the start screen. The controller starts on loading already.
        assertEquals(listOf(null, "sample"), reported(listOf(null, LOADING_ROUTE, SAMPLE_ROUTE)))
    }

    @Test
    fun backToLoading_isStillReported() {
        assertEquals(listOf("sample", null), reported(listOf(SAMPLE_ROUTE, LOADING_ROUTE)))
    }

    private companion object {
        const val SAMPLE_ROUTE = "com.vglstudio.template.feature.sample.navigation.SampleDestination"
        const val LOADING_ROUTE = "com.vglstudio.template.feature.loading.navigation.LoadingDestination"
    }
}
