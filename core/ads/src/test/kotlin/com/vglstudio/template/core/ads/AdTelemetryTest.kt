package com.vglstudio.template.core.ads

import com.vglstudio.template.core.analytics.AdMetricEvent
import com.vglstudio.template.core.analytics.AdMetrics
import com.vglstudio.template.core.analytics.RecordingAdMetrics
import com.vglstudio.template.core.logging.LogLevel
import com.vglstudio.template.core.logging.RecordingAppLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The recorder of ad metrics: group gate, placement read at event time, swallowed errors (plan T-023). */
class AdTelemetryTest {
    private val metrics = RecordingAdMetrics()
    private val logger = RecordingAppLogger()
    private val counter = LoadAttemptCounter()
    private var logFullScreen = false
    private var logSlot = false
    private var serving = false
    private var offline = false

    private fun telemetry(target: AdMetrics = metrics) = AdTelemetry(
        metrics = target,
        lifecycleSwitches = { DEFAULT_FLAGS.copy(logFullScreenEvents = logFullScreen, logSlotEvents = logSlot) },
        serving = { serving },
        offline = { offline },
        counter = counter,
        logger = logger,
    )

    @Test
    fun revenue_readsPlacementAtEventTime() {
        var cell: AdPlacement? = AdPlacement.SAMPLE_NATIVE_SMALL
        val placement = { cell }
        val telemetry = telemetry()
        // The listener is attached with the cell; the placement changes before the event comes.
        cell = AdPlacement.GALLERY_INTERSTITIAL

        telemetry.revenue(AdFormat.INTERSTITIAL, "unit", placement, 12_300, "USD", "AppLovin", "com.x.Adapter")

        val event = metrics.events.single() as AdMetricEvent.ImpressionRevenue
        assertEquals("gallery_interstitial", event.adPlacement)
        assertEquals("INTER", event.adFormat)
        assertEquals("AppLovin", event.adSource)
        assertEquals("unit", event.adUnitName)
        assertEquals("USD", event.currency)
        assertEquals(0.0123, event.value, 0.0)
        assertEquals("com.x.Adapter", event.adapterClassName)
    }

    @Test
    fun placementThrows_logsWithoutPlacement() {
        telemetry().revenue(
            AdFormat.NATIVE_SMALL,
            "unit",
            { throw IllegalStateException("gone") },
            1_000,
            "USD",
            null,
            null,
        )

        val event = metrics.events.single() as AdMetricEvent.ImpressionRevenue
        assertNull(event.adPlacement)
        assertEquals("admob", event.adSource)
    }

    @Test
    fun revenue_ignoresLifecycleSwitchesAndServing() {
        val telemetry = telemetry()

        telemetry.revenue(AdFormat.NATIVE_SMALL, "unit", { AdPlacement.SAMPLE_NATIVE_SMALL }, 0, "USD", null, null)
        assertEquals(1, metrics.events.size)

        // Control: under the same conditions a lifecycle event is not logged.
        telemetry.requestSent(SLOT, 0)
        telemetry.loadSucceeded(AdFormat.NATIVE_SMALL, SLOT, "sample_native_small", 10)
        assertEquals(1, metrics.events.size)
    }

    @Test
    fun fallback_logsUsdZero() {
        telemetry().revenueFallback(
            AdFormat.NATIVE_MEDIUM,
            "unit",
            { AdPlacement.GALLERY_NATIVE_MEDIUM },
            "Pangle",
            null,
        )

        val event = metrics.events.single() as AdMetricEvent.ImpressionRevenue
        assertEquals("USD", event.currency)
        assertEquals(0.0, event.value, 0.0)
        assertEquals("NATIVE_MEDIUM", event.adFormat)
        assertEquals("gallery_native_medium", event.adPlacement)
    }

