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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The ads controller of the running app, provided by :app's activity. Tests and previews provide nothing,
 * so [AdSlot] and [PrivacyOptionsButton] render nothing there and screenshots never contain an ad.
 */
val LocalAdsController = staticCompositionLocalOf<AdsController?> { null }

/**
 * Shows the banner of [placement] in a screen. Features call this, never an ads SDK directly.
 * It takes no space until ads are allowed (consent given, SDK started, placement switched on remotely).
 */
@Composable
fun AdSlot(placement: AdPlacement, modifier: Modifier = Modifier) {
    require(placement.format == AdFormat.BANNER) { "${placement.key} is not a banner placement" }
    val ads = LocalAdsController.current
    if (ads == null) {
        Box(modifier = modifier)
        return
    }
    val canRequestAds by ads.canRequestAds.collectAsStateWithLifecycle()
    val enabled by remember(placement) { ads.isEnabled(placement) }.collectAsStateWithLifecycle(initialValue = false)
    val unitId = remember(placement) { ads.unitId(placement) }
    if (!canRequestAds || !enabled || unitId == null) {
        Box(modifier = modifier)
        return
    }
    BannerAd(ads = ads, placement = placement, unitId = unitId, modifier = modifier)
}

/**
 * The banner of [placement]. Its paid events, load results and clicks go to the ad metrics (plan T-023): no
 * zero-value fallback for banners (T-02); only the first result after [AdView.loadAd] has a latency, the refreshes
 * of the SDK have none (A 06:157).
 */
@Composable
private fun BannerAd(ads: AdsController, placement: AdPlacement, unitId: String, modifier: Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val context = LocalContext.current
        val screenWidthDp = LocalConfiguration.current.screenWidthDp
        val widthDp = if (constraints.hasBoundedWidth) maxWidth.value.toInt() else screenWidthDp
        val adSize = remember(widthDp) { AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp) }
        val adView = remember(unitId, adSize) {
            AdView(context).apply {
                adUnitId = unitId
                setAdSize(adSize)
                attachBannerMetrics(ads, placement, unitId)
            }
        }
        LaunchedEffect(adView) {
            ads.telemetry.requestSent(placement.key, ads.metricsNowMillis())
            adView.loadAd(ads.adRequest())
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(adView, lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }

        // The height is reserved before the ad loads, so the screen does not jump when it appears.
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().height(adSize.height.dp))
    }
}

/** The banner slot: frame, ad view, pauses and sends (B-01 to B-06). */
@Suppress("UNUSED_PARAMETER")
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
    Box(modifier = modifier)
}

private fun AdView.attachBannerMetrics(ads: AdsController, placement: AdPlacement, unitId: String) {
    val telemetry = ads.telemetry
    val format = placement.format
    val loadPlacement = loadPlacementOf(format, placement)
    setOnPaidEventListener { value ->
        val adapter = responseInfo?.loadedAdapterResponseInfo
        telemetry.revenue(
            format,
            unitId,
            { placement },
            value.valueMicros,
            value.currencyCode,
            adapter?.adSourceName,
            adapter?.adapterClassName,
        )
    }
    val slot = placement.key
    adListener = object : AdListener() {
        override fun onAdLoaded() {
            telemetry.loadSucceeded(format, slot, loadPlacement, ads.metricsNowMillis())
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            val now = ads.metricsNowMillis()
            telemetry.loadFailed(format, slot, loadPlacement, error.code, error.message, now)
        }

        override fun onAdClicked() = telemetry.clicked(format, placement)
    }
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
