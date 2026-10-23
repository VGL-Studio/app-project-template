package com.vglstudio.template.core.ads

/** Limits of full-screen ads (interstitial and full-screen native), read from Remote Config. Never negative. */
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

/** Why a full-screen ad is not shown, in the order the rules are checked. */
internal enum class BlockReason {
    MASTER_SWITCH_OFF,
    HISTORY_UNAVAILABLE,
    FIRST_SESSION_GRACE,
    SESSION_CAP_REACHED,
    TOO_SOON,
}

/**
 * Decides whether a full-screen ad may be shown now (KB-6, KB-7), kept free of Android so it runs in unit tests.
 * Checks stop at the first rule that blocks. Unknown or unreadable data blocks, so errors mean fewer ads.
 * A stored time after [nowMillis] (the device clock was set back) is ignored, so ads are not blocked forever;
 * within one process [FullScreenAdGuard] still applies the interval on a monotonic clock.
 *
 * @param history `null` when the stored times could not be read.
 */
internal fun decideFullScreenAd(
    nowMillis: Long,
    history: FullScreenAdHistory?,
    shownThisSession: Int,
    limits: FullScreenAdLimits,
): FullScreenAdVerdict {
    if (!limits.adsEnabled) return FullScreenAdVerdict.Blocked(BlockReason.MASTER_SWITCH_OFF)
    if (history == null) return FullScreenAdVerdict.Blocked(BlockReason.HISTORY_UNAVAILABLE)
    val firstOpenAt = history.firstOpenAtMillis
    if (firstOpenAt == null || isWithin(nowMillis - firstOpenAt, limits.firstSessionGraceMillis)) {
        return FullScreenAdVerdict.Blocked(BlockReason.FIRST_SESSION_GRACE)
    }
    if (shownThisSession >= limits.maxPerSession) return FullScreenAdVerdict.Blocked(BlockReason.SESSION_CAP_REACHED)
    val lastShownAt = history.lastShownAtMillis
    if (lastShownAt != null && isWithin(nowMillis - lastShownAt, limits.minIntervalMillis)) {
        return FullScreenAdVerdict.Blocked(BlockReason.TOO_SOON)
    }
    return FullScreenAdVerdict.Allowed
}

/** A negative [elapsed] means the stored time is in the future: it is ignored. */
private fun isWithin(elapsed: Long, period: Long): Boolean = elapsed in 0 until period
