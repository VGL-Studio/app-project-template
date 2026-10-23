package com.vglstudio.template.core.network

import com.vglstudio.template.core.config.RemoteKey
import com.vglstudio.template.core.config.RemoteSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Network state held in memory; [collectors] tells whether the system callback would be registered. */
internal class FakeConnectivitySource(connected: Boolean = true) : ConnectivitySource {
    val connected = MutableStateFlow(connected)

    val collectors: Int get() = connected.subscriptionCount.value

    override fun isConnected(): Boolean = connected.value

    override fun connectivity(): Flow<Boolean> = connected
}

/** A key missing from [values] has no remote value, so its default is used. */
internal class FakeRemoteSettings : RemoteSettings {
    val values = MutableStateFlow<Map<String, Any>>(emptyMap())

    // Safe cast: tests put a value of the key's type under its name.
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> value(key: RemoteKey<T>): Flow<T> = values.map { (it[key.name] as T?) ?: key.default }
}
