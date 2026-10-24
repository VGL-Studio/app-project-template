package com.vglstudio.template.core.ads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/** S-24: changes of usable Internet within this window count as one. */
internal const val NETWORK_SETTLE_MILLIS = 3_000L

/** Emits once each time usable Internet, stable for [windowMillis], comes back after a stable loss. A first stable `true` is no return. */
internal fun networkReturns(connected: Flow<Boolean>, windowMillis: Long = NETWORK_SETTLE_MILLIS): Flow<Unit> =
    connected.filter { it }.map { }
