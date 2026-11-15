package com.vglstudio.template.core.ads

import com.vglstudio.template.core.analytics.AdFunnelEvent
import com.vglstudio.template.core.analytics.AdFunnelKind
import com.vglstudio.template.core.analytics.AdMetricEvent
import com.vglstudio.template.core.analytics.RecordingAdFunnel
import com.vglstudio.template.core.analytics.RecordingAdMetrics
import com.vglstudio.template.core.logging.RecordingAppLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one interstitial slot of AppLovin MAX (plan T-069) on a virtual clock: every way to a send (a load asked by the
 * controller, the wait for MAX to be ready, the retry of F-05, the reload of F-06) checks the order and switches
 * ([shouldLoad]) and the consent gate ([mayRequestNow]) right before sending; one request at a time (L-068).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MaxInterstitialSlotTest {
    private val logger = RecordingAppLogger()
    private val metrics = RecordingAdMetrics()
    private val funnel = RecordingAdFunnel()
    private val counter = LoadAttemptCounter()
    private val maxReady = MutableStateFlow(true)
    private var shouldLoad = true
    private var gateOpen = true
    private val built = mutableListOf<FakeMaxInterstitialSdkAd>()
    private val ad: FakeMaxInterstitialSdkAd get() = built.single()

    private val telemetry = AdTelemetry(
        metrics = metrics,
        funnel = funnel,
        lifecycleSwitches = { FLAGS.copy(logFullScreenEvents = true) },
        serving = { true },
        offline = { false },
        counter = counter,
        logger = logger,
    )

    private fun TestScope.slot(
        unit: String? = UNIT,
        shouldLoadGate: suspend () -> Boolean = { shouldLoad },
    ) = MaxInterstitialSlot(
        scope = backgroundScope,
        clock = MonotonicClock { testScheduler.currentTime },
        unitId = { unit },
        maxReady = maxReady,
        shouldLoad = shouldLoadGate,
        mayRequestNow = { gateOpen },
        newAd = { FakeMaxInterstitialSdkAd(it).also { fake -> built += fake } },
        telemetry = telemetry,
        failuresInARow = { counter.failuresInARow(MaxInterstitialSlot.SLOT) },
        logger = logger,
    )

    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    private fun loads(): Int = built.sumOf { it.loadCount }

    @Test
    fun load_beforeMaxReady_sentOnceWhenReady() = runTest {
        maxReady.value = false
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(0, loads())
        maxReady.value = true
        runCurrent()
        assertEquals(1, loads())
        assertEquals(FullScreenLoadState.Loading, slot.state)
        // MAX ready again later does not send again (wanted was used).
        maxReady.value = false
        runCurrent()
        maxReady.value = true
        runCurrent()
        assertEquals(1, loads())
    }

    @Test
    fun maxReady_notWanted_noRequest() = runTest {
        maxReady.value = false
        slot()
        runCurrent()
        maxReady.value = true
        runCurrent()
        assertEquals(0, loads())
    }

    @Test
    fun load_gateClosedAtSend_noRequest_idle_noRetry() = runTest {
        gateOpen = false
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(0, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
        at(60_000)
        assertEquals(0, loads())
        assertTrue(logger.entries.any { it.message == "Interstitial (MAX) not requested: consent gate closed" })
        // Control: the gate open, the same call sends.
        gateOpen = true
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads())
    }

    @Test
    fun loadFailed_retriesAfterFullScreenDelay() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204)
        assertEquals(FullScreenLoadState.WaitingRetry(2_000), slot.state)
        at(1_999)
        assertEquals(1, loads())
        at(2_000)
        assertEquals(2, loads())
        ad.emitLoadFailed(204)
        assertEquals(FullScreenLoadState.WaitingRetry(6_000), slot.state)
        at(5_999)
        assertEquals(2, loads())
        at(6_000)
        assertEquals(3, loads())
    }

    @Test
    fun loadFailed_metricsHaveMaxLabels_lastLoadErrorKept() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204)
        val fail = metrics.events.filterIsInstance<AdMetricEvent.LoadFail>().single()
        assertEquals("INTER", fail.adFormat)
        assertEquals("preload", fail.adPlacement)
        assertEquals("no_fill", slot.lastLoadError)
        assertEquals(1, counter.failuresInARow(MaxInterstitialSlot.SLOT))
        at(2_000)
        ad.emitLoaded()
        assertNull(slot.lastLoadError)
        assertEquals(FullScreenLoadState.Ready, slot.state)
        assertEquals(1, metrics.events.filterIsInstance<AdMetricEvent.LoadSuccess>().size)
    }

    @Test
    fun retryDue_gateClosed_noRequest() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204)
        gateOpen = false
        at(2_000)
        assertEquals(1, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
    }

    @Test
    fun retryDue_shouldLoadFalse_noRequest_idle() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204)
        // The order no longer has MAX, or every interstitial placement is off.
        shouldLoad = false
        at(2_000)
        assertEquals(1, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
        at(60_000)
        assertEquals(1, loads())
    }

    @Test
    fun closed_reloadsAfterTwoSeconds() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoaded()
        val taken = slot.take()
        assertSame(ad, taken)
        assertEquals(FullScreenLoadState.Idle, slot.state)
        slot.onClosedOrShowFailed()
        at(1_999)
        assertEquals(1, loads())
        at(2_000)
        assertEquals(2, loads())
    }

    @Test
    fun takenAd_loadWhileShowing_noLoad_thenClosedReloads() = runTest {
        // Review of T-069: the one SDK object shows; a load asked meanwhile (another placement's retry) waits.
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoaded()
        slot.take()
        slot.loadIfNeeded()
        runCurrent()
        at(5_000)
        assertEquals(1, loads())
        ad.emitHidden()
        slot.onClosedOrShowFailed()
        at(7_000)
        assertEquals(2, loads())
    }

    @Test
    fun takenAd_showingFlagLowered_loadAllowed() = runTest {
        // The turn ended without a callback (stuck recovery, F-03): the full-screen flag is down, the slot loads.
        var busy = true
        val slot = MaxInterstitialSlot(
            scope = backgroundScope,
            clock = MonotonicClock { testScheduler.currentTime },
            unitId = { UNIT },
            maxReady = maxReady,
            shouldLoad = { true },
            mayRequestNow = { true },
            newAd = { FakeMaxInterstitialSdkAd(it).also { fake -> built += fake } },
            telemetry = telemetry,
            failuresInARow = { 0 },
            logger = logger,
            fullScreenBusy = { busy },
        )
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoaded()
        slot.take()
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads())
        busy = false
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(2, loads())
    }

    @Test
    fun showFailed_reloadsAfterTwoSeconds() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoaded()
        slot.take()
        slot.onClosedOrShowFailed()
        at(2_000)
        assertEquals(2, loads())
    }

    @Test
    fun reloadAfterClose_gateClosed_noRequest() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoaded()
        slot.take()
        slot.onClosedOrShowFailed()
        gateOpen = false
        at(2_000)
        assertEquals(1, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
    }

    @Test
    fun reloadAfterClose_shouldLoadFalse_noRequest() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoaded()
        slot.take()
        slot.onClosedOrShowFailed()
        shouldLoad = false
        at(2_000)
        assertEquals(1, loads())
    }

    @Test
    fun maxReadyLater_gateClosed_noRequest() = runTest {
        maxReady.value = false
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        gateOpen = false
        maxReady.value = true
        runCurrent()
        assertEquals(0, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
    }

    @Test
    fun maxReadyLater_shouldLoadFalse_noRequest() = runTest {
        maxReady.value = false
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        shouldLoad = false
        maxReady.value = true
        runCurrent()
        assertEquals(0, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
    }

    @Test
    fun loadTwiceWhileLoading_oneRequest() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        slot.loadIfNeeded()
        runCurrent()
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads())
        assertEquals(1, built.size)
    }

    @Test
    fun loadTwice_whileShouldLoadSuspended_oneRequest() = runTest {
        val answer = CompletableDeferred<Boolean>()
        val slot = slot(shouldLoadGate = { answer.await() })
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(FullScreenLoadState.Loading, slot.state)
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(0, loads())
        answer.complete(true)
        runCurrent()
        assertEquals(1, loads())
    }

    @Test
    fun loaded_recordsApiCalled() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoaded()
        assertEquals(
            listOf(AdFunnelEvent.ApiCalled(AdFunnelKind.INTERS, "INTER", UNIT, "")),
            funnel.events.filterIsInstance<AdFunnelEvent.ApiCalled>(),
        )
    }

    @Test
    fun noUnit_neverBuildsOrLoads() = runTest {
        val slot = slot(unit = null)
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(0, built.size)
        assertEquals(FullScreenLoadState.Idle, slot.state)
    }

    @Test
    fun logs_neverContainUnitId() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204)
        at(2_000)
        ad.emitLoaded()
        slot.take()
        slot.onClosedOrShowFailed()
        gateOpen = false
        at(4_000)
        // Control: the slot logged something.
        assertTrue(logger.entries.isNotEmpty())
        assertTrue(logger.entries.none { UNIT in it.message })
    }

    private companion object {
        const val UNIT = "FAKE_UNIT_A"
        val FLAGS: AdsGlobalFlags = runBlocking { DefaultAdsFlags(FakeRemoteSettings()).global.first() }
    }
}
