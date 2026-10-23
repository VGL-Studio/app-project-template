package com.vglstudio.template.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether a full-screen ad (interstitial or native full screen) is on screen. The network popup (ADR-0024, T-017)
 * waits for it to close; a second full-screen ad is skipped while it is true.
 */
internal class FullScreenAdPresence {
    private val _showing = MutableStateFlow(false)

    val showing: StateFlow<Boolean> = _showing.asStateFlow()

    /** A full-screen ad is about to be shown. */
    fun onShowing() {
        _showing.value = true
    }

    /** The full-screen ad was closed or failed to show. Calling it again changes nothing. */
    fun onClosed() {
        _showing.value = false
    }
}
