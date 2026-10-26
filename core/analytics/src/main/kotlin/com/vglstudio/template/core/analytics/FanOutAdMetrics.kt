package com.vglstudio.template.core.analytics

import com.vglstudio.template.core.logging.AppLogger

// SKELETON (T-033, step 3).
class FanOutAdMetrics(private val targets: List<AdMetrics>, private val logger: AppLogger) : AdMetrics {
    override fun log(event: AdMetricEvent) = Unit
}
