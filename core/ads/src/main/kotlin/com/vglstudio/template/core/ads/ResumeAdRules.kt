package com.vglstudio.template.core.ads

// T-025 skeleton.
internal enum class ResumeDecision { FIRST_FOREGROUND, STARTUP, NOT_SERVING, SUPPRESSED, OFF, BUSY, THROTTLED, NOT_READY, SHOW }

internal data class ResumeGate(
    val firstForeground: Boolean,
    val startupFinished: Boolean,
    val serving: Boolean,
    val suppressed: Boolean,
    val placement: AdPlacement?,
    val busy: Boolean,
    val appOpenLockPassed: Boolean,
    val ready: Boolean,
)

internal fun decideResumeAd(gate: ResumeGate): ResumeDecision = ResumeDecision.SHOW

internal fun ResumeDecision.opportunityResult(): String? = null
