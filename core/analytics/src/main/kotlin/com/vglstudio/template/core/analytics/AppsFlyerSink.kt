package com.vglstudio.template.core.analytics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

// SKELETON (T-033, step 3).
internal interface AppsFlyerSink {
    fun init(devKey: String)
    fun start()
    fun logEvent(name: String, params: Map<String, Any>?)
}

internal class AppsFlyerLibSink @Inject constructor(@param:ApplicationContext private val context: Context) :
    AppsFlyerSink {
    override fun init(devKey: String) = Unit

    override fun start() = Unit

    override fun logEvent(name: String, params: Map<String, Any>?) = Unit
}
