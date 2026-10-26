package com.vglstudio.template.core.analytics

import com.vglstudio.template.core.config.RemoteKey
import java.util.Locale

// SKELETON (T-033, step 3): replaced by the real mapping in step 4.

internal data class AfEvent(val name: String, val params: Map<String, Any>?)

internal fun afRevenue(event: AdMetricEvent.ImpressionRevenue, country: String): AfEvent = AfEvent("", null)

internal fun AdFunnelEvent.toAfEvent(): AfEvent? = null

internal fun AdFunnelEvent.switch(): RemoteKey<Boolean> = AppsFlyerSettings.IS_LOG_AF_AD_REVENUE

internal val REVENUE_SWITCH: RemoteKey<Boolean> = AppsFlyerSettings.IS_LOG_AF_AD_REVENUE

internal fun countryCode(sim: String?, network: String?, locale: Locale): String = ""
