package com.vglstudio.template.core.network

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Whether the device has usable Internet (ADR-0024), for other core modules (:core:ads, S-09). */
interface InternetConnectivity {
    /** The current value at once, then every change. */
    val connected: Flow<Boolean>
}

@Suppress("UNUSED_PARAMETER", "unused")
internal class DefaultInternetConnectivity @Inject constructor(private val source: ConnectivitySource) :
    InternetConnectivity {
    override val connected: Flow<Boolean> = flowOf(false)
}
