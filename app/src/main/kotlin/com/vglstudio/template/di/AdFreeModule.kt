package com.vglstudio.template.di

import com.vglstudio.template.core.ads.AdFreeStatus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow

/** T-063 step 3 frame: a fresh status, not yet the shared NONE. */
@Module
@InstallIn(SingletonComponent::class)
object AdFreeModule {
    @Provides
    fun adFreeStatus(): AdFreeStatus = object : AdFreeStatus {
        override val adFree = MutableStateFlow(false)
    }
}
