package com.vglstudio.template.core.ads

internal data class FullScreenAdLimits(
    val adsEnabled: Boolean,
    val minIntervalMillis: Long,
    val firstSessionGraceMillis: Long,
    val maxPerSession: Int,
)

/** Wall-clock times stored on the device; null when unknown. */
internal data class FullScreenAdHistory(val firstOpenAtMillis: Long?, val lastShownAtMillis: Long?)

internal sealed interface FullScreenAdVerdict {
    data object Allowed : FullScreenAdVerdict

    data class Blocked(val reason: BlockReason) : FullScreenAdVerdict
}

internal enum class BlockReason {
    MASTER_SWITCH_OFF,
    HISTORY_UNAVAILABLE,
    FIRST_SESSION_GRACE,
    SESSION_CAP_REACHED,
    TOO_SOON,
}

// Stub for the test-first commit.
internal fun decideFullScreenAd(
    nowMillis: Long,
    history: FullScreenAdHistory?,
    shownThisSession: Int,
    limits: FullScreenAdLimits,
): FullScreenAdVerdict = FullScreenAdVerdict.Allowed
