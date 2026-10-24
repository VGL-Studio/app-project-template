package com.vglstudio.template.core.ads

import android.app.Activity
import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Runs the consent rounds of :core:ads over [ConsentPlatform] and [ConsentGate]: gather, regather, privacy form,
 * reset (S-01 to S-19). Main thread only.
 */
@Suppress("UNUSED_PARAMETER", "unused")
internal class ConsentCoordinator(
    private val platform: ConsentPlatform,
    private val gate: ConsentGate,
    private val suppressor: ForegroundAdSuppressor,
    private val isActivityGone: (Activity) -> Boolean,
    private val isResumed: (Activity) -> Boolean,
    private val fullScreenShowing: () -> Boolean,
    private val allowReset: Boolean,
    private val logger: AppLogger,
    private val onGateOpened: (firstTime: Boolean) -> Unit,
) {
    private val _finished = MutableStateFlow(false)

    /** The round of this start has its final result, or the result was settled before. */
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)

    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /** S-01: false when already started. */
    fun start(): Boolean = false

    fun onShortcutChanged(active: Boolean) {}

    fun gather(activity: Activity) {}

    fun onForegroundChanged(foreground: Boolean) {}

    fun onInternetChanged(connected: Boolean) {}

    fun onPreloadRequested() {}

    fun onActivityResumed(activity: Activity) {}

    fun showPrivacyOptions(activity: Activity) {}

    fun reset() {}

    fun mayRequestNow(foreground: Boolean): Boolean = false
}
