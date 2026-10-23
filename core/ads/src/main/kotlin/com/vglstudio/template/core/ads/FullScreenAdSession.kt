package com.vglstudio.template.core.ads

/**
 * Counts the full-screen ads shown in the current session, in memory. A session starts with the process and
 * starts again when the app comes back to foreground after at least [timeoutMillis] in background, like a
 * Firebase Analytics session. Times are milliseconds of a monotonic clock. Main thread only.
 */
internal class FullScreenAdSession(private val timeoutMillis: Long = SESSION_TIMEOUT_MILLIS) {
    private var backgroundSince: Long? = null

    var shownCount: Int = 0
        private set

    fun onBackground(now: Long) {
        backgroundSince = now
    }

    /** Starts a new session when the app was in background for at least [timeoutMillis]. */
    fun onForeground(now: Long) {
        val since = backgroundSince ?: return
        backgroundSince = null
        if (now - since >= timeoutMillis) shownCount = 0
    }

    fun onShown() {
        shownCount++
    }

    companion object {
        const val SESSION_TIMEOUT_MILLIS = 30 * 60 * 1_000L
    }
}
