package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/** P-02: ads a pool holds, counting the ones in use; null for a format with no pool. Skeleton (T-028, step 3). */
internal fun AdFormat.poolCapacity(): Int? = null

/** The formats whose ads come from a [NativeAdPool]. */
internal val AdFormat.usesPool: Boolean get() = poolCapacity() != null

/** One pool per (format, ad unit) (plan T-028, question 1). */
internal data class NativePoolKey(val format: AdFormat, val unitId: String)

/** The placement a loaded ad is shown for; empty until its show (PL-03). */
internal class PlacementCell {
    @Volatile var value: AdPlacement? = null
}

/** One ad a pool loaded, with the cell its metrics read the placement from (P-04). */
internal class PoolLoad(val ad: LoadedNativeAd, val holder: PlacementCell)

/** What a cell of a pool takes ads from. */
internal interface PooledNativeAds {
    fun claim(holder: AdPlacement): LoadedNativeAd?

    fun release(ad: LoadedNativeAd)

    val exhausted: Boolean
}

/** Skeleton of the pool (T-028, step 3). */
internal class NativeAdPool(
    val key: NativePoolKey,
    private val capacity: Int,
    private val load: suspend () -> PoolLoad?,
    private val sendGate: suspend () -> Boolean,
    private val canLoadNow: () -> Boolean,
    private val canShowNow: () -> Boolean,
    private val retryResets: StateFlow<Int>,
    private val clock: MonotonicClock,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
    private val logKey: String,
) : PooledNativeAds {
    val isLoading: Boolean get() = false

    override val exhausted: Boolean get() = false

    override fun claim(holder: AdPlacement): LoadedNativeAd? = null

    override fun release(ad: LoadedNativeAd) = Unit

    fun refill() = Unit

    fun settled(): Boolean = false

    fun close() = Unit

    fun reopen() = Unit
}
