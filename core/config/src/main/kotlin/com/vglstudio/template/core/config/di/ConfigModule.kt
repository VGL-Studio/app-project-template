package com.vglstudio.template.core.config.di

import com.vglstudio.template.core.config.DefaultFeatureFlags
import com.vglstudio.template.core.config.FeatureFlags
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ConfigModule {
    @Binds
    abstract fun bindFeatureFlags(impl: DefaultFeatureFlags): FeatureFlags
}
