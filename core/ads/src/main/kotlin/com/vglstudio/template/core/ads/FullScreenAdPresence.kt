package com.vglstudio.template.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Whether a full-screen ad (interstitial or native full screen) is on screen. */
internal class FullScreenAdPresence {
    private val _showing = MutableStateFlow(false)

    val showing: StateFlow<Boolean> = _showing.asStateFlow()

    fun onShowing() = Unit

    fun onClosed() = Unit
}
