package com.vglstudio.template.core.ads

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch

/**
 * Returns an action that shows the full-screen ad of [placement] (interstitial or native full screen) if the
 * guardrails allow it: frequency rules shared by both formats (T-009), and nothing when no ad is preloaded. Call the
 * action at a natural break only (ADR-0013). It does nothing in tests and previews.
 */
@Composable
fun rememberShowFullScreenAd(placement: AdPlacement): () -> Unit {
    require(placement.format.isFullScreen) { "${placement.key} is not a full-screen placement" }
    return rememberFullScreenAdAction(placement) { ads, activity, shown -> ads.showFullScreenAd(activity, shown) }
}

/** The action of [rememberShowFullScreenAd]: calls [show] when an ads controller and an activity are present. */
@Composable
internal fun rememberFullScreenAdAction(
    placement: AdPlacement,
    show: suspend (AdsController, Activity, AdPlacement) -> Unit,
): () -> Unit {
    val ads = LocalAdsController.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val currentShow = rememberUpdatedState(show)
    return remember(ads, activity, placement) {
        { if (ads != null && activity != null) scope.launch { currentShow.value(ads, activity, placement) } }
    }
}
