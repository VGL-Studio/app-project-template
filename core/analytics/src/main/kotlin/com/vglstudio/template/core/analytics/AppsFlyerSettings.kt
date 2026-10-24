package com.vglstudio.template.core.analytics

import com.vglstudio.template.core.config.RemoteKey

/**
 * Remote Config switches of the AppsFlyer events (docs/ops/ads-business-rules.md, sections 8.3 and 10.1; ADR-0027).
 * On by default. Declared for the key table only: nothing reads them yet (T-033). None of these names is in the
 * official AdsKit key table, so every description says the template named it.
 */
object AppsFlyerSettings {
    val IS_LOG_AF_AD_REVENUE = key("is_log_af_ad_revenue", "Sends af_ad_revenue for every paid ad impression.")
    val IS_LOG_AF_INTERS_AD_ELIGIBLE = key("is_log_af_inters_ad_eligible", "Sends af_inters_ad_eligible.")
    val IS_LOG_AF_INTERS_API_CALLED = key("is_log_af_inters_api_called", "Sends af_inters_api_called.")
    val IS_LOG_AF_INTERS_DISPLAYED = key("is_log_af_inters_displayed", "Sends af_inters_displayed.")
    val IS_LOG_AF_REWARDED_AD_ELIGIBLE = key("is_log_af_rewarded_ad_eligible", "Sends af_rewarded_ad_eligible.")
    val IS_LOG_AF_REWARDED_API_CALLED = key("is_log_af_rewarded_api_called", "Sends af_rewarded_api_called.")
    val IS_LOG_AF_REWARDED_AD_DISPLAYED = key("is_log_af_rewarded_ad_displayed", "Sends af_rewarded_ad_displayed.")
    val IS_LOG_AF_NATIVE_AD_ELIGIBLE = key("is_log_af_native_ad_eligible", "Sends af_native_ad_eligible.")
    val IS_LOG_AF_NATIVE_API_CALLED = key("is_log_af_native_api_called", "Sends af_native_api_called.")
    val IS_LOG_AF_NATIVE_AD_DISPLAYED = key("is_log_af_native_ad_displayed", "Sends af_native_ad_displayed.")
    val IS_LOG_AF_INTERS_DISPLAYED_N_TIMES =
        key("is_log_af_inters_displayed_n_times", "Sends af_inters_displayed_{N}_times.")

    /** Every key of the module, in the order of section 10.1. */
    fun all(): List<RemoteKey<*>> = listOf(
        IS_LOG_AF_AD_REVENUE,
        IS_LOG_AF_INTERS_AD_ELIGIBLE,
        IS_LOG_AF_INTERS_API_CALLED,
        IS_LOG_AF_INTERS_DISPLAYED,
        IS_LOG_AF_REWARDED_AD_ELIGIBLE,
        IS_LOG_AF_REWARDED_API_CALLED,
        IS_LOG_AF_REWARDED_AD_DISPLAYED,
        IS_LOG_AF_NATIVE_AD_ELIGIBLE,
        IS_LOG_AF_NATIVE_API_CALLED,
        IS_LOG_AF_NATIVE_AD_DISPLAYED,
        IS_LOG_AF_INTERS_DISPLAYED_N_TIMES,
    )

    private fun key(name: String, description: String): RemoteKey<Boolean> = RemoteKey.boolean(
        name = name,
        default = true,
        description = "$description AppsFlyer event switch. Name set by the template, not an AdsKit key.",
    )
}
