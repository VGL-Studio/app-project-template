package com.vglstudio.template.core.ads.di

import com.vglstudio.template.core.ads.AdsController
import com.vglstudio.template.core.ads.StartupAds
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The ad of the loading screen (T-024), shown by the one ads controller of the app. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class StartupAdsModule {
    @Binds
    abstract fun bindStartupAds(impl: AdsController): StartupAds
}
