package com.vglstudio.template.core.config

import kotlinx.coroutines.flow.Flow

/**
 * Boolean values fetched remotely (Firebase Remote Config in the app, a fake in tests).
 * [boolean] emits the value activated now, then every new value once it is activated.
 * It emits `null` while the key has no remote value, or when the remote value is not a boolean.
 */
internal interface RemoteFlagSource {
    fun boolean(key: String): Flow<Boolean?>
}
