package com.vglstudio.template.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** What other modules may know about the network dialog (spec network). */
interface NetworkStatus {
    /** True while the network dialog covers the screen, for example to pause a countdown. */
    val dialogShown: StateFlow<Boolean>

    /**
     * Emits once each time the dialog closes because the network is back: "Retry" found it, or the device reported
     * it on its own. A screen with a failed load can load it again.
     */
    val recovered: Flow<Unit>
}
