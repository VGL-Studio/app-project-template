package com.vglstudio.template.core.ads.di

import com.vglstudio.template.core.ads.AdsFlags
import com.vglstudio.template.core.ads.DefaultAdsFlags
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The ad flags read from Remote Config, for :core:ads and the app shell. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class AdsFlagsModule {
    @Binds
    abstract fun bindAdsFlags(impl: DefaultAdsFlags): AdsFlags
}
