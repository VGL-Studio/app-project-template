package com.vglstudio.template.core.config

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Business settings tuned remotely without a new release (ADR-0003), such as ad frequency.
 * Not feature flags: keys are declared as [RemoteKey] by the core module that uses them (ADR-0023).
 * Feature modules do not use this.
 * The flow emits the current value at once ([RemoteKey.default] until a remote value of the key's type is
 * activated), then every change.
 */
interface RemoteSettings {
    fun <T : Any> value(key: RemoteKey<T>): Flow<T>
}

internal class DefaultRemoteSettings @Inject constructor(
    private val remote: RemoteFlagSource,
    @LocalRemoteValues localValues: Set<@JvmSuppressWildcards LocalRemoteValue>,
) : RemoteSettings {
    override fun <T : Any> value(key: RemoteKey<T>): Flow<T> {
        val values: Flow<Any?> = when (key.type) {
            RemoteKey.Type.BOOLEAN -> remote.boolean(key.name)
            RemoteKey.Type.LONG -> remote.long(key.name)
            RemoteKey.Type.DOUBLE -> remote.double(key.name)
            RemoteKey.Type.STRING -> remote.string(key.name)
        }
        // Safe: the factories of RemoteKey tie each Type to one T (BOOLEAN to Boolean, LONG to Long, and so on),
        // and the source function above is chosen by that Type.
        @Suppress("UNCHECKED_CAST")
        return values.map { (it as T?) ?: key.default }
    }
}
