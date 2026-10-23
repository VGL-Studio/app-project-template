package com.vglstudio.template.core.ads

import com.vglstudio.template.core.ads.FullScreenAdVerdict.Allowed
import com.vglstudio.template.core.ads.FullScreenAdVerdict.Blocked
import org.junit.Assert.assertEquals
import org.junit.Test

class FullScreenAdPolicyTest {
    private val limits = FullScreenAdLimits(
        adsEnabled = true,
        minIntervalMillis = 45_000,
        firstSessionGraceMillis = 180_000,
        maxPerSession = 4,
    )
    private val first = 1_000_000L
    private val afterGrace = first + 1_000_000L

    private fun decide(
        now: Long = afterGrace,
        history: FullScreenAdHistory? = FullScreenAdHistory(firstOpenAtMillis = first, lastShownAtMillis = null),
        shown: Int = 0,
        limits: FullScreenAdLimits = this.limits,
    ) = decideFullScreenAd(nowMillis = now, history = history, shownThisSession = shown, limits = limits)

    @Test
    fun masterSwitchOff_blocksEvenWhenEverythingElseAllows() {
        assertEquals(Blocked(BlockReason.MASTER_SWITCH_OFF), decide(limits = limits.copy(adsEnabled = false)))
    }

    @Test
    fun historyUnavailable_blocks() {
        assertEquals(Blocked(BlockReason.HISTORY_UNAVAILABLE), decide(history = null))
    }

    @Test
    fun firstOpenUnknown_blocksAsGrace() {
        assertEquals(Blocked(BlockReason.FIRST_SESSION_GRACE), decide(history = FullScreenAdHistory(null, null)))
    }

    @Test
    fun withinGrace_blocks() {
        assertEquals(Blocked(BlockReason.FIRST_SESSION_GRACE), decide(now = first + 179_999))
    }

    @Test
    fun graceOver_allows() {
        // Control: one millisecond earlier is still in the grace period.
        assertEquals(Blocked(BlockReason.FIRST_SESSION_GRACE), decide(now = first + 179_999))
        assertEquals(Allowed, decide(now = first + 180_000))
    }

    @Test
    fun zeroGrace_allowsRightAfterFirstOpen() {
        // Control: the default grace blocks at the same moment.
        assertEquals(Blocked(BlockReason.FIRST_SESSION_GRACE), decide(now = first))
        assertEquals(Allowed, decide(now = first, limits = limits.copy(firstSessionGraceMillis = 0)))
    }

    @Test
    fun firstOpenInFuture_isIgnored() {
        // Control: a first open 1 ms in the past still blocks.
        assertEquals(Blocked(BlockReason.FIRST_SESSION_GRACE), decide(now = first + 1))
        assertEquals(Allowed, decide(now = first - 60_000))
    }

    @Test
    fun sessionCapReached_blocks() {
        assertEquals(Blocked(BlockReason.SESSION_CAP_REACHED), decide(shown = 4))
    }

    @Test
    fun belowSessionCap_allows() {
        // Control: one more ad reaches the cap.
        assertEquals(Blocked(BlockReason.SESSION_CAP_REACHED), decide(shown = 4))
        assertEquals(Allowed, decide(shown = 3))
    }

    @Test
    fun zeroCap_blocksEveryFullScreenAd() {
        assertEquals(
            Blocked(BlockReason.SESSION_CAP_REACHED),
            decide(shown = 0, limits = limits.copy(maxPerSession = 0)),
        )
    }

    @Test
    fun lastShownWithinInterval_blocksTooSoon() {
        val history = FullScreenAdHistory(first, lastShownAtMillis = afterGrace - 44_999)
        assertEquals(Blocked(BlockReason.TOO_SOON), decide(history = history))
    }

    @Test
    fun lastShownAtInterval_allows() {
        // Control: one millisecond later the last ad is too recent.
        assertEquals(
            Blocked(BlockReason.TOO_SOON),
            decide(history = FullScreenAdHistory(first, lastShownAtMillis = afterGrace - 44_999)),
        )
        assertEquals(Allowed, decide(history = FullScreenAdHistory(first, lastShownAtMillis = afterGrace - 45_000)))
    }

    @Test
    fun lastShownInFuture_isIgnored() {
        // Control: an ad shown 1 ms ago still blocks.
        assertEquals(Blocked(BlockReason.TOO_SOON), decide(history = FullScreenAdHistory(first, afterGrace - 1)))
        assertEquals(Allowed, decide(history = FullScreenAdHistory(first, lastShownAtMillis = afterGrace + 10_000)))
    }

    @Test
    fun graceIsReportedBeforeCapAndInterval() {
        val now = first + 100_000
        val history = FullScreenAdHistory(first, lastShownAtMillis = now - 1_000)
        assertEquals(Blocked(BlockReason.FIRST_SESSION_GRACE), decide(now = now, history = history, shown = 4))
    }
}
