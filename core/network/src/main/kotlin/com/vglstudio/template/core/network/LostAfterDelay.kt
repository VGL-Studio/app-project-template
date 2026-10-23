package com.vglstudio.template.core.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest

/**
 * Emits once each time the source stays offline for delaySeconds(); a shorter loss emits nothing (spec network,
 * KB-2). The delay is read again at each loss, so a new remote value applies from the next loss. A delay of zero or
 * less emits at once. Being offline at the start also waits: the caller checks isConnected() itself when it needs
 * an answer at once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun ConnectivitySource.lostAfterDelay(delaySeconds: suspend () -> Long): Flow<Unit> =
    connectivity().transformLatest { connected ->
        if (!connected) {
            delay(delaySeconds().coerceAtLeast(0) * 1_000)
            emit(Unit)
        }
    }
