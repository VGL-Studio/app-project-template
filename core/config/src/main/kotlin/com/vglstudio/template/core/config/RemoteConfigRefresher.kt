package com.vglstudio.template.core.config

import com.vglstudio.template.core.logging.AppLogger

/**
 * Fetches the latest Remote Config values from the server and activates them (ADR-0018).
 * Only the loading screen may use this to block the UI; every other screen reads what is already there (ADR-0012).
 */
interface RemoteConfigRefresher {
    /** Returns true when fresh values are active. Never throws, except for cancellation. */
    suspend fun refresh(): Boolean
}

internal class RemoteConfigRefreshPolicy(
    private val isFakeConfig: () -> Boolean,
    private val fetchAndActivate: suspend () -> Unit,
    private val logger: AppLogger,
    private val timeoutMillis: Long = REFRESH_TIMEOUT_MILLIS,
) {
    suspend fun refresh(): Boolean = TODO()

    internal companion object {
        const val REFRESH_TIMEOUT_MILLIS = 10_000L
        const val LOG_TAG = "RemoteConfig"
    }
}
