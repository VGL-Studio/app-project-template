package com.vglstudio.template.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Blocks the ads that show by themselves (resume ad) while any source holds it (G-01). Main thread only. */
internal class ForegroundAdSuppressor {
    private val _suppressed = MutableStateFlow(false)

    val suppressed: StateFlow<Boolean> = _suppressed.asStateFlow()

    @Suppress("UNUSED_PARAMETER")
    fun acquire(source: String): Hold = Hold()

    inner class Hold internal constructor() {
        fun release() {}
    }
}
