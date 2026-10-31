package com.vglstudio.template.di

import com.vglstudio.template.BuildConfig
import com.vglstudio.template.core.ads.MaxConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * The AppLovin MAX SDK key of the build flavor, set in app/build.gradle.kts (ADR-0037, plan T-068): `-PmaxSdkKey` in
 * production, `ads.maxSdkKey` of local.properties in staging; none on CI, so MAX never starts there.
 */
@Module
@InstallIn(SingletonComponent::class)
object MaxConfigModule {
    @Provides
    fun maxConfig(): MaxConfig = MaxConfig(BuildConfig.MAX_SDK_KEY)
}
