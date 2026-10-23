package com.vglstudio.template.core.network

import com.vglstudio.template.core.analytics.Analytics
import com.vglstudio.template.core.analytics.AnalyticsEvent
import com.vglstudio.template.core.config.RemoteKey
import com.vglstudio.template.core.config.RemoteSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Network state held in memory; [collectors] tells whether the system callback would be registered. */
internal class FakeConnectivitySource(connected: Boolean = true) : ConnectivitySource {
    val connected = MutableStateFlow(connected)

    val collectors: Int get() = connected.subscriptionCount.value

    /** How many times [isConnected] was read. */
    var isConnectedCalls = 0
        private set

    override fun isConnected(): Boolean {
        isConnectedCalls++
        return connected.value
    }

    override fun connectivity(): Flow<Boolean> = connected
}

/** A key missing from [values] has no remote value, so its default is used. */
internal class FakeRemoteSettings : RemoteSettings {
    val values = MutableStateFlow<Map<String, Any>>(emptyMap())

    // Safe cast: tests put a value of the key's type under its name.
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> value(key: RemoteKey<T>): Flow<T> = values.map { (it[key.name] as T?) ?: key.default }
}

/** Keeps logged events in memory, in order. */
internal class FakeAnalytics : Analytics {
    private val logged = mutableListOf<AnalyticsEvent>()

    val events: List<AnalyticsEvent> get() = synchronized(logged) { logged.toList() }

    override fun log(event: AnalyticsEvent) {
        synchronized(logged) { logged += event }
    }
}

/** A clock that reads [now], in tests the virtual time of the test scheduler. */
internal class FakeNetworkClock(private val now: () -> Long) : NetworkClock {
    override fun elapsedRealtimeMillis(): Long = now()
}
