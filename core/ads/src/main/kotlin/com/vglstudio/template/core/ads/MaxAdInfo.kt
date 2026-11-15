package com.vglstudio.template.core.ads

import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdWaterfallInfo

/** The result of one network MAX tried for a load. */
internal enum class MaxNetworkResult { LOADED, FAILED, NOT_TRIED }

/** One network MAX tried for a load: its name, latency and result; never its credentials or an error message. */
internal data class MaxNetworkAttempt(
    val networkName: String,
    val latencyMillis: Long,
    val result: MaxNetworkResult,
    val errorCode: Int?,
)

/** What :core:ads reads of one MAX ad (revenue, funnel, network log). [toString] leaves the ad unit out. */
internal class MaxAdInfo(
    val adUnitId: String,
    val revenueUsd: Double,
    val networkName: String?,
    val revenuePrecision: String?,
    val adapterClassName: String?,
    val networks: List<MaxNetworkAttempt>,
) {
    companion object {
        val EMPTY = MaxAdInfo("", 0.0, null, null, null, emptyList())
    }
}

internal fun maxAdInfoOf(ad: MaxAd?): MaxAdInfo = MaxAdInfo.EMPTY

internal fun maxNetworkAttemptsOf(waterfall: MaxAdWaterfallInfo?): List<MaxNetworkAttempt> = emptyList()

internal fun maxNetworkLines(key: String, attempts: List<MaxNetworkAttempt>): List<String> = emptyList()

internal fun maxRevenueValue(revenue: Double): Double = revenue
