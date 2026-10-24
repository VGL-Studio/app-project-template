package com.vglstudio.template.core.ads

import com.vglstudio.template.core.config.RemoteSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Step 3 skeleton (plan T-019): fixed default values, RemoteSettings is ignored.
interface AdsFlags {
    val global: Flow<AdsGlobalFlags>

    fun placementEnabled(placement: AdPlacement): Flow<Boolean>

    fun intervalSeconds(group: AdIntervalGroup): Flow<Long>

    fun nativeEvery(placement: AdPlacement): Flow<Long?>
}

data class AdsGlobalFlags(
    val adsEnabled: Boolean,
    val showOpenAds: Boolean,
    val showOpenAdsFirstOpen: Boolean,
    val openAdType: OpenAdType,
    val showResumeAds: Boolean,
    val resumeAdType: OpenAdType,
    val adsIntervalSeconds: Long,
    val showNativeAfterInter: Boolean,
    val showNativeBottom: Boolean,
    val showNativeCollap: Boolean,
    val expandCollapAfterSeconds: Long,
    val consentShortcutEnabled: Boolean,
    val logFullScreenEvents: Boolean,
    val logSlotEvents: Boolean,
)

enum class OpenAdType { APP_OPEN, INTERSTITIAL }

@Suppress("UnusedPrivateProperty")
internal class DefaultAdsFlags @Inject constructor(private val settings: RemoteSettings) : AdsFlags {
    override val global: Flow<AdsGlobalFlags> = flowOf(
        AdsGlobalFlags(
            true, false, false, OpenAdType.APP_OPEN, false, OpenAdType.APP_OPEN, 45, false, false, false, 30, false,
            false, false,
        ),
    )

    override fun placementEnabled(placement: AdPlacement): Flow<Boolean> = placementEnabled(placement.declaration)

    internal fun placementEnabled(declaration: AdPlacementDeclaration): Flow<Boolean> = flowOf(false)

    override fun intervalSeconds(group: AdIntervalGroup): Flow<Long> = flowOf(45L)

    override fun nativeEvery(placement: AdPlacement): Flow<Long?> = nativeEvery(placement.declaration)

    internal fun nativeEvery(declaration: AdPlacementDeclaration): Flow<Long?> = flowOf(null)
}
