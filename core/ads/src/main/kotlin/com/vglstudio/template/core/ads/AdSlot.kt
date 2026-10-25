package com.vglstudio.template.core.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * The ads controller of the running app, provided by :app's activity. Tests and previews provide nothing,
 * so [AdSlot] and [PrivacyOptionsButton] render nothing there and screenshots never contain an ad.
 */
val LocalAdsController = staticCompositionLocalOf<AdsController?> { null }

/**
 * Shows the banner of [placement] in a screen. Features call this, never an ads SDK directly.
 *
 * - [AdFormat.BANNER]: a fixed banner, refreshed by the SDK at the rate of its ad unit in AdMob; the app only retries
 *   after a failure (B-02). [AdFormat.BANNER_COLLAPSIBLE]: a collapsible banner, loaded again by :core:ads 30 s after
 *   each load, each time as a new expanded banner (B-03); place it at the bottom of the screen. Any other format
 *   throws [IllegalArgumentException].
 * - It takes no space until ads are allowed (consent given, placement switched on remotely, a unit id; B-01, B-04).
 *   The height is reserved before the ad loads, so the screen does not jump when it appears.
 * - Requests are sent only while the screen shows (resumed) and the consent gate allows them right then; a retry or
 *   reload due meanwhile waits for the screen to show again (B-05, AR-03). The banner is paused while hidden.
 * - Consent withdrawn after the banner had an ad: the frame keeps its height, the ad is hidden and paused; consent
 *   given again loads a new ad in the same frame (B-06).
 */
@Composable
fun AdSlot(placement: AdPlacement, modifier: Modifier = Modifier) {
    val refresh = bannerRefreshOf(placement.format)
    val ads = LocalAdsController.current
    if (ads == null) {
        Box(modifier = modifier)
        return
    }
    val canRequestAds by ads.canRequestAds.collectAsStateWithLifecycle()
    val enabled by remember(placement) { ads.isEnabled(placement) }.collectAsStateWithLifecycle(initialValue = false)
    val unitId = remember(placement) { ads.unitId(placement) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val visible = remember(lifecycle) { lifecycle.resumedFlow() }
    BoxWithConstraints(modifier = modifier) {
        // The activity of the screen: Unity and ironSource need an Activity to load a banner.
        val context = LocalContext.current
        val screenWidthDp = LocalConfiguration.current.screenWidthDp
        val widthDp = if (constraints.hasBoundedWidth) maxWidth.value.toInt() else screenWidthDp
        val adSize = remember(widthDp) { AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp) }
        key(adSize) {
            BannerSlotHost(
                gateOpen = canRequestAds,
                enabled = enabled,
                unitId = unitId,
                heightDp = adSize.height,
                refresh = refresh,
                visible = visible,
                mayRequestNow = ads::mayRequestNow,
                retryResets = ads.retryResets,
                logger = ads.logger,
                placementKey = placement.key,
                newView = {
                    val id = checkNotNull(unitId) { "${placement.key} has no ad unit" }
                    val adView = AdView(context).apply {
                        adUnitId = id
                        setAdSize(adSize)
                    }
                    SdkBannerAdView(adView, refresh, placement, id, ads.telemetry, ads::metricsNowMillis)
                },
            )
        }
    }
}

/**
 * The banner slot: its frame ([bannerFrame]), one ad view per frame ([newView], destroyed when the frame goes),
 * pause and resume ([bannerShouldRun]) and the sends ([runBannerLoads]) while the frame is active.
 */
@Composable
internal fun BannerSlotHost(
    gateOpen: Boolean,
    enabled: Boolean,
    unitId: String?,
    heightDp: Int,
    refresh: BannerRefresh,
    visible: Flow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    logger: AppLogger,
    placementKey: String,
    newView: () -> BannerAdView,
    modifier: Modifier = Modifier,
) {
    var hadAd by remember { mutableStateOf(false) }
    val frame = bannerFrame(gateOpen, enabled, unitId, hadAd)
    if (frame == BannerFrame.NONE) {
        // The space is given back and the ad view destroyed: a later frame starts with no ad.
        LaunchedEffect(Unit) { hadAd = false }
        Box(modifier = modifier)
        return
    }
    Box(modifier = modifier) {
        // One ad view per placement, refresh kind and ad unit: two placements may share a unit (staging test ids).
        key(placementKey, refresh, unitId) {
            BannerFrameContent(
                frame = frame,
                heightDp = heightDp,
                refresh = refresh,
                visible = visible,
                mayRequestNow = mayRequestNow,
                retryResets = retryResets,
                logger = logger,
                placementKey = placementKey,
                newView = newView,
                onLoaded = { hadAd = true },
            )
        }
    }
}

@Composable
private fun BannerFrameContent(
    frame: BannerFrame,
    heightDp: Int,
    refresh: BannerRefresh,
    visible: Flow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    logger: AppLogger,
    placementKey: String,
    newView: () -> BannerAdView,
    onLoaded: () -> Unit,
) {
    val view = remember { newView() }
    DisposableEffect(view) {
        onDispose { view.destroy() }
    }
    val currentFrame by rememberUpdatedState(frame)
    LaunchedEffect(view, visible) {
        // Paused when it should not run; resumed only after a pause, so a view built running gets neither call.
        var running: Boolean? = null
        combine(visible, snapshotFlow { currentFrame }) { shows, current -> bannerShouldRun(shows, current) }
            .distinctUntilChanged()
            .collect { run ->
                when {
                    !run -> view.pause()
                    running != null -> view.resume()
                }
                running = run
            }
    }
    val active = frame == BannerFrame.ACTIVE
    val currentMayRequestNow by rememberUpdatedState(mayRequestNow)
    val currentOnLoaded by rememberUpdatedState(onLoaded)
    LaunchedEffect(view, active) {
        if (active) {
            runBannerLoads(
                refresh = refresh,
                view = view,
                visible = visible,
                mayRequestNow = { currentMayRequestNow() },
                retryResets = retryResets,
                onLoaded = { currentOnLoaded() },
                logger = logger,
                placementKey = placementKey,
            )
        }
    }
    view.Content(showAd = active, modifier = Modifier.fillMaxWidth().height(heightDp.dp))
}

/**
 * Returns an action that shows the full-screen ad of [placement] if the guardrails allow it (ADR-0004).
 * Call the action at a natural break only. It does nothing in tests and previews.
 * New code uses [rememberShowFullScreenAd], which shows both interstitial and native full-screen placements.
 */
@Composable
fun rememberShowInterstitial(placement: AdPlacement): () -> Unit {
    require(placement.format == AdFormat.INTERSTITIAL) { "${placement.key} is not a full-screen placement" }
    val ads = LocalAdsController.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    return remember(ads, activity, placement) {
        { if (ads != null && activity != null) scope.launch { ads.showInterstitial(activity, placement) } }
    }
}

/**
 * Entry point to change consent, required by Google where the consent form applies.
 * Place it in the app's settings or main screen. It renders nothing where it is not required.
 */
@Composable
fun PrivacyOptionsButton(modifier: Modifier = Modifier) {
    val ads = LocalAdsController.current ?: return
    val activity = LocalActivity.current ?: return
    val required by ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    if (required) {
        TextButton(onClick = { ads.showPrivacyOptionsForm(activity) }, modifier = modifier) {
            Text(text = stringResource(R.string.ads_privacy_options))
        }
    }
}
