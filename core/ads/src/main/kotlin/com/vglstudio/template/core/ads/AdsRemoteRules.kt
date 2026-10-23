package com.vglstudio.template.core.ads

import com.vglstudio.template.core.config.RemoteSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Stubs for the test-first commit.
internal fun placementActive(settings: RemoteSettings, placement: AdPlacement): Flow<Boolean> =
    settings.value(AdsSettings.placementEnabled(placement))

internal fun fullScreenAdLimits(settings: RemoteSettings): Flow<FullScreenAdLimits> = flowOf(
    FullScreenAdLimits(adsEnabled = false, minIntervalMillis = 0, firstSessionGraceMillis = 0, maxPerSession = 0),
)
