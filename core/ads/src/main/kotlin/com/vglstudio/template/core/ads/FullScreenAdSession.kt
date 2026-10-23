package com.vglstudio.template.core.ads

// Stub for the test-first commit.
internal class FullScreenAdSession(private val timeoutMillis: Long = SESSION_TIMEOUT_MILLIS) {
    val shownCount: Int get() = 0

    fun onBackground(now: Long) = Unit

    fun onForeground(now: Long) = Unit

    fun onShown() = Unit

    companion object {
        const val SESSION_TIMEOUT_MILLIS = 30 * 60 * 1_000L
    }
}
