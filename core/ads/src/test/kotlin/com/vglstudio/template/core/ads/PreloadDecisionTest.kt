package com.vglstudio.template.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The decision of one asked preload (S-21, S-23). */
class PreloadDecisionTest {
    private val open = PreloadGate(sdkStarted = true, sdkReady = true, mayRequest = true, consentDenied = false)

    private data class Row(
        val name: String,
        val reason: PreloadReason,
        val gate: PreloadGate,
        val everRequested: Boolean,
        val running: Boolean,
        val expected: PreloadDecision,
    )

    @Test
    fun order() {
        val rows = listOf(
            Row("open", PreloadReason.NETWORK_BACK, open, true, false, PreloadDecision.RUN),
            Row(
                "SDK not started, everything else wrong too",
                PreloadReason.NETWORK_BACK,
                PreloadGate(sdkStarted = false, sdkReady = false, mayRequest = false, consentDenied = true),
                false,
                true,
                PreloadDecision.IGNORE_SDK_NOT_STARTED,
            ),
            Row(
                "consent denied",
                PreloadReason.NETWORK_BACK,
                open.copy(consentDenied = true),
                true,
                false,
                PreloadDecision.IGNORE_CONSENT_DENIED,
            ),
            Row(
                "never requested",
                PreloadReason.NETWORK_BACK,
                open,
                false,
                false,
                PreloadDecision.IGNORE_NEVER_REQUESTED,
            ),
            Row("first preload", PreloadReason.SDK_READY, open, false, false, PreloadDecision.RUN),
            Row("running", PreloadReason.NETWORK_BACK, open, true, true, PreloadDecision.IGNORE_RUNNING),
            Row(
                "SDK not ready",
                PreloadReason.NETWORK_BACK,
                open.copy(sdkReady = false),
                true,
                false,
                PreloadDecision.HOLD,
            ),
            Row(
                "gate closed",
                PreloadReason.NETWORK_BACK,
                open.copy(mayRequest = false),
                true,
                false,
                PreloadDecision.HOLD,
            ),
        )
        rows.forEach { row ->
            assertEquals(row.name, row.expected, decidePreload(row.reason, row.gate, row.everRequested, row.running))
        }
    }

    @Test
    fun sdkReady_atCallbackOr35s() {
        assertFalse(sdkReadyAt(initStartedAt = 1_000, callbackArrived = false, now = 35_999))
        assertTrue(sdkReadyAt(initStartedAt = 1_000, callbackArrived = false, now = 36_000))
        assertTrue(sdkReadyAt(initStartedAt = 1_000, callbackArrived = true, now = 2_000))
        // S-19: never before the SDK was started, even with a callback.
        assertFalse(sdkReadyAt(initStartedAt = null, callbackArrived = true, now = 10_000_000))
        assertFalse(sdkReadyAt(initStartedAt = null, callbackArrived = false, now = 10_000_000))
    }
}