    @Test
    fun lifecycleEvents_needGroupSwitchAndServing() {
        val telemetry = telemetry()
        logFullScreen = true
        serving = true
        telemetry.loadSucceeded(AdFormat.INTERSTITIAL, SLOT, "preload", 10)
        assertEquals(1, metrics.events.size)
        assertTrue(metrics.events.single() is AdMetricEvent.LoadSuccess)

        serving = false
        telemetry.loadSucceeded(AdFormat.INTERSTITIAL, SLOT, "preload", 20)
        assertEquals(1, metrics.events.size)

        serving = true
        logFullScreen = false
        telemetry.loadSucceeded(AdFormat.INTERSTITIAL, SLOT, "preload", 30)
        assertEquals(1, metrics.events.size)

        // A slot format follows is_log_ad_slot_events, not the full-screen key.
        logFullScreen = true
        logSlot = false
        telemetry.loadSucceeded(AdFormat.NATIVE_SMALL, NATIVE_SLOT, "sample_native_small", 40)
        assertEquals(1, metrics.events.size)
        logFullScreen = false
        logSlot = true
        telemetry.loadSucceeded(AdFormat.NATIVE_SMALL, NATIVE_SLOT, "sample_native_small", 50)
        assertEquals(2, metrics.events.size)
        assertEquals("NATIVE_SMALL", metrics.events.last().adFormat)
    }

    @Test
    fun switchChangedMidSession_appliesToNextEvent() {
        val telemetry = telemetry()
        serving = true
        logSlot = false
        telemetry.clicked(AdFormat.NATIVE_SMALL, AdPlacement.SAMPLE_NATIVE_SMALL)
        assertTrue(metrics.events.isEmpty())

        logSlot = true
        telemetry.clicked(AdFormat.NATIVE_SMALL, AdPlacement.SAMPLE_NATIVE_SMALL)
        assertEquals(
            listOf(AdMetricEvent.Clicked("NATIVE_SMALL", "sample_native_small")),
            metrics.events,
        )

        logSlot = false
        telemetry.clicked(AdFormat.NATIVE_SMALL, AdPlacement.SAMPLE_NATIVE_SMALL)
        assertEquals(1, metrics.events.size)
    }

    @Test
    fun opportunity_withoutPlacementOrResult_isSkipped() {
        val telemetry = telemetry()
        logFullScreen = true
        serving = true

        telemetry.opportunity(AdFormat.INTERSTITIAL, null, "not_ready", "idle")
        telemetry.opportunity(AdFormat.INTERSTITIAL, AdPlacement.GALLERY_INTERSTITIAL, null, "idle")
        assertTrue(metrics.events.isEmpty())

        // Control (L-055): with both the opportunity is logged.
        telemetry.opportunity(AdFormat.INTERSTITIAL, AdPlacement.GALLERY_INTERSTITIAL, "not_ready", "loading")
        assertEquals(
            listOf(AdMetricEvent.Opportunity("INTER", "gallery_interstitial", "not_ready", "loading")),
            metrics.events,
        )
    }

