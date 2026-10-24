package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.LogLevel
import com.vglstudio.template.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Held, dropped and refill preloads of the plan on a virtual clock (S-09, S-21, S-23, S-24, S-25, HD-02). */
@OptIn(ExperimentalCoroutinesApi::class)
class AdPreloaderTest {
    private val logger = RecordingAppLogger()
    private val events = mutableListOf<String>()
    private var gate = OPEN

    private fun TestScope.now(): Long = testScheduler.currentTime

    private fun TestScope.slot(format: AdFormat, settleAt: Long? = 0, order: MutableList<AdFormat>? = null) =
        FakePreloadSlot(format, { now() }, settleAt, order)

    private fun TestScope.preloader(slots: List<PreloadSlot>) = AdPreloader(
        scope = backgroundScope,
        runner = PreloadPlanRunner(MonotonicClock { now() }, logger),
        slotsOf = { format -> slots.filter { it.format == format } },
        gate = {
            events += "gate"
            gate
        },
        askConsentAgain = { events += "ask" },
        logger = logger,
    )

    /** Moves the virtual clock to [millis] after the start and runs what is due. */
    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    private fun logged(message: String) = logger.entries.any { it.tag == "Ads" && it.message == message }

    @Test
    fun planSetBeforeFirstRun_isUsed() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val nativeFullScreen = slot(AdFormat.NATIVE_FULL_SCREEN)
        val preloader = preloader(listOf(interstitial, nativeFullScreen))

        preloader.setPlan(AdPreloadPlan(listOf(setOf(AdFormat.INTERSTITIAL))))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()

        assertEquals(1, interstitial.loadCalls.size)
        assertEquals(0, nativeFullScreen.loadCalls.size)
        assertTrue(logged("Preload SDK_READY: RUN"))

