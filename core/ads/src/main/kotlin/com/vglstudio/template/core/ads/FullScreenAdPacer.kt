package com.vglstudio.template.core.ads

import android.os.SystemClock
import com.vglstudio.template.core.logging.AppLogger
import java.io.IOException

/** Wall clock (stored times) and monotonic clock (session), replaced by a fake in unit tests. */
internal interface AdClock {
    fun wallClockMillis(): Long

    fun elapsedRealtimeMillis(): Long
}

internal object SystemAdClock : AdClock {
    override fun wallClockMillis(): Long = System.currentTimeMillis()

    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
}

/**
 * Applies the frequency rules shared by every full-screen ad (interstitial today, full-screen native later):
 * master switch, first-session grace, cap per session and minimum interval kept across app restarts (KB-6, KB-7).
 * Logs hold only reasons, never times (ADR-0019). Main thread only.
 */
internal class FullScreenAdPacer(
    private val store: FullScreenAdHistoryStore,
    private val clock: AdClock,
    private val logger: AppLogger,
) {
    private val session = FullScreenAdSession()

    /** Full-screen ads shown in the current session. */
    val shownCount: Int get() = session.shownCount

    suspend fun check(limits: FullScreenAdLimits): FullScreenAdVerdict {
        val verdict = decideFullScreenAd(clock.wallClockMillis(), store.read(), session.shownCount, limits)
        if (verdict is FullScreenAdVerdict.Blocked) logger.info(TAG, "Full-screen ad blocked: ${verdict.reason}")
        return verdict
    }

    /** Counts the ad in the session and saves its time. A write error is logged, never thrown. */
    suspend fun onShown() {
        session.onShown()
        logger.debug(TAG, "Full-screen ads this session: ${session.shownCount}")
        try {
            store.recordShown(clock.wallClockMillis())
        } catch (e: IOException) {
            logger.warn(TAG, "Could not save the time of the last full-screen ad", e)
        }
    }

    /** Saves the first time the app was opened, once. A write error is logged, never thrown. */
    suspend fun recordFirstOpen() {
        try {
            store.recordFirstOpenIfAbsent(clock.wallClockMillis())
        } catch (e: IOException) {
            logger.warn(TAG, "Could not save the first app open time", e)
        }
    }

    fun onForeground() = session.onForeground(clock.elapsedRealtimeMillis())

    fun onBackground() = session.onBackground(clock.elapsedRealtimeMillis())

    private companion object {
        const val TAG = "Ads"
    }
}
