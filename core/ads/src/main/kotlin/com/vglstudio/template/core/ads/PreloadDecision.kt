package com.vglstudio.template.core.ads

/** Why a preload of the plan is asked for. */
internal enum class PreloadReason { SDK_READY, GATE_REOPENED, NETWORK_BACK, PLACEMENT_ENABLED, NATIVE_REFILL }

/** What happens to one asked preload (S-21, S-23). */
internal enum class PreloadDecision {
    RUN,
    HOLD,
    IGNORE_SDK_NOT_STARTED,
    IGNORE_CONSENT_DENIED,
    IGNORE_NEVER_REQUESTED,
    IGNORE_RUNNING,
}

/** The state of the consent gate and the SDK that a preload reads. */
internal data class PreloadGate(
    /** MobileAds.initialize was called (onGateOpened(firstTime = true)). */
    val sdkStarted: Boolean,
    /** S-21. */
    val sdkReady: Boolean,
    /** mayRequestNow() of T-020: consent gate, and foreground for ERROR_ALLOWED. */
    val mayRequest: Boolean,
    /** consentPath == DENIED_OR_REQUIRED. */
    val consentDenied: Boolean,
)

/** S-21, S-23, in this order: SDK not started, consent denied, never requested, running, then HOLD or RUN. */
internal fun decidePreload(
    reason: PreloadReason,
    gate: PreloadGate,
    everRequested: Boolean,
    running: Boolean,
): PreloadDecision = when {
    !gate.sdkStarted -> PreloadDecision.IGNORE_SDK_NOT_STARTED
    gate.consentDenied -> PreloadDecision.IGNORE_CONSENT_DENIED
    !everRequested && reason != PreloadReason.SDK_READY -> PreloadDecision.IGNORE_NEVER_REQUESTED
    running -> PreloadDecision.IGNORE_RUNNING
    !gate.sdkReady || !gate.mayRequest -> PreloadDecision.HOLD
    else -> PreloadDecision.RUN
}

/** S-21: the longest wait for the init callback of the SDK. */
internal const val SDK_READY_CAP_MILLIS = 35_000L

/** S-21: ready at the init callback, or 35 s after init started; never before init started (S-19). */
internal fun sdkReadyAt(initStartedAt: Long?, callbackArrived: Boolean, now: Long): Boolean =
    initStartedAt != null && (callbackArrived || now - initStartedAt >= SDK_READY_CAP_MILLIS)
