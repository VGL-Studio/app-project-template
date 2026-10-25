package com.vglstudio.template.core.ads

import com.vglstudio.template.core.analytics.AdMetricEvent
import com.vglstudio.template.core.analytics.AdMetrics
import com.vglstudio.template.core.logging.AppLogger

/**
 * Records the ad metrics of :core:ads (docs/ops/ads-business-rules.md, sections 8.1 and 8.2; plan T-023).
 *
 * - Revenue ([revenue], [revenueFallback]) is always logged, whatever the switches or the consent gate say; the
 *   placement is read when the event comes (PL-03), and a placement that cannot be read counts as none (PL-04).
 * - Lifecycle events are logged only when the switch of the format's group ([lifecycleSwitches], read at every
 *   event) is on and ads are being served ([serving]).
 * - The [counter] moves on every request and result, metrics on or off (T-021 reads it).
 * - A failing [metrics] is swallowed and logged by class name only: the ads keep running. No message of the SDK is
 *   ever put in a param.
 */
internal class AdTelemetry(
    private val metrics: AdMetrics,
    /** Latest value of [AdsFlags.global], read at each event. */
    private val lifecycleSwitches: () -> AdsGlobalFlags,
    /** [AdsController.canRequestAds]. */
    private val serving: () -> Boolean,
    /** No usable Internet now. */
    private val offline: () -> Boolean,
    private val counter: LoadAttemptCounter,
    private val logger: AppLogger,
) {
    /** One paid event of the SDK (T-01). */
    fun revenue(
        format: AdFormat,
        unitId: String,
        placement: () -> AdPlacement?,
        valueMicros: Long,
        currency: String,
        adSource: String?,
        adapterClassName: String?,
    ) = logRevenue(format, unitId, placement, revenueValue(valueMicros), currency, adSource, adapterClassName, false)

    /** The zero-value impression of a native ad with no paid event 2 s after its impression (T-02). */
    fun revenueFallback(
        format: AdFormat,
        unitId: String,
        placement: () -> AdPlacement?,
        adSource: String?,
        adapterClassName: String?,
    ) = logRevenue(format, unitId, placement, 0.0, FALLBACK_CURRENCY, adSource, adapterClassName, true)

    /** The load of a slot with a failure counter: same as [requestSent], [loadSucceeded], [loadFailed]. */
    fun countedLoad(format: AdFormat, slot: String, placement: String?): LoadReport = object : LoadReport {
        override fun sent(now: Long) = requestSent(slot, now)

        override fun succeeded(now: Long) = loadSucceeded(format, slot, placement, now)

        override fun failed(code: Int, sdkMessage: String?, now: Long): String =
            loadFailed(format, slot, placement, code, sdkMessage, now)
    }

    /**
     * One request that does not go through the counter (a row of a list, like a pool: question 6, option B): no
     * `load_attempt`, latency from the send of this very request. Each call gives a new report.
     */
    fun perRequestLoad(format: AdFormat, placement: String?): LoadReport = object : LoadReport {
        @Volatile private var sentAt: Long? = null

        override fun sent(now: Long) {
            sentAt = now
        }

        override fun succeeded(now: Long) {
            if (!lifecycleOn(format)) return
            log(AdMetricEvent.LoadSuccess(format.metricCode(), placement, loadAttempt = null, latencyMs = latency(now)))
        }

        override fun failed(code: Int, sdkMessage: String?, now: Long): String {
            val label = loadStateAfterError(code, offline(), isSdkThrottledMessage(sdkMessage))
            if (lifecycleOn(format)) {
                log(
                    AdMetricEvent.LoadFail(
                        format.metricCode(),
                        placement,
                        loadAttempt = null,
                        latencyMs = latency(now),
                        loadErrorCode = code.toString(),
                        loadState = label,
                    ),
                )
            }
            return label
        }

        private fun latency(now: Long): Long? = sentAt?.let { now - it }
    }

    /** :core:ads sends a request for [slot] now. */
    fun requestSent(slot: String, now: Long) {
        counter.onRequestSent(slot, now)
    }

    fun loadSucceeded(format: AdFormat, slot: String, placement: String?, now: Long) {
        val outcome = counter.onLoaded(slot, now)
        if (!lifecycleOn(format)) return
        log(AdMetricEvent.LoadSuccess(format.metricCode(), placement, outcome.attempt, outcome.latencyMs))
    }

    /**
     * A load of [slot] failed with [code]. [sdkMessage] is only read here to tell the SDK's own throttling; it is
     * never logged or sent. Returns the `load_state` label of the error, kept by the caller for later opportunities.
     */
    fun loadFailed(
        format: AdFormat,
        slot: String,
        placement: String?,
        code: Int,
        sdkMessage: String?,
        now: Long,
    ): String {
        val outcome = counter.onFailed(slot, now)
        val label = loadStateAfterError(code, offline(), isSdkThrottledMessage(sdkMessage))
        if (lifecycleOn(format)) {
            log(
                AdMetricEvent.LoadFail(
                    format.metricCode(),
                    placement,
                    outcome.attempt,
                    outcome.latencyMs,
                    code.toString(),
                    label,
                ),
            )
        }
        return label
    }

    /** One call to show [placement]; no placement or no [result] is no opportunity (A 06:197). */
    fun opportunity(format: AdFormat, placement: AdPlacement?, result: String?, loadState: String) {
        if (placement == null || result == null || !lifecycleOn(format)) return
        log(AdMetricEvent.Opportunity(format.metricCode(), placement.key, result, loadState))
    }

    fun showFailed(format: AdFormat, placement: AdPlacement?, code: Int) {
        if (!lifecycleOn(format)) return
        log(AdMetricEvent.ShowFail(format.metricCode(), placement?.key, code.toString(), showFailReasonOf(code)))
    }

    fun clicked(format: AdFormat, placement: AdPlacement?) {
        if (!lifecycleOn(format)) return
        log(AdMetricEvent.Clicked(format.metricCode(), placement?.key))
    }

    private fun logRevenue(
        format: AdFormat,
        unitId: String,
        placement: () -> AdPlacement?,
        value: Double,
        currency: String,
        adSource: String?,
        adapterClassName: String?,
        fallback: Boolean,
    ) {
        val placementKey = try {
            placement()?.key
        } catch (e: Exception) {
            logger.warn(TAG, "Ad revenue placement not read: ${e.javaClass.simpleName}")
            null
        }
        logger.debug(TAG, "Ad revenue ${format.metricCode()} placement=${placementKey ?: "none"} fallback=$fallback")
        log(
            AdMetricEvent.ImpressionRevenue(
                adFormat = format.metricCode(),
                adSource = adSourceOrAdmob(adSource),
                adUnitName = unitId,
                currency = currency,
                value = value,
                adPlacement = placementKey,
                adapterClassName = adapterClassName,
            ),
        )
    }

    /** "Group on" (A 06:135-139): the group's switch is on and ads are being served, read again at every event. */
    private fun lifecycleOn(format: AdFormat): Boolean {
        val switches = lifecycleSwitches()
        val switchOn = when (format.metricGroup()) {
            MetricGroup.FULL_SCREEN -> switches.logFullScreenEvents
            MetricGroup.SLOT -> switches.logSlotEvents
        }
        return switchOn && serving()
    }

    private fun log(event: AdMetricEvent) {
        try {
            metrics.log(event)
        } catch (e: Exception) {
            // Class name only: the message of the error may hold anything.
            logger.warn(TAG, "Ad metric not logged: ${e.javaClass.simpleName}")
        }
    }

    private companion object {
        const val TAG = "Ads"
        const val FALLBACK_CURRENCY = "USD"
    }
}

/** One load and its result, reported once each (plan T-023, question 6). */
internal interface LoadReport {
    fun sent(now: Long)
    fun succeeded(now: Long)

    /** Returns the `load_state` label of the error, like [AdTelemetry.loadFailed]. */
    fun failed(code: Int, sdkMessage: String?, now: Long): String
}

/** How a native placement is loaded: one ad in its slot, or one row of a list (plan T-023, question 6). */
internal enum class NativeLoadUse { SINGLE_AD, LIST_ROW, REFRESH }

/** The load report of a native [placement] used as [use]; the choice of [AdsController], testable without the SDK. */
internal fun AdTelemetry.nativeLoadReport(
    placement: AdPlacement,
    use: NativeLoadUse,
    @Suppress("UNUSED_PARAMETER") format: AdFormat = placement.format,
): LoadReport {
    // A list row keeps the placement key of its screen (question 7); only a real pool (T-028) logs `pool`.
    val loadPlacement = loadPlacementOf(placement.format, placement)
    return when (use) {
        NativeLoadUse.SINGLE_AD, NativeLoadUse.REFRESH -> countedLoad(placement.format, placement.key, loadPlacement)
        NativeLoadUse.LIST_ROW -> perRequestLoad(placement.format, loadPlacement)
    }
}
