package com.vglstudio.template.core.ads

import android.content.Context
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The AppLovin MAX SDK key of the build (ADR-0037), given by :app: `-PmaxSdkKey` in production, `ads.maxSdkKey` of
 * local.properties in staging. Without a key MAX never starts. [toString] hides the key, so it reaches no log.
 */
data class MaxConfig(val sdkKey: String) {
    val hasKey: Boolean get() = sdkKey.isNotBlank()

    override fun toString(): String = if (hasKey) "MaxConfig(sdkKey=${sdkKey.take(4)}…)" else "MaxConfig(no key)"

    companion object {
        val NONE = MaxConfig("")
    }
}

/** The AppLovin MAX SDK as :core:ads uses it; a fake in tests. */
internal interface MaxPlatform {
    /** True once the shared AppLovin SDK object is initialized, by MAX or by the AppLovin adapter of AdMob. */
    val isInitialized: Boolean

    fun initialize(sdkKey: String, onInitialized: () -> Unit)
}

/**
 * The real SDK. Consent is not set here: MAX reads the TCF and AC strings that UMP writes (plan T-068, section 2), so
 * neither `AppLovinPrivacySettings` nor the consent flow of MAX is used.
 */
internal class AppLovinMaxPlatform(private val context: Context) : MaxPlatform {
    override val isInitialized: Boolean get() = AppLovinSdk.getInstance(context).isInitialized

    override fun initialize(sdkKey: String, onInitialized: () -> Unit) {
        val configuration = AppLovinSdkInitializationConfiguration.builder(sdkKey)
            .setMediationProvider(AppLovinMediationProvider.MAX)
            .build()
        AppLovinSdk.getInstance(context).initialize(configuration) { onInitialized() }
    }
}

/**
 * Starts AppLovin MAX once (plan T-068, KB-31, ADR-0037): after the consent gate opened and AdMob is ready
 * ([onAdmobReady], called once by `startSdk`), with an SDK key, and while `ads_mediation_order` has MAX. There is no
 * stop: withdrawn consent closes the gate, which every request of MAX asks (S-15). Main thread.
 */
internal class MaxSdkStarter(
    private val config: MaxConfig,
    private val platform: MaxPlatform,
    private val logger: AppLogger,
) {
    private var gateOpened = false
    private var started = false
    private val _ready = MutableStateFlow(false)

    /** True once MAX answered its initialization. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    /** The consent gate opened and AdMob is ready (S-21). */
    fun onAdmobReady(order: MediationOrder) {
        if (gateOpened) return
        gateOpened = true
        if (!config.hasKey) logger.info(TAG, "MAX skipped: no SDK key")
        maybeStart(order)
    }

    /** `ads_mediation_order` changed remotely. */
    fun onOrderChanged(order: MediationOrder) = maybeStart(order)

    /** The only call of [MaxPlatform.initialize] (L-072). */
    private fun maybeStart(order: MediationOrder) {
        if (!gateOpened || started || !config.hasKey || !order.usesMax) return
        started = true
        if (platform.isInitialized) {
            logger.warn(
                TAG,
                "MAX: AppLovin SDK already started by AdMob mediation; " +
                    "remove AppLovin from the AdMob mediation groups (ADR-0037)",
            )
        }
        logger.info(TAG, "MAX started")
        platform.initialize(config.sdkKey) {
            _ready.value = true
            logger.info(TAG, "MAX ready")
        }
    }

    private companion object {
        const val TAG = "Ads"
    }
}
