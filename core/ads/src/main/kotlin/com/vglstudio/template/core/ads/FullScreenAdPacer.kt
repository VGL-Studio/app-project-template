package com.vglstudio.template.core.ads

import android.os.SystemClock
import com.vglstudio.template.core.logging.AppLogger

internal interface AdClock {
    fun wallClockMillis(): Long

    fun elapsedRealtimeMillis(): Long
}

internal object SystemAdClock : AdClock {
    override fun wallClockMillis(): Long = System.currentTimeMillis()

    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
}

// Stub for the test-first commit.
internal class FullScreenAdPacer(
    private val store: FullScreenAdHistoryStore,
    private val clock: AdClock,
    private val logger: AppLogger,
) {
    val shownCount: Int get() = 0

    suspend fun check(limits: FullScreenAdLimits): FullScreenAdVerdict = FullScreenAdVerdict.Allowed

    suspend fun onShown() = Unit

    suspend fun recordFirstOpen() = Unit

    fun onForeground() = Unit

    fun onBackground() = Unit
}
