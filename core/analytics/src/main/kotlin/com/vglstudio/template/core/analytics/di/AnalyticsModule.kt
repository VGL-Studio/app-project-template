package com.vglstudio.template.core.analytics.di

import com.vglstudio.template.core.analytics.AdFunnel
import com.vglstudio.template.core.analytics.AdMetrics
import com.vglstudio.template.core.analytics.Analytics
import com.vglstudio.template.core.analytics.AndroidCountrySource
import com.vglstudio.template.core.analytics.AppsFlyerLibSink
import com.vglstudio.template.core.analytics.AppsFlyerSink
import com.vglstudio.template.core.analytics.AppsFlyerStartup
import com.vglstudio.template.core.analytics.AppsFlyerTracker
import com.vglstudio.template.core.analytics.CountrySource
import com.vglstudio.template.core.analytics.FirebaseAdMetrics
import com.vglstudio.template.core.analytics.FirebaseAnalyticsLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// SKELETON (T-033, step 3): AdMetrics still goes to Firebase only.
@Module
@InstallIn(SingletonComponent::class)
internal abstract class AnalyticsModule {
    @Binds
    abstract fun bindAnalytics(impl: FirebaseAnalyticsLogger): Analytics

    @Binds
    abstract fun bindAdMetrics(impl: FirebaseAdMetrics): AdMetrics

    @Binds
    abstract fun bindAdFunnel(impl: AppsFlyerTracker): AdFunnel

    @Binds
    abstract fun bindAppsFlyerStartup(impl: AppsFlyerTracker): AppsFlyerStartup

    @Binds
    abstract fun bindAppsFlyerSink(impl: AppsFlyerLibSink): AppsFlyerSink

    @Binds
    abstract fun bindCountrySource(impl: AndroidCountrySource): CountrySource
}
