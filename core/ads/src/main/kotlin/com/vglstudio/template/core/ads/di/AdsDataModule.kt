package com.vglstudio.template.core.ads.di

import com.vglstudio.template.core.ads.DataStoreFullScreenAdHistoryStore
import com.vglstudio.template.core.ads.FullScreenAdHistoryStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Stores the times of full-screen ads in the app's shared DataStore of :core:data. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class AdsDataModule {
    @Binds
    abstract fun bindFullScreenAdHistoryStore(impl: DataStoreFullScreenAdHistoryStore): FullScreenAdHistoryStore
}
