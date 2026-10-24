package com.vglstudio.template.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How ads were allowed (or not) in this process (S-13). Public: the loading screen (T-024) and the gallery read it. */
enum class ConsentPath { CACHED_EARLY, GRANTED, DENIED_OR_REQUIRED, ERROR_ALLOWED, INTERRUPTED_REGATHER }

internal enum class UmpFailure { NETWORK, CONFIGURATION, INTERRUPTED }

/** S-04, S-05: INTERNET_ERROR (2), TIME_OUT (4) are NETWORK; a form error while the activity is gone is INTERRUPTED first. */
@Suppress("UNUSED_PARAMETER")
internal fun umpFailureOf(errorCode: Int, activityGone: Boolean): UmpFailure = UmpFailure.CONFIGURATION

/** S-12: both the build switch of :app and is_enable_consent_shortcut. */
@Suppress("UNUSED_PARAMETER")
internal fun shortcutActive(enabledInBuild: Boolean, remoteEnabled: Boolean): Boolean = false

internal data class ConsentGateState(
    /** Null before the first result (S-19). */
    val path: ConsentPath?,
    /** The load and show gate (S-10, S-11, S-15). */
    val allowed: Boolean,
    /** Allowed only by CACHED_EARLY or ERROR_ALLOWED. */
    val temporary: Boolean,
    /** S-06. */
    val settled: Boolean,
)

/** The consent rules of docs/ops/ads-business-rules.md (S-03 to S-19), with no Android or UMP call. Main thread only. */
@Suppress("UNUSED_PARAMETER")
internal class ConsentGate {
    private val _state =
        MutableStateFlow(ConsentGateState(path = null, allowed = false, temporary = false, settled = false))

    val state: StateFlow<ConsentGateState> = _state.asStateFlow()

    fun onShortcutChanged(active: Boolean) {}

    fun onRequestSent(umpCanRequestAds: Boolean) {}

    fun onUpdateSucceeded(umpCanRequestAds: Boolean) {}

    fun onUpdateFailed(failure: UmpFailure, umpCanRequestAds: Boolean) {}

    fun onFormFinished(failure: UmpFailure?, umpCanRequestAds: Boolean) {}

    fun onPrivacyFormClosed(umpCanRequestAds: Boolean) {}

    fun onReset() {}

    /** S-08, S-15: may a request be sent right now. */
    fun mayRequestNow(foreground: Boolean): Boolean = false

    /** S-09: a regather is due (ERROR_ALLOWED and not settled). */
    val regatherDue: Boolean get() = false
}
