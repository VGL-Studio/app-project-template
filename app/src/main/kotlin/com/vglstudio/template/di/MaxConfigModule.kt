package com.vglstudio.template.di

import com.vglstudio.template.core.ads.MaxConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Skeleton of plan T-068, step 3: no key until step 4 reads it from the build. */
@Module
@InstallIn(SingletonComponent::class)
object MaxConfigModule {
    @Provides
    fun maxConfig(): MaxConfig = MaxConfig.NONE
}
