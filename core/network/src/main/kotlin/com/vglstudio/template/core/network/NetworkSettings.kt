package com.vglstudio.template.core.network

import com.vglstudio.template.core.config.RemoteKey

/** Remote Config keys of the network module (ADR-0023), listed by :app in AppRemoteKeys. */
object NetworkSettings {
    /** How long usable Internet must be missing before the network dialog shows (spec network, section 8). */
    val LOST_DELAY_SECONDS: RemoteKey<Long> = RemoteKey.long(
        name = "network_lost_delay_seconds",
        default = 2,
        description = "Seconds without usable Internet before the network dialog shows (ADR-0024).",
    )

    fun all(): List<RemoteKey<*>> = listOf(LOST_DELAY_SECONDS)

    // Skeleton for the tests written first (T-017, step 3); step 4 sets the real values.
    internal const val RETRY_MAX_MILLIS = 0L
    internal const val RETRY_POLL_MILLIS = 0L
}
