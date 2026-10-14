package com.vglstudio.template.core.analytics

/**
 * An analytics event. Every event must be listed in the spec of its feature and approved
 * before it is added (risk-scoring.md, blocking point 4). Do not put personal data in [params].
 */
data class AnalyticsEvent(val name: String, val params: Map<String, String> = emptyMap())

/** Logs analytics events. Feature modules use this interface, never an analytics SDK directly. */
interface Analytics {
    fun log(event: AnalyticsEvent)
}
