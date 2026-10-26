package com.vglstudio.template.core.analytics

import android.content.Context
import com.vglstudio.template.core.logging.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// SKELETON (T-033, step 3).
@Singleton
internal class AppsFlyerTracker @Inject constructor(
    private val config: AppsFlyerConfig,
    private val sink: AppsFlyerSink,
    private val switches: AppsFlyerSwitches,
    private val country: CountrySource,
    private val logger: AppLogger,
) : AppsFlyerStartup,
    AdFunnel,
    AdMetrics {
    override fun prepare() = Unit

    override fun start() = Unit

    override fun log(event: AdMetricEvent) = Unit

    override fun log(event: AdFunnelEvent) = Unit
}

internal interface CountrySource {
    fun current(): String
}

internal class AndroidCountrySource @Inject constructor(@param:ApplicationContext private val context: Context) :
    CountrySource {
    override fun current(): String = ""
}
