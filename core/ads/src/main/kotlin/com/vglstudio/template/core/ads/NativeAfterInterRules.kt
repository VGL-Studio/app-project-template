package com.vglstudio.template.core.ads

// Skeleton of plan T-026, step 3: compiles, every rule still wrong.

internal const val NATIVE_AFTER_INTER_COUNTDOWN_MILLIS = 3_000L
internal const val INTER_GONE_WAIT_MILLIS = 2_000L
internal const val INTER_GONE_POLL_MILLIS = 200L

internal fun countdownSecondsLeft(deadline: Long, now: Long): Int = -1

internal suspend fun awaitInterGone(isGone: () -> Boolean): Boolean = false

internal data class NativeFullScreenView(val ad: LoadedNativeAd?, val secondsLeft: Int, val loadFailed: Boolean) {
    val closeEnabled: Boolean get() = false
    val backCloses: Boolean get() = false
}

internal fun comboOpportunity(everShown: Boolean, loadStateAtOpen: String): Pair<String, String> = "" to ""

internal class NativeLoadResults {
    fun signal(placement: AdPlacement) {}

    fun generation(placement: AdPlacement): Int = 0

    suspend fun await(placement: AdPlacement, since: Int) {}
}
