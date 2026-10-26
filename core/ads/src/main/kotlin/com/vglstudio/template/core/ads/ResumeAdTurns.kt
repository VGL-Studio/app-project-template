package com.vglstudio.template.core.ads

import android.app.Activity
import com.vglstudio.template.core.logging.AppLogger

// T-025 skeleton.
internal interface ResumeAdPort {
    fun readGate(): ResumeGate
    fun loadStateOf(placement: AdPlacement): String
    fun recordOpportunity(placement: AdPlacement, result: String, loadState: String)
    fun loadIfNeeded(placement: AdPlacement)
    fun placementNow(): AdPlacement?
    fun suppressedNow(): Boolean
    fun showAppOpen(activity: Activity, placement: AdPlacement)
    fun showInterstitial(activity: Activity, placement: AdPlacement, combo: Boolean)
}

internal class ResumeAdTurns(private val port: ResumeAdPort, private val logger: AppLogger) {
    fun onProcessForegroundChanged(foreground: Boolean) {}

    fun onAppActivityResumed(activity: Activity) {}
}
