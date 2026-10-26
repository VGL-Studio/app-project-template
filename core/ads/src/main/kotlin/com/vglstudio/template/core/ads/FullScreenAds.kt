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
 * full-screen gate allows it: consent, placement switches, one ad at a time, a loaded ad, and the `ads_interval` mark
 * of the placement's group (docs/ops/ads-business-rules.md, sections 3 and 4). Call the action at a natural break
 * only (ADR-0013). [onFinished] runs exactly once per call: at once when nothing shows, else after the ad has
 * closed (N-02); with no ads controller (tests, previews) it runs at once.
 */
@Composable
fun rememberShowFullScreenAd(placement: AdPlacement, onFinished: () -> Unit = {}): () -> Unit {
    require(placement.format.isFullScreen) { "${placement.key} is not a full-screen placement" }
    // App-wide placements are shown by :core:ads itself, never by a feature (plan T-024, question 3).
    require(!placement.appWide) { "${placement.key} is app-wide: the loading screen shows it with StartupAds" }
    val currentOnFinished = rememberUpdatedState(onFinished)
    return rememberFullScreenAdAction(
        placement = placement,
        show = { ads, activity, shown -> ads.showFullScreenAd(activity, shown) { currentOnFinished.value() } },
        onUnavailable = { currentOnFinished.value() },
    )
}

// SKELETON (plan T-036, step 3).
@Composable
fun rememberShowAppOpenAd(placement: AdPlacement, onFinished: () -> Unit = {}): () -> Unit = {}

/**
 * The action of [rememberShowFullScreenAd]: calls [show] when an ads controller and an activity are present, else
 * [onUnavailable].
 */
@Composable
internal fun rememberFullScreenAdAction(
    placement: AdPlacement,
    show: suspend (AdsController, Activity, AdPlacement) -> Unit,
    onUnavailable: () -> Unit = {},
): () -> Unit {
    val ads = LocalAdsController.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val currentShow = rememberUpdatedState(show)
    val currentOnUnavailable = rememberUpdatedState(onUnavailable)
    return remember(ads, activity, placement) {
        {
            if (ads != null && activity != null) {
                scope.launch { currentShow.value(ads, activity, placement) }
            } else {
                currentOnUnavailable.value()
            }
        }
    }
}
