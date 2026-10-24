package com.vglstudio.template.core.network

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Whether the device has usable Internet (INTERNET and VALIDATED, ADR-0024), for other core modules (:core:ads, S-09).
 * The same watch as the network popup: no second watcher of the system network state.
 */
interface InternetConnectivity {
    /** The current value at once, then every change. */
    val connected: Flow<Boolean>
}

internal class DefaultInternetConnectivity @Inject constructor(source: ConnectivitySource) : InternetConnectivity {
    override val connected: Flow<Boolean> = source.connectivity()
}
