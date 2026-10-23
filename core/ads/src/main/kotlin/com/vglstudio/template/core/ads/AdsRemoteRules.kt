package com.vglstudio.template.core.ads

import com.vglstudio.template.core.config.RemoteSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Whether [placement] may load and show ads: the master switch `ads_enabled` and the placement's own switch. */
internal fun placementActive(settings: RemoteSettings, placement: AdPlacement): Flow<Boolean> = combine(
    settings.value(AdsSettings.ENABLED),
    settings.value(AdsSettings.placementEnabled(placement)),
) { adsEnabled, placementEnabled -> adsEnabled && placementEnabled }
    .distinctUntilChanged()

/** Limits of full-screen ads from Remote Config, in milliseconds; negative remote values count as 0. */
internal fun fullScreenAdLimits(settings: RemoteSettings): Flow<FullScreenAdLimits> = combine(
    settings.value(AdsSettings.ENABLED),
    settings.value(AdsSettings.INTERSTITIAL_MIN_INTERVAL_SECONDS),
    settings.value(AdsSettings.FIRST_SESSION_GRACE_SECONDS),
    settings.value(AdsSettings.INTERSTITIAL_MAX_PER_SESSION_COUNT),
) { adsEnabled, minIntervalSeconds, graceSeconds, maxPerSession ->
    FullScreenAdLimits(
        adsEnabled = adsEnabled,
        minIntervalMillis = minIntervalSeconds.coerceIn(0, MAX_SECONDS) * 1_000,
        firstSessionGraceMillis = graceSeconds.coerceIn(0, MAX_SECONDS) * 1_000,
        maxPerSession = maxPerSession.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
    )
}

/** Keeps the conversion to milliseconds from overflowing. */
private const val MAX_SECONDS = Long.MAX_VALUE / 1_000
