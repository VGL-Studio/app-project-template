package com.vglstudio.template.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The pure mappings of the ad metrics (docs/ops/ads-business-rules.md, sections 8.1 and 8.2; plan T-023). */
class AdTelemetryRulesTest {
    @Test
    fun revenueValue_microsToUnits() {
        assertEquals(0.0123, revenueValue(12_300), 0.0)
        assertEquals(0.0, revenueValue(0), 0.0)
        assertEquals("admob", adSourceOrAdmob(null))
        assertEquals("admob", adSourceOrAdmob(""))
        assertEquals("Pangle", adSourceOrAdmob("Pangle"))
    }

    @Test
    fun metricCode_coversEveryFormat() {
        // Copied by hand from section 3.1 of plan T-023 (T-04).
        val expected = mapOf(
            AdFormat.INTERSTITIAL to "INTER",
            AdFormat.REWARDED to "REWARDED",
            AdFormat.APP_OPEN to "APP_OPEN",
            AdFormat.BANNER to "BANNER",
            AdFormat.BANNER_COLLAPSIBLE to "BANNER",
            AdFormat.NATIVE_FULL_SCREEN to "NATIVE_FULL",
            AdFormat.NATIVE_COLLAPSIBLE to "NATIVE_COLLAP",
            AdFormat.NATIVE_SMALL to "NATIVE_SMALL",
            AdFormat.NATIVE_STEP to "NATIVE_ONBOARDING",
            AdFormat.NATIVE_IN_COLLECTION to "NATIVE_CONTENT",
            AdFormat.NATIVE_MEDIUM to "NATIVE_MEDIUM",
        )
        assertEquals(AdFormat.entries.toSet(), expected.keys)
        expected.forEach { (format, code) -> assertEquals(format.name, code, format.metricCode()) }
        assertEquals(
            setOf(
                "INTER", "REWARDED", "APP_OPEN", "BANNER", "NATIVE_FULL", "NATIVE_COLLAP", "NATIVE_SMALL",
                "NATIVE_ONBOARDING", "NATIVE_CONTENT", "NATIVE_MEDIUM",
            ),
            AdFormat.entries.map { it.metricCode() }.toSet(),
        )
    }

    @Test
    fun metricGroup_fullScreenFour() {
        assertEquals(
            setOf(AdFormat.INTERSTITIAL, AdFormat.REWARDED, AdFormat.APP_OPEN, AdFormat.NATIVE_FULL_SCREEN),
            AdFormat.entries.filter { it.metricGroup() == MetricGroup.FULL_SCREEN }.toSet(),
        )
        assertEquals(MetricGroup.SLOT, AdFormat.NATIVE_SMALL.metricGroup())
        assertEquals(MetricGroup.SLOT, AdFormat.BANNER.metricGroup())
    }

    @Test
    fun loadStateAfterError_table() {
        (0..11).forEach { code ->
            assertEquals("offline $code", "network", loadStateAfterError(code, offline = true, sdkThrottled = true))
            assertEquals("offline $code", "network", loadStateAfterError(code, offline = true, sdkThrottled = false))
        }
        assertEquals("no_fill", loadStateAfterError(3, offline = false, sdkThrottled = false))
        assertEquals("no_fill", loadStateAfterError(9, offline = false, sdkThrottled = false))
        assertEquals("sdk_throttled", loadStateAfterError(1, offline = false, sdkThrottled = true))
        assertEquals("invalid", loadStateAfterError(1, offline = false, sdkThrottled = false))
        listOf(8, 10, 11).forEach { assertEquals("code $it", "invalid", loadStateAfterError(it, false, false)) }
        assertEquals("internal", loadStateAfterError(0, offline = false, sdkThrottled = false))
        assertEquals("unknown", loadStateAfterError(2, offline = false, sdkThrottled = false))
        assertEquals("unknown", loadStateAfterError(7, offline = false, sdkThrottled = false))
    }

    @Test
    fun loadStateAtDecision_snapshot() {
        assertEquals("ready", loadStateAtDecision(FullScreenLoadState.Ready, lastErrorLabel = "no_fill"))
        assertEquals("loading", loadStateAtDecision(FullScreenLoadState.Loading, lastErrorLabel = "no_fill"))
        assertEquals("no_fill", loadStateAtDecision(FullScreenLoadState.Idle, lastErrorLabel = "no_fill"))
        assertEquals("idle", loadStateAtDecision(FullScreenLoadState.Idle, lastErrorLabel = null))
        val waiting = FullScreenLoadState.WaitingRetry(5_000)
        assertEquals("network", loadStateAtDecision(waiting, lastErrorLabel = "network"))
    }

    @Test
    fun showFailReason_table() {
        assertEquals("internal", showFailReasonOf(0))
        assertEquals("ad_reused", showFailReasonOf(1))
        assertEquals("not_ready", showFailReasonOf(2))
        assertEquals("app_not_foreground", showFailReasonOf(3))
        assertEquals("mediation_show_error", showFailReasonOf(4))
        assertEquals("unknown", showFailReasonOf(9))
    }

    @Test
    fun opportunityResult_ofDecision() {
        assertEquals("shown", FullScreenShowDecision.SHOW.opportunityResult())
        assertEquals("busy", FullScreenShowDecision.BUSY.opportunityResult())
        assertEquals("not_ready", FullScreenShowDecision.NOT_READY.opportunityResult())
        assertEquals("throttled", FullScreenShowDecision.THROTTLED.opportunityResult())
        assertNull(FullScreenShowDecision.CONSENT_CLOSED.opportunityResult())
        assertNull(FullScreenShowDecision.DISABLED.opportunityResult())
    }

    @Test
    fun loadPlacement_table() {
        val screen = AdPlacement.SAMPLE_NATIVE_SMALL
        listOf(AdFormat.INTERSTITIAL, AdFormat.NATIVE_FULL_SCREEN, AdFormat.APP_OPEN, AdFormat.REWARDED).forEach {
            assertEquals(it.name, "preload", loadPlacementOf(it, screen))
            assertEquals(it.name, "preload", loadPlacementOf(it, null))
        }
        assertEquals("sample_native_small", loadPlacementOf(AdFormat.NATIVE_SMALL, screen))
        assertEquals("preload", loadPlacementOf(AdFormat.NATIVE_SMALL, null))
        assertEquals("sample_native_small", loadPlacementOf(AdFormat.BANNER, screen))
        assertNull(loadPlacementOf(AdFormat.BANNER, null))
    }
}
