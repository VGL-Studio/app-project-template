package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The result of one [NativeAdSource.claim]. */
internal sealed interface NativeClaim {
    data class Ad(val ad: LoadedNativeAd) : NativeClaim

    /** No ad came (error, timeout, source closed). */
    data object NoAd : NativeClaim

    /** The gate was closed right before the send, or the placement is off: not a failure. */
    data object Blocked : NativeClaim
}

/** The slot state of a claim; null when the claim was blocked. */
internal fun NativeClaim.toStateOrNull(): NativeAdState? = null

/** Times of the native single-ad rules (docs/ops/ads-business-rules.md, O-04, CL-02). */
internal object NativeAdPolicy {
    const val MAX_AGE_MILLIS = 55 * 60 * 1_000L
    const val REFRESH_INTERVAL_MILLIS = 60_000L
    const val REFRESH_WAIT_MILLIS = 10_000L
}

/** Skeleton (plan T-040, step 3). */
@Suppress("UNUSED_PARAMETER", "unused")
internal class NativeAdSource(
    private val load: suspend (NativeLoadUse) -> NativeAdState,
    private val sendGate: suspend () -> Boolean,
    private val clock: MonotonicClock,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
    private val placementKey: String,
    private val allowed: () -> Boolean = { true },
) {
    private val closedCount = MutableStateFlow(0L)

    val closed: StateFlow<Long> = closedCount

    val isLoading: Boolean get() = false

    val pending: LoadedNativeAd? get() = null

    fun attach() = Unit

    fun detach() = Unit

    fun shutDown() = Unit

    suspend fun claim(use: NativeLoadUse): NativeClaim = NativeClaim.NoAd
}
