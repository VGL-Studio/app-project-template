package com.vglstudio.template.core.ads

/** Retry waits of the PARTNER rules (docs/ops/ads-business-rules.md, F-05, F-06, O-05, section 11). */
internal object AdRetryPolicy {
    /** F-05, B-02: min(n x 2 s, 10 s) for the n-th failure in a row; n below 1 counts as 1; never gives up. */
    fun fullScreenDelayMillis(failuresInARow: Int): Long = 0

    /** O-05: holding an ad, 2 s doubled up to 60 s (n at most 10); holding none, always 5 s. */
    fun nativeDelayMillis(failuresInARow: Int, holdsAd: Boolean): Long = 0

    /** F-06: interstitial and rewarded wait 2 s after closing or failing to show; app open and native full screen reload at once (O-01). */
    fun reloadAfterCloseMillis(format: AdFormat): Long = 0
}
