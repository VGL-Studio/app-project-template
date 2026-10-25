package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Skeleton of the pool manager (T-028, step 3). */
internal class NativeAdPools(
    private val placements: List<AdPlacement>,
    private val unitId: (AdPlacement) -> String?,
    private val enabled: (AdPlacement) -> Flow<Boolean>,
    private val canRequestAds: StateFlow<Boolean>,
    private val newPool: (NativePoolKey, AdPlacement) -> NativeAdPool,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
) {
    fun pool(placement: AdPlacement): NativeAdPool? = null

    fun preloadSlots(format: AdFormat): List<PreloadSlot> = emptyList()
}
