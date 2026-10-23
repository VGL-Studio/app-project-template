package com.vglstudio.template.core.network

import app.cash.turbine.test
import com.vglstudio.template.core.analytics.AnalyticsEvent
import com.vglstudio.template.core.logging.LogLevel
import com.vglstudio.template.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When the network dialog shows and closes (spec network, KB-1 to KB-3), on virtual time. Unless a test says
 * otherwise: the network is up, no full-screen ad shows and the app is past loading on the "sample" screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkDialogControllerTest {
    private val connectivity = FakeConnectivitySource(connected = true)
    private val settings = FakeRemoteSettings()
    private val analytics = FakeAnalytics()
    private val logger = RecordingAppLogger()

    private fun TestScope.controller(screen: String? = SAMPLE): NetworkDialogController = NetworkDialogController(
        connectivity = connectivity,
        settings = settings,
        analytics = analytics,
        logger = logger,
        clock = FakeNetworkClock { testScheduler.currentTime },
        scope = backgroundScope,
    ).also {
        it.setScreen(screen)
        runCurrent()
    }

    private fun TestScope.recoveredCount(controller: NetworkDialogController): () -> Int {
        var count = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { controller.recovered.collect { count++ } }
        return { count }
    }

    private fun TestScope.seenStates(controller: NetworkDialogController): List<NetworkDialogState> {
        val seen = mutableListOf<NetworkDialogState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { controller.state.collect { seen += it } }
        return seen
    }

    private fun TestScope.setConnected(connected: Boolean) {
        connectivity.connected.value = connected
        runCurrent()
    }

    private fun TestScope.reportFailure(controller: NetworkDialogController) {
        controller.reportNetworkFailure("assets")
        runCurrent()
    }

    private fun events(name: String): List<AnalyticsEvent> = analytics.events.filter { it.name == name }

    private fun infoMessages(): List<String> =
        logger.entries.filter { it.tag == "Network" && it.level == LogLevel.INFO }.map { it.message }

    // KB-1, criterion 1: nothing happens on the loading screen.

    @Test
    fun onLoading_ignoresOfflineAndFailures() = runTest {
        val controller = controller(screen = null)

        setConnected(false)
        advanceTimeBy(10_000)
        reportFailure(controller)
        assertEquals(HIDDEN, controller.state.value)
        assertEquals(0, connectivity.collectors)
        assertEquals(emptyList<AnalyticsEvent>(), analytics.events)

        // A failure reported on the loading screen is not kept for later.
        setConnected(true)
        controller.setScreen(SAMPLE)
        runCurrent()
        assertEquals(HIDDEN, controller.state.value)

        // Control: past loading, the same controller watches the network and shows on a failure.
        assertTrue("collectors ${connectivity.collectors}", connectivity.collectors > 0)
        reportFailure(controller)
        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun backToLoading_hidesWithoutRestoredEvent() = runTest {
        val controller = controller()
        reportFailure(controller)
        assertEquals(SHOWN, controller.state.value)

        controller.setScreen(null)
        runCurrent()

        assertEquals(HIDDEN, controller.state.value)
        assertEquals(0, connectivity.collectors)
        assertEquals(emptyList<AnalyticsEvent>(), events("network_restored"))
    }

    // KB-1, criterion 2: leaving loading without network shows the dialog at once.

    @Test
    fun leavingLoadingOffline_showsAtOnce() = runTest {
        connectivity.connected.value = false
        val controller = controller(screen = null)
        assertEquals(HIDDEN, controller.state.value)

        controller.setScreen(SAMPLE)
        runCurrent()

        assertEquals(0L, currentTime)
        assertEquals(SHOWN, controller.state.value)
        assertEquals(listOf(lostShown(SAMPLE)), events("network_lost_shown"))
    }

    // KB-2, criterion 1: the dialog shows after the delay of network_lost_delay_seconds.

    @Test
    fun deviceOffline_shownOnlyAfterDelay() = runTest {
        val controller = controller()

        setConnected(false)
        advanceTimeBy(1_999)
        assertEquals(HIDDEN, controller.state.value)

        advanceTimeBy(2)
        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun delay_followsRemoteSetting() = runTest {
        settings.values.value = mapOf("network_lost_delay_seconds" to 5L)
        val controller = controller()

        setConnected(false)
        advanceTimeBy(4_999)
        assertEquals(HIDDEN, controller.state.value)

        advanceTimeBy(2)
        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun flapping_underDelay_showsNothing() = runTest {
        val controller = controller()
        val seen = seenStates(controller)

        repeat(2) {
            setConnected(false)
            advanceTimeBy(1_500)
            setConnected(true)
            advanceTimeBy(1)
        }
        advanceTimeBy(10_000)
        assertTrue("$seen", seen.all { it == HIDDEN })

        // Control: the controller does watch the network: a loss longer than the delay shows the dialog.
        setConnected(false)
        advanceTimeBy(2_001)
        assertEquals(SHOWN, controller.state.value)
    }

    // KB-2, criterion 3: "Retry" while still offline.

    @Test
    fun retry_offline_spinsAtMost2s_thenStillOffline() = runTest {
        connectivity.connected.value = false
        val controller = controller()
        val recovered = recoveredCount(controller)
        assertEquals(SHOWN, controller.state.value)

        controller.retry()
        runCurrent()
        assertEquals(CHECKING, controller.state.value)

        advanceTimeBy(1_999)
        assertEquals(CHECKING, controller.state.value)

        advanceTimeBy(2)
        assertEquals(STILL_OFFLINE, controller.state.value)
        assertEquals(0, recovered())
    }

    @Test
    fun retry_whileChecking_isIgnored() = runTest {
        connectivity.connected.value = false
        val controller = controller()

        controller.retry()
        advanceTimeBy(1_000)
        controller.retry()
        advanceTimeBy(1_001)

        assertEquals(STILL_OFFLINE, controller.state.value)
    }

    @Test
    fun retryAgain_clearsStillOffline() = runTest {
        connectivity.connected.value = false
        val controller = controller()
        controller.retry()
        advanceTimeBy(2_001)
        assertEquals(STILL_OFFLINE, controller.state.value)

        controller.retry()
        runCurrent()

        assertEquals(CHECKING, controller.state.value)
    }

    // KB-2, criterion 4: the dialog closes when the network is back.

    @Test
    fun networkBack_closesAtOnce_andEmitsRecovered() = runTest {
        connectivity.connected.value = false
        val controller = controller()
        val recovered = recoveredCount(controller)
        assertEquals(SHOWN, controller.state.value)
        val shownAt = currentTime

        setConnected(true)

        assertEquals(shownAt, currentTime)
        assertEquals(HIDDEN, controller.state.value)
        assertEquals(1, recovered())
    }

    @Test
    fun retry_online_closes_andEmitsRecovered() = runTest {
        val controller = controller()
        val recovered = recoveredCount(controller)
        reportFailure(controller)
        assertEquals(SHOWN, controller.state.value)

        controller.retry()
        runCurrent()

        assertEquals(HIDDEN, controller.state.value)
        assertEquals(1, recovered())
    }

    @Test
    fun retry_networkReturnsWhileChecking_closes() = runTest {
        connectivity.connected.value = false
        val controller = controller()
        val recovered = recoveredCount(controller)

        controller.retry()
        advanceTimeBy(1_000)
        setConnected(true)
        advanceTimeBy(300)

        assertEquals(HIDDEN, controller.state.value)
        assertEquals(1, recovered())
    }

    // KB-2, special cases.

    @Test
    fun fullScreenAd_holdsDialog_thenShowsWhenClosed() = runTest {
        val controller = controller()
        controller.setFullScreenAdShowing(true)
        runCurrent()

        setConnected(false)
        advanceTimeBy(3_000)
        assertEquals(HIDDEN, controller.state.value)
        assertEquals(emptyList<AnalyticsEvent>(), analytics.events)

        controller.setFullScreenAdShowing(false)
        runCurrent()

        assertEquals(SHOWN, controller.state.value)
        assertEquals(listOf(lostShown(SAMPLE)), events("network_lost_shown"))
    }

    @Test
    fun adStartsWhileShown_hidesWithoutRestoredEvent() = runTest {
        connectivity.connected.value = false
        val controller = controller()
        assertEquals(SHOWN, controller.state.value)

        controller.setFullScreenAdShowing(true)
        runCurrent()

        assertEquals(HIDDEN, controller.state.value)
        assertEquals(emptyList<AnalyticsEvent>(), events("network_restored"))
    }

    @Test
    fun appForeground_checksAtOnceExceptFirst() = runTest {
        val controller = controller()
        val readsBefore = connectivity.isConnectedCalls

        // The first start follows the loading screen, which already checked.
        controller.onAppForeground()
        runCurrent()
        assertEquals(readsBefore, connectivity.isConnectedCalls)

        // Offline for less than the delay: only the check on return can show the dialog now.
        setConnected(false)
        controller.onAppForeground()
        runCurrent()

        assertEquals(SHOWN, controller.state.value)
    }

    // KB-3: a download that fails because of the network.

    @Test
    fun networkFailure_whileDeviceOnline_showsAtOnce_andRetryCloses() = runTest {
        val controller = controller()

        reportFailure(controller)

        assertEquals(SHOWN, controller.state.value)
        val shownLog = infoMessages().single { "load_failed" in it }
        assertTrue(shownLog, "online=true" in shownLog)

        controller.retry()
        runCurrent()

        assertEquals(HIDDEN, controller.state.value)
        assertEquals(1, events("network_restored").size)
    }

    @Test
    fun networkFailure_whileDeviceOffline_showsAtOnce() = runTest {
        val controller = controller()
        setConnected(false)
        advanceTimeBy(500)
        assertEquals(HIDDEN, controller.state.value)

        reportFailure(controller)

        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun manyFailures_singleDialog() = runTest {
        val controller = controller()

        controller.state.test {
            assertEquals(HIDDEN, awaitItem())
            repeat(5) { controller.reportNetworkFailure("assets") }
            connectivity.connected.value = false
            advanceTimeBy(10_000)
            assertEquals(SHOWN, awaitItem())
            expectNoEvents()
        }
        assertEquals(1, events("network_lost_shown").size)
    }

    // Analytics events (spec network, section 6).

    @Test
    fun shown_logsLostEventWithScreen() = runTest {
        val controller = controller()
        reportFailure(controller)
        assertEquals(listOf(lostShown(SAMPLE)), events("network_lost_shown"))

        controller.retry()
        runCurrent()
        reportFailure(controller)

        assertEquals(listOf(lostShown(SAMPLE), lostShown(SAMPLE)), events("network_lost_shown"))
    }

    @Test
    fun restored_logsDurationAndRetries() = runTest {
        advanceTimeBy(5_000)
        connectivity.connected.value = false
        val controller = controller()
        assertEquals(SHOWN, controller.state.value)
        val shownAt = currentTime

        repeat(2) {
            controller.retry()
            advanceTimeBy(2_001)
            assertEquals(STILL_OFFLINE, controller.state.value)
        }
        advanceTimeBy(shownAt + 12_000 - currentTime)
        setConnected(true)

        val expected = AnalyticsEvent(
            "network_restored",
            mapOf("duration_ms" to "12000", "retry_count" to "2"),
        )
        assertEquals(listOf(expected), events("network_restored"))
    }

    @Test
    fun dialogShown_followsState() = runTest {
        val controller = controller()
        assertFalse(controller.dialogShown.value)

        reportFailure(controller)
        assertTrue(controller.dialogShown.value)

        controller.retry()
        runCurrent()
        assertFalse(controller.dialogShown.value)
    }

    @Test
    fun logsShowAndRetryOutcome() = runTest {
        val controller = controller()
        setConnected(false)
        advanceTimeBy(2_001)
        controller.retry()
        advanceTimeBy(2_001)
        connectivity.connected.value = true
        controller.retry()
        runCurrent()
        reportFailure(controller)

        val messages = infoMessages()
        listOf("device_offline", "still_offline", "online", "load_failed").forEach { word ->
            assertTrue("$word in $messages", messages.any { word in it })
        }
        assertTrue("$messages", messages.none { "/" in it })
    }

    private companion object {
        const val SAMPLE = "sample"
        val HIDDEN = NetworkDialogState.Hidden
        val SHOWN = NetworkDialogState.Shown(checking = false, stillOffline = false)
        val CHECKING = NetworkDialogState.Shown(checking = true, stillOffline = false)
        val STILL_OFFLINE = NetworkDialogState.Shown(checking = false, stillOffline = true)

        fun lostShown(screen: String) = AnalyticsEvent("network_lost_shown", mapOf("screen" to screen))
    }
}
