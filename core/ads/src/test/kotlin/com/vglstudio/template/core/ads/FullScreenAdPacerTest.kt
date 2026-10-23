package com.vglstudio.template.core.ads

import com.vglstudio.template.core.ads.FullScreenAdVerdict.Allowed
import com.vglstudio.template.core.ads.FullScreenAdVerdict.Blocked
import com.vglstudio.template.core.logging.LogLevel
import com.vglstudio.template.core.logging.RecordingAppLogger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FullScreenAdPacerTest {
    private val limits = FullScreenAdLimits(
        adsEnabled = true,
        minIntervalMillis = 45_000,
        firstSessionGraceMillis = 180_000,
        maxPerSession = 4,
    )
    private val first = 1_000_000L
    private val clock = FakeAdClock(wallMillis = first, elapsedMillis = 5_000)
    private val logger = RecordingAppLogger()

    private fun pacer(store: FullScreenAdHistoryStore) = FullScreenAdPacer(store, clock, logger)

    @Test
    fun intervalCountsAcrossAppRestarts() = runTest {
        val store = InMemoryFullScreenAdHistoryStore(firstOpenAtMillis = first)
        clock.wallMillis = first + 1_000_000
        val shownAt = clock.wallMillis
        pacer(store).onShown()

        // A new process: new pacer, new session, same stored history.
        val restarted = pacer(store)
        clock.wallMillis = shownAt + 10_000
        assertEquals(Blocked(BlockReason.TOO_SOON), restarted.check(limits))
        clock.wallMillis = shownAt + 45_000
        assertEquals(Allowed, restarted.check(limits))
    }

    @Test
    fun recordFirstOpen_startsGrace() = runTest {
        val store = InMemoryFullScreenAdHistoryStore()
        val pacer = pacer(store)
        pacer.recordFirstOpen()
        clock.wallMillis = first + 100_000
        assertEquals(Blocked(BlockReason.FIRST_SESSION_GRACE), pacer.check(limits))
        clock.wallMillis = first + 180_000
        assertEquals(Allowed, pacer.check(limits))
    }

    @Test
    fun recordFirstOpen_isWrittenOnce() = runTest {
        val store = InMemoryFullScreenAdHistoryStore()
        val pacer = pacer(store)
        pacer.recordFirstOpen()
        clock.wallMillis = first + 500_000
        pacer.recordFirstOpen()
        assertEquals(first, store.firstOpenAtMillis)
    }

    @Test
    fun foregroundAlone_doesNotRecordFirstOpen() = runTest {
        val store = InMemoryFullScreenAdHistoryStore()
        val pacer = pacer(store)
        pacer.onForeground()
        assertNull(store.firstOpenAtMillis)

        // recordFirstOpen needs no foreground before it.
        val other = InMemoryFullScreenAdHistoryStore()
        pacer(other).recordFirstOpen()
        assertEquals(first, other.firstOpenAtMillis)
    }

    @Test
    fun recordFirstOpenWriteFailure_isLoggedNotThrown() = runTest {
        val store = InMemoryFullScreenAdHistoryStore().apply { failWrites = true }
        val pacer = pacer(store)
        pacer.recordFirstOpen()
        val warnings = logger.entries.filter { it.level == LogLevel.WARN && it.tag == "Ads" }
        assertEquals(1, warnings.size)
        clock.wallMillis = first + 1_000_000
        assertEquals(Blocked(BlockReason.FIRST_SESSION_GRACE), pacer.check(limits))
    }

    @Test
    fun fifthAdInSession_isBlockedUntilNextSession() = runTest {
        val store = InMemoryFullScreenAdHistoryStore(firstOpenAtMillis = first)
        val pacer = pacer(store)
        clock.advanceBy(1_000_000)
        repeat(4) {
            assertEquals(Allowed, pacer.check(limits))
            pacer.onShown()
            clock.advanceBy(60_000)
        }
        assertEquals(Blocked(BlockReason.SESSION_CAP_REACHED), pacer.check(limits))

        pacer.onBackground()
        clock.advanceBy(FullScreenAdSession.SESSION_TIMEOUT_MILLIS)
        pacer.onForeground()
        assertEquals(Allowed, pacer.check(limits))
    }

    @Test
    fun unreadableHistory_blocksAndLogs() = runTest {
        val store = InMemoryFullScreenAdHistoryStore(firstOpenAtMillis = first).apply { failReads = true }
        clock.wallMillis = first + 1_000_000
        assertEquals(Blocked(BlockReason.HISTORY_UNAVAILABLE), pacer(store).check(limits))
        val infos = logger.entries.filter { it.level == LogLevel.INFO && it.tag == "Ads" }
        assertEquals(1, infos.size)
        assertTrue(infos.single().message, "HISTORY_UNAVAILABLE" in infos.single().message)
    }

    @Test
    fun writeFailureOnShown_isLoggedAndStillCounted() = runTest {
        val store = InMemoryFullScreenAdHistoryStore(firstOpenAtMillis = first).apply { failWrites = true }
        val pacer = pacer(store)
        pacer.onShown()
        assertEquals(1, pacer.shownCount)
        val warnings = logger.entries.filter { it.level == LogLevel.WARN && it.tag == "Ads" }
        assertEquals(1, warnings.size)
    }

    @Test
    fun logsHoldNoTimestamps() = runTest {
        val store = InMemoryFullScreenAdHistoryStore()
        val pacer = pacer(store)
        val usedTimes = mutableListOf<Long>()
        pacer.recordFirstOpen()
        usedTimes += clock.wallMillis
        clock.wallMillis = first + 1_000
        usedTimes += clock.wallMillis
        pacer.check(limits) // blocked by the grace period, logged
        clock.wallMillis = first + 1_000_000
        usedTimes += clock.wallMillis
        pacer.onShown()
        store.failWrites = true
        clock.wallMillis = first + 2_000_000
        usedTimes += clock.wallMillis
        pacer.onShown() // write failure, logged
        pacer.recordFirstOpen() // write failure, logged

        // Control: something was logged, so the check below is not empty.
        assertTrue("Nothing logged", logger.entries.size >= 3)
        logger.entries.forEach { entry ->
            usedTimes.forEach { time -> assertFalse(entry.message, time.toString() in entry.message) }
        }
    }
}