    @Test
    fun loadCounter_updatesWhenLoggingOff() {
        val telemetry = telemetry()
        logFullScreen = false
        logSlot = false
        serving = false

        telemetry.requestSent(SLOT, 0)
        telemetry.loadFailed(AdFormat.INTERSTITIAL, SLOT, "preload", 3, null, 10)
        assertEquals(1, counter.failuresInARow(SLOT))
        telemetry.requestSent(SLOT, 20)
        telemetry.loadFailed(AdFormat.INTERSTITIAL, SLOT, "preload", 3, null, 30)
        assertEquals(2, counter.failuresInARow(SLOT))
        telemetry.requestSent(SLOT, 40)
        telemetry.loadSucceeded(AdFormat.INTERSTITIAL, SLOT, "preload", 50)
        assertEquals(0, counter.failuresInARow(SLOT))
        assertTrue(metrics.events.isEmpty())

        // Control (L-055): the same sequence with logging on gives the same counts, and events.
        logFullScreen = true
        serving = true
        telemetry.requestSent(SLOT, 100)
        telemetry.loadFailed(AdFormat.INTERSTITIAL, SLOT, "preload", 3, null, 110)
        assertEquals(1, counter.failuresInARow(SLOT))
        telemetry.requestSent(SLOT, 120)
        telemetry.loadFailed(AdFormat.INTERSTITIAL, SLOT, "preload", 3, null, 130)
        assertEquals(2, counter.failuresInARow(SLOT))
        telemetry.requestSent(SLOT, 140)
        telemetry.loadSucceeded(AdFormat.INTERSTITIAL, SLOT, "preload", 150)
        assertEquals(0, counter.failuresInARow(SLOT))
        assertEquals(
            listOf(
                AdMetricEvent.LoadFail("INTER", "preload", 1, 10, "3", "no_fill"),
                AdMetricEvent.LoadFail("INTER", "preload", 2, 10, "3", "no_fill"),
                AdMetricEvent.LoadSuccess("INTER", "preload", 3, 10),
            ),
            metrics.events,
        )
    }

    @Test
    fun loadFailed_returnsLoadStateLabel() {
        val telemetry = telemetry()

        assertEquals("no_fill", telemetry.loadFailed(AdFormat.INTERSTITIAL, SLOT, "preload", 3, null, 0))
        offline = true
        assertEquals("network", telemetry.loadFailed(AdFormat.INTERSTITIAL, SLOT, "preload", 3, null, 0))
        offline = false
        assertEquals(
            "sdk_throttled",
            telemetry.loadFailed(AdFormat.INTERSTITIAL, SLOT, "preload", 1, "Too many recently failed requests", 0),
        )
        assertEquals("invalid", telemetry.loadFailed(AdFormat.INTERSTITIAL, SLOT, "preload", 1, "Bad request", 0))
    }

    @Test
    fun throwingMetrics_isSwallowedAndLogged() {
        val throwing = object : AdMetrics {
            override fun log(event: AdMetricEvent) = throw IllegalStateException("secret detail")
        }
        val telemetry = telemetry(throwing)
        logFullScreen = true
        serving = true

        telemetry.revenue(AdFormat.INTERSTITIAL, "unit", { AdPlacement.GALLERY_INTERSTITIAL }, 1, "USD", null, null)
        telemetry.requestSent(SLOT, 0)
        telemetry.loadFailed(AdFormat.INTERSTITIAL, SLOT, "preload", 3, null, 10)

        val warnings = logger.entries.filter { it.level == LogLevel.WARN }
        assertEquals(2, warnings.size)
        warnings.forEach {
            assertEquals("Ads", it.tag)
            assertTrue(it.message, it.message.contains("IllegalStateException"))
            assertFalse(it.message, it.message.contains("secret detail"))
        }
        // The counter still moved.
        assertEquals(1, counter.failuresInARow(SLOT))
    }

    @Test
    fun sdkMessage_neverInParams() {
        val telemetry = telemetry()
        logFullScreen = true
        serving = true

        telemetry.requestSent(SLOT, 0)
        telemetry.loadFailed(AdFormat.INTERSTITIAL, SLOT, "preload", 0, "unit ca-app-pub-x", 10)

        val event = metrics.events.single() as AdMetricEvent.LoadFail
        assertEquals("0", event.loadErrorCode)
        val values = listOf(
            event.adFormat,
            event.adPlacement,
            event.loadAttempt,
            event.latencyMs,
            event.loadErrorCode,
            event.loadState,
        )
        assertFalse(values.any { it.toString().contains("ca-app-pub-x") })
    }

    private companion object {
        const val SLOT = "preload:INTER"
        const val NATIVE_SLOT = "sample_native_small"

        /** The defaults of T-019, as read when Remote Config has no value. */
        val DEFAULT_FLAGS: AdsGlobalFlags = runBlocking { DefaultAdsFlags(FakeRemoteSettings()).global.first() }
    }
}
