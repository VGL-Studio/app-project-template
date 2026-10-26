package com.vglstudio.template.core.ads

import android.app.Activity
import com.vglstudio.template.core.logging.AppLogger

/** What one caller of a rewarded turn gets (R-02 to R-04). */
internal sealed interface RewardedOutcome {
    data object OnScreen : RewardedOutcome

    data object Granted : RewardedOutcome

    data class NoAd(val network: Boolean, val granted: Boolean) : RewardedOutcome
}

/** What [RewardedTurns] asks [AdsController] for. Main thread only. */
internal interface RewardedPort {
    suspend fun enabled(declaration: AdPlacementDeclaration): Boolean
    fun consentOpen(): Boolean
    fun mayRequestNow(): Boolean
    fun offline(): Boolean
    fun ready(declaration: AdPlacementDeclaration): Boolean
    fun needsLoad(declaration: AdPlacementDeclaration): Boolean
    fun loadState(declaration: AdPlacementDeclaration): String
    suspend fun loadIfNeeded(declaration: AdPlacementDeclaration)
    fun clearLoading(declaration: AdPlacementDeclaration)
    fun send(declaration: AdPlacementDeclaration)
    fun sendBlocked(declaration: AdPlacementDeclaration)
    fun take(declaration: AdPlacementDeclaration): Boolean
    fun show(declaration: AdPlacementDeclaration, activity: Activity?)
    fun showAgain(declaration: AdPlacementDeclaration, activity: Activity?)
    fun raisePresence(declaration: AdPlacementDeclaration, activity: Activity?): Boolean
    fun lowerPresence()
    fun recordOpportunity(declaration: AdPlacementDeclaration, result: String, loadState: String)
    fun recordReward(declaration: AdPlacementDeclaration)
}

/** The rewarded turns and load rounds (skeleton of T-031). */
internal class RewardedTurns(private val port: RewardedPort, private val logger: AppLogger) {
    suspend fun loadIfNeeded(declaration: AdPlacementDeclaration) {}

    fun onLoadFailed(declaration: AdPlacementDeclaration, code: Int, offline: Boolean) {}

    suspend fun request(
        declaration: AdPlacementDeclaration,
        activity: Activity?,
        onOutcome: (RewardedOutcome) -> Unit,
    ) {}

    fun onShowed(declaration: AdPlacementDeclaration) {}

    fun onReward(declaration: AdPlacementDeclaration) {}

    fun onDismissed(declaration: AdPlacementDeclaration) {}

    fun onShowFailed(declaration: AdPlacementDeclaration, code: Int) {}

    fun onStuck() {}
}
