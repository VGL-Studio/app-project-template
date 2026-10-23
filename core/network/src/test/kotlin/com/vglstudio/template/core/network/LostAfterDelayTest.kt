package com.vglstudio.template.core.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Virtual time: every emission is recorded with the virtual time it happened at. */
@OptIn(ExperimentalCoroutinesApi::class)
class LostAfterDelayTest {
    private val source = FakeConnectivitySource(connected = true)

    /** Collects lostAfterDelay in the background and returns the virtual times of its emissions. */
    private fun TestScope.collectLosses(delaySeconds: suspend () -> Long = { 2 }): List<Long> {
        val times = mutableListOf<Long>()
        backgroundScope.launch { source.lostAfterDelay(delaySeconds).collect { times += currentTime } }
        runCurrent()
        return times
    }

    private fun TestScope.setConnected(connected: Boolean) {
        source.connected.value = connected
        runCurrent()
    }

    /** Moves virtual time to [time] and runs everything due by then. */
    private fun TestScope.advanceTo(time: Long) {
        advanceTimeBy(time - currentTime)
        runCurrent()
    }

    @Test
    fun lossLongerThanDelay_emitsAfterDelay() = runTest {
        val times = collectLosses()
        setConnected(false)

        advanceTo(1_999)
        assertEquals(emptyList<Long>(), times)

        advanceTo(2_001)
        assertEquals(listOf(2_000L), times)
    }

    @Test
    fun flapping_emitsNothing() = runTest {
        val times = collectLosses()
        repeat(3) {
            setConnected(false)
            advanceTo(currentTime + 1_500)
            setConnected(true)
            advanceTo(currentTime + 500)
        }

        advanceTo(10_000)
        assertEquals(emptyList<Long>(), times)

        // Control: a loss as long as the delay emits, so the silence above comes from the short losses.
        setConnected(false)
        advanceTo(12_001)
        assertEquals(listOf(12_000L), times)
    }

    @Test
    fun delayIsReadAtEachLoss() = runTest {
        val delays = ArrayDeque(listOf(2L, 5L))
        val times = collectLosses { delays.removeFirst() }

        setConnected(false)
        advanceTo(3_000)
        setConnected(true)
        advanceTo(4_000)
        setConnected(false)
        advanceTo(20_000)

        assertEquals(listOf(2_000L, 9_000L), times)
    }

    @Test
    fun zeroOrNegativeDelay_emitsAtOnce() = runTest {
        val delays = ArrayDeque(listOf(0L, -3L))
        val times = collectLosses { delays.removeFirst() }

        setConnected(false)
        advanceTo(100)
        setConnected(true)
        advanceTo(200)
        setConnected(false)

        assertEquals(listOf(0L, 200L), times)
    }

    @Test
    fun offlineAtStart_emitsAfterDelay() = runTest {
        source.connected.value = false
        val times = collectLosses()

        advanceTo(1_999)
        assertEquals(emptyList<Long>(), times)

        advanceTo(2_001)
        assertEquals(listOf(2_000L), times)
    }

    @Test
    fun backOnlineDuringDelay_cancelsLoss() = runTest {
        val times = collectLosses()
        setConnected(false)
        advanceTo(1_000)
        setConnected(true)

        advanceTo(10_000)
        assertEquals(emptyList<Long>(), times)

        // Control: staying offline for the delay emits, so the silence above comes from coming back online.
        setConnected(false)
        advanceTo(12_001)
        assertEquals(listOf(12_000L), times)
    }

    @Test
    fun onlineSource_neverEmits() = runTest {
        val times = collectLosses()

        advanceTo(60_000)
        assertEquals(emptyList<Long>(), times)

        // Control: the same collection emits once the source reports a loss, so it was listening.
        setConnected(false)
        advanceTo(62_001)
        assertEquals(listOf(62_000L), times)
    }

    @Test
    fun collectsSourceOnlyWhileCollected() = runTest {
        assertEquals(0, source.collectors)

        val job = backgroundScope.launch { source.lostAfterDelay { 2 }.collect {} }
        runCurrent()
        assertEquals(1, source.collectors)

        job.cancel()
        runCurrent()
        assertEquals(0, source.collectors)
    }
}
