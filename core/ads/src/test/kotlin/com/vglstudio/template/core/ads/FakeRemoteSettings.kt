package com.vglstudio.template.core.ads

import com.vglstudio.template.core.config.RemoteKey
import com.vglstudio.template.core.config.RemoteSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * [RemoteSettings] for unit tests, following the T-007 contract: a value set by key name, else the key's default.
 * Like the real one, the flow emits the current value at once, then again on every change of any key.
 */
internal class FakeRemoteSettings : RemoteSettings {
    private val values = MutableStateFlow<Map<String, Any>>(emptyMap())

    fun <T : Any> set(key: RemoteKey<T>, value: T) {
        values.update { it + (key.name to value) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> value(key: RemoteKey<T>): Flow<T> = values.map { (it[key.name] as T?) ?: key.default }
}
