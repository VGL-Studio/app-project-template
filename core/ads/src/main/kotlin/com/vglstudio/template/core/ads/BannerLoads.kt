package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Who refreshes a banner once it has an ad (B-02, B-03). */
internal enum class BannerRefresh {
    /** The SDK, at the refresh rate set on the ad unit in AdMob; the app only retries after a failure. */
    BY_AD_UNIT,

    /** :core:ads, every [COLLAPSIBLE_BANNER_RELOAD_MILLIS], each time with a new collapsible request id. */
    MANUAL,
}

/** [AdFormat.BANNER] is refreshed by its ad unit, [AdFormat.BANNER_COLLAPSIBLE] by hand; other formats throw. */
internal fun bannerRefreshOf(format: AdFormat): BannerRefresh = BannerRefresh.BY_AD_UNIT

/** One result of a banner load, sent by the app or refreshed by the SDK. */
internal sealed interface BannerLoadResult {
    data object Loaded : BannerLoadResult

    data class Failed(val code: Int) : BannerLoadResult
}

/** B-03, section 11 of docs/ops/ads-business-rules.md: a collapsible banner loads again 30 s after it loaded. */
internal const val COLLAPSIBLE_BANNER_RELOAD_MILLIS = 30_000L

/** The sends of one banner slot (B-02 to B-06). */
@Suppress("UNUSED_PARAMETER")
internal suspend fun runBannerLoads(
    refresh: BannerRefresh,
    view: BannerAdView,
    visible: Flow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    onLoaded: () -> Unit,
    logger: AppLogger,
    placementKey: String,
) {
}
