package com.vglstudio.template.core.network.di

import android.content.Context
import com.vglstudio.template.core.network.AndroidConnectivitySource
import com.vglstudio.template.core.network.ConnectivitySource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** The scope in which the network dialog controller runs, for the whole life of the app. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class NetworkScope

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun connectivitySource(@ApplicationContext context: Context): ConnectivitySource =
        AndroidConnectivitySource(context)

    @Provides
    @Singleton
    @NetworkScope
    fun networkScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
