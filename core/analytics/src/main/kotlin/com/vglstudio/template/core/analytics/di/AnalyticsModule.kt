package com.vglstudio.template.core.analytics.di

import com.vglstudio.template.core.analytics.AdMetrics
import com.vglstudio.template.core.analytics.Analytics
import com.vglstudio.template.core.analytics.FirebaseAdMetrics
import com.vglstudio.template.core.analytics.FirebaseAnalyticsLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AnalyticsModule {
    @Binds
    abstract fun bindAnalytics(impl: FirebaseAnalyticsLogger): Analytics

    @Binds
    abstract fun bindAdMetrics(impl: FirebaseAdMetrics): AdMetrics
}
