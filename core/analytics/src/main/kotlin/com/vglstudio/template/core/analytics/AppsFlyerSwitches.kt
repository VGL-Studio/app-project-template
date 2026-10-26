package com.vglstudio.template.core.analytics

import com.vglstudio.template.core.config.RemoteKey
import com.vglstudio.template.core.config.RemoteSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// SKELETON (T-033, step 3).
@Singleton
internal class AppsFlyerSwitches(settings: RemoteSettings, scope: CoroutineScope) {
    @Inject
    constructor(settings: RemoteSettings) : this(settings, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    fun isOn(key: RemoteKey<Boolean>): Boolean = true
}
