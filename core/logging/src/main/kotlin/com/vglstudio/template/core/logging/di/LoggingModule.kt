package com.vglstudio.template.core.logging.di

import com.vglstudio.template.core.logging.AppLogger
import com.vglstudio.template.core.logging.DefaultAppLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LoggingModule {
    @Binds
    abstract fun bindAppLogger(impl: DefaultAppLogger): AppLogger
}
