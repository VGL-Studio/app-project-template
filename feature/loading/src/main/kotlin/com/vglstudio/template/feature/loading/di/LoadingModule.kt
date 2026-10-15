package com.vglstudio.template.feature.loading.di

import android.os.SystemClock
import com.vglstudio.template.feature.loading.LoadingClock
import com.vglstudio.template.feature.loading.data.DefaultPreparationRepository
import com.vglstudio.template.feature.loading.data.PreparationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LoadingModule {
    @Binds
    abstract fun bindPreparationRepository(impl: DefaultPreparationRepository): PreparationRepository

    companion object {
        @Provides
        fun loadingClock(): LoadingClock = LoadingClock { SystemClock.elapsedRealtime() }
    }
}
