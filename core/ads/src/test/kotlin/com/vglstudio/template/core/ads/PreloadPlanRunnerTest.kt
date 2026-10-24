package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The groups of the preload plan on a virtual clock: order, 6 s cap, 200 ms checks (S-20, S-22). */
@OptIn(ExperimentalCoroutinesApi::class)
class PreloadPlanRunnerTest {
    private val logger = RecordingAppLogger()

    private fun TestScope.now(): Long = testScheduler.currentTime

    private fun TestScope.slot(format: AdFormat, settleAt: Long? = null) = FakePreloadSlot(format, { now() }, settleAt)

    private fun TestScope.runPlan(plan: AdPreloadPlan, slots: List<PreloadSlot>) {
        val runner = PreloadPlanRunner(MonotonicClock { now() }, logger)
        backgroundScope.launch { runner.run(plan, { format -> slots.filter { it.format == format } }) }
        runCurrent()
    }

    /** Moves the virtual clock to [millis] after the start and runs what is due. */
    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    private val twoGroups = AdPreloadPlan(listOf(setOf(AdFormat.APP_OPEN), setOf(AdFormat.INTERSTITIAL)))

    @Test
    fun nextGroupStartsWhenPreviousSettles() = runTest {
        val first = slot(AdFormat.APP_OPEN, settleAt = 500)
        val second = slot(AdFormat.INTERSTITIAL, settleAt = 0)
        runPlan(twoGroups, listOf(first, second))

        assertEquals(listOf(0L), first.loadCalls)
        at(400)
        assertEquals(emptyList<Long>(), second.loadCalls)
        at(600)
        assertEquals(listOf(600L), second.loadCalls)
        assertTrue(logger.entries.any { it.tag == "Ads" && it.message == "Preload group 1/2 moved on: settled" })
    }

    @Test
    fun unsettledGroup_movesOnAfter6s() = runTest {
        val first = slot(AdFormat.APP_OPEN)
        val second = slot(AdFormat.INTERSTITIAL, settleAt = 0)
        runPlan(twoGroups, listOf(first, second))

        at(5_800)
        assertEquals(emptyList<Long>(), second.loadCalls)
        at(6_000)
        assertEquals(listOf(6_000L), second.loadCalls)
        assertTrue(logger.entries.any { it.tag == "Ads" && it.message == "Preload group 1/2 moved on: timed out" })
    }

    @Test
    fun failedSlot_countsAsSettled() = runTest {
        // The fake settles at 300 the way a slot that failed (or waits for a retry) does.
        val first = slot(AdFormat.APP_OPEN, settleAt = 300)
        val second = slot(AdFormat.INTERSTITIAL, settleAt = 0)
        runPlan(twoGroups, listOf(first, second))

        at(399)
        assertEquals(emptyList<Long>(), second.loadCalls)
        at(400)
        assertEquals(listOf(400L), second.loadCalls)
    }

    @Test
    fun settledAtStart_movesOnAtOnce() = runTest {
        val first = slot(AdFormat.APP_OPEN, settleAt = 0)
        val second = slot(AdFormat.INTERSTITIAL, settleAt = 0)
        val third = slot(AdFormat.NATIVE_SMALL, settleAt = 0)
        val plan = AdPreloadPlan(
            listOf(setOf(AdFormat.APP_OPEN), setOf(AdFormat.INTERSTITIAL), setOf(AdFormat.NATIVE_SMALL)),
        )
        runPlan(plan, listOf(first, second, third))

        assertEquals(listOf(0L), second.loadCalls)
        assertEquals(listOf(0L), third.loadCalls)
        assertTrue(logger.entries.any { it.tag == "Ads" && it.message == "Preload plan finished" })
    }

    @Test
    fun formatWithoutSlots_isSettled() = runTest {
        val second = slot(AdFormat.INTERSTITIAL, settleAt = 0)
        // APP_OPEN has no slot: the first group has nothing to wait for.
        runPlan(twoGroups, listOf(second))

        assertEquals(listOf(0L), second.loadCalls)
        assertTrue(logger.entries.any { it.tag == "Ads" && it.message == "Preload group 1/2 moved on: settled" })
    }

    @Test
    fun slotNotInPlan_isNeverLoaded() = runTest {
        val banner = slot(AdFormat.BANNER, settleAt = 0)
        val interstitial = slot(AdFormat.INTERSTITIAL, settleAt = 0)
        val plan = AdPreloadPlan(listOf(setOf(AdFormat.INTERSTITIAL)))
        // The slots of the banner exist too; only the plan decides what loads.
        runPlan(plan, listOf(banner, interstitial))
        at(10_000)

        assertEquals(0, banner.loadCalls.size)
        // Control: the slot in the plan loads once.
        assertEquals(1, interstitial.loadCalls.size)
        assertTrue(logger.entries.any { it.tag == "Ads" && it.message == "Preload group 1/1 started: INTERSTITIAL" })
    }
}