        // Control: the default plan loads both.
        val otherInterstitial = slot(AdFormat.INTERSTITIAL)
        val otherNative = slot(AdFormat.NATIVE_FULL_SCREEN)
        preloader(listOf(otherInterstitial, otherNative)).request(PreloadReason.SDK_READY)
        runCurrent()
        assertEquals(1, otherInterstitial.loadCalls.size)
        assertEquals(1, otherNative.loadCalls.size)
    }

    @Test
    fun planSetWhileRunning_appliesNextRun() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL, settleAt = null)
        val nativeFullScreen = slot(AdFormat.NATIVE_FULL_SCREEN)
        val preloader = preloader(listOf(interstitial, nativeFullScreen))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()

        at(100)
        preloader.setPlan(AdPreloadPlan(listOf(setOf(AdFormat.NATIVE_FULL_SCREEN))))
        assertTrue(logger.entries.any { it.tag == "Ads" && it.level == LogLevel.WARN })

        at(7_000)
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()
        assertEquals(listOf(0L), interstitial.loadCalls)
        assertEquals(listOf(0L, 7_000L), nativeFullScreen.loadCalls)
    }

    @Test
    fun heldUntilSdkReady() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val preloader = preloader(listOf(interstitial))
        gate = OPEN.copy(sdkReady = false)

        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        assertEquals(0, interstitial.loadCalls.size)

        gate = OPEN
        preloader.onGateChanged()
        runCurrent()
        assertEquals(1, interstitial.loadCalls.size)
    }

    @Test
    fun heldWhileGateClosed_runsOnceWhenOpen() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        assertEquals(1, interstitial.loadCalls.size)

        gate = OPEN.copy(mayRequest = false)
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()
        assertEquals(1, interstitial.loadCalls.size)

        gate = OPEN
        preloader.onGateChanged()
        runCurrent()
        preloader.onGateChanged()
        runCurrent()
        assertEquals(2, interstitial.loadCalls.size)
    }

    @Test
    fun consentDenied_dropsHeldRequest() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        gate = OPEN.copy(mayRequest = false)
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()

        gate = OPEN.copy(mayRequest = false, consentDenied = true)
        preloader.onGateChanged()
        gate = OPEN
        preloader.onGateChanged()
        runCurrent()
        assertEquals(1, interstitial.loadCalls.size)

        // S-14: the gate opened again by a new consent asks for its own preload.
        preloader.request(PreloadReason.GATE_REOPENED)
        runCurrent()
        assertEquals(2, interstitial.loadCalls.size)
    }

    @Test
    fun noHold_beforeSdkStarted() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val preloader = preloader(listOf(interstitial))
        gate = OPEN.copy(sdkStarted = false, mayRequest = false)

        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        gate = OPEN
        preloader.onGateChanged()
        runCurrent()
        assertEquals(0, interstitial.loadCalls.size)

        // Control (L-055): the same request with the SDK started is held, then runs.
        val other = slot(AdFormat.INTERSTITIAL)
        val otherPreloader = preloader(listOf(other))
        gate = OPEN.copy(mayRequest = false)
        otherPreloader.request(PreloadReason.SDK_READY)
        runCurrent()
        gate = OPEN
        otherPreloader.onGateChanged()
        runCurrent()
        assertEquals(1, other.loadCalls.size)
    }

    @Test
    fun noHold_beforeFirstPreload() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val preloader = preloader(listOf(interstitial))
        gate = OPEN.copy(mayRequest = false)

        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()
        gate = OPEN
        preloader.onGateChanged()
        runCurrent()
        assertEquals(0, interstitial.loadCalls.size)

        // Control (L-055): after the first preload, the same request is held, then runs.
        val other = slot(AdFormat.INTERSTITIAL)
        val otherPreloader = preloader(listOf(other))
        otherPreloader.request(PreloadReason.SDK_READY)
        runCurrent()
        gate = OPEN.copy(mayRequest = false)
        otherPreloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()
        gate = OPEN
        otherPreloader.onGateChanged()
        runCurrent()
        assertEquals(2, other.loadCalls.size)
    }

    @Test
    fun noOverlap_whileRunning() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL, settleAt = null)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()

        at(1_000)
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()

        assertEquals(listOf(0L), interstitial.loadCalls)
        assertTrue(logged("Preload NETWORK_BACK: IGNORE_RUNNING"))
    }

    @Test
    fun everyRequest_asksConsentAgain() = runTest {
        val preloader = preloader(listOf(slot(AdFormat.INTERSTITIAL)))

        preloader.request(PreloadReason.SDK_READY)
        preloader.request(PreloadReason.NETWORK_BACK)
        preloader.request(PreloadReason.PLACEMENT_ENABLED)

        assertEquals(listOf("ask", "gate", "ask", "gate", "ask", "gate"), events)
    }

    @Test
    fun networkReturn_preloadsAndResetsNativeWait() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val nativeFullScreen = slot(AdFormat.NATIVE_FULL_SCREEN)
        val preloader = preloader(listOf(interstitial, nativeFullScreen))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        val resets = preloader.nativeRetryResets.value

        at(5_000)
        preloader.onNetworkReturned()
        runCurrent()

        assertEquals(listOf(0L, 5_000L), interstitial.loadCalls)
        assertEquals(listOf(0L, 5_000L), nativeFullScreen.loadCalls)
        assertEquals(resets + 1, preloader.nativeRetryResets.value)
    }

    @Test
    fun activityResumed_refillsNativeInPlanOrder() = runTest {
        val order = mutableListOf<AdFormat>()
        val step = slot(AdFormat.NATIVE_STEP, order = order)
        val interstitial = slot(AdFormat.INTERSTITIAL, order = order)
        val small = slot(AdFormat.NATIVE_SMALL, order = order)
        val preloader = preloader(listOf(step, interstitial, small))
        preloader.setPlan(
            AdPreloadPlan(listOf(setOf(AdFormat.NATIVE_STEP), setOf(AdFormat.INTERSTITIAL, AdFormat.NATIVE_SMALL))),
        )
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        order.clear()

        preloader.onActivityResumed(fullScreenShowing = false)
        runCurrent()

        assertEquals(listOf(AdFormat.NATIVE_STEP, AdFormat.NATIVE_SMALL), order)
        assertEquals(1, interstitial.loadCalls.size)
    }

    @Test
    fun activityResumed_skipsWhenNeverFinishedRunningOrFullScreenShowing() = runTest {
        val small = slot(AdFormat.NATIVE_SMALL)
        val preloader = preloader(listOf(small))
        preloader.setPlan(AdPreloadPlan(listOf(setOf(AdFormat.NATIVE_SMALL))))

        // The plan never finished.
        preloader.onActivityResumed(fullScreenShowing = false)
        runCurrent()
        assertEquals(0, small.loadCalls.size)

        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        assertEquals(1, small.loadCalls.size)

        // A full-screen ad is on screen.
        preloader.onActivityResumed(fullScreenShowing = true)
        runCurrent()
        assertEquals(1, small.loadCalls.size)

        // Control (L-055): the same resume with nothing in the way refills.
        preloader.onActivityResumed(fullScreenShowing = false)
        runCurrent()
        assertEquals(2, small.loadCalls.size)

        // A plan is running.
        small.settleAt = null
        at(1_000)
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()
        assertEquals(3, small.loadCalls.size)
        preloader.onActivityResumed(fullScreenShowing = false)
        runCurrent()
        assertEquals(3, small.loadCalls.size)
    }

    private companion object {
        val OPEN = PreloadGate(sdkStarted = true, sdkReady = true, mayRequest = true, consentDenied = false)
    }
}
