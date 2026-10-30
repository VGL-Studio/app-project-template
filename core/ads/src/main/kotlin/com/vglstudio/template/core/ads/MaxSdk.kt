package com.vglstudio.template.core.ads

import android.content.Context
import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Skeleton of plan T-068, step 3: the tests are written first; step 4 writes the real code.

/** The AppLovin MAX SDK key of the build (ADR-0037). */
data class MaxConfig(val sdkKey: String) {
    val hasKey: Boolean get() = sdkKey.isNotBlank()

    companion object {
        val NONE = MaxConfig("")
    }
}

internal interface MaxPlatform {
    val isInitialized: Boolean

    fun initialize(sdkKey: String, onInitialized: () -> Unit)
}

internal class AppLovinMaxPlatform(private val context: Context) : MaxPlatform {
    override val isInitialized: Boolean get() = TODO("plan T-068, step 4")

    override fun initialize(sdkKey: String, onInitialized: () -> Unit): Unit = TODO("plan T-068, step 4")
}

internal class MaxSdkStarter(
    private val config: MaxConfig,
    private val platform: MaxPlatform,
    private val logger: AppLogger,
) {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    fun onAdmobReady(order: MediationOrder) = Unit

    fun onOrderChanged(order: MediationOrder) = Unit
}
