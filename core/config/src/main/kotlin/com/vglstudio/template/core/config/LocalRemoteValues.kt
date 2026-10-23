package com.vglstudio.template.core.config

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalRemoteValues

class LocalRemoteValue private constructor(val key: RemoteKey<*>, val value: Any) {
    companion object {
        fun <T : Any> of(key: RemoteKey<T>, value: T): LocalRemoteValue = LocalRemoteValue(key, value)
    }
}
