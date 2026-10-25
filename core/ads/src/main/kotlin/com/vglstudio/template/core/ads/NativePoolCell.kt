package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Skeleton (T-028, step 3). */
internal object PoolCellPolicy {
    const val RETRY_MILLIS = 500L
    const val EXHAUSTED_RETRY_MILLIS = 2_000L
}

/** Skeleton (T-028, step 3). */
internal fun pooledCellStates(
    pool: PooledNativeAds,
    holder: AdPlacement,
    logger: AppLogger,
    placementKey: String,
): Flow<NativeAdState> = flowOf(NativeAdState.Loading)

/** Skeleton (T-028, step 3). */
internal class PoolCellOpportunity(private val record: (result: String) -> Unit) {
    fun onState(hasAd: Boolean, viewed: Boolean) = Unit

    fun onLeave() = Unit
}
