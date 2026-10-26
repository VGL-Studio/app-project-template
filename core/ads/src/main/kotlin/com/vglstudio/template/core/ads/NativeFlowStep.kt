package com.vglstudio.template.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

private const val TAG = "Ads"

internal object NativeFlowStepTestTags {
    /** The root of [NativeFullScreenStepGate]: the area the app gives the page. */
    const val FULL_PAGE = "nativeFlowStepFullPage"
}

/**
 * The native card of a flow step (MC-06, OB-01, OB-02; plan T-032): 250 dp, from the pool of its placement
 * (`native_step`, P-02), drawn outside the pager. Pass the key of the step on screen as [step] (for example
 * `pagerState.settledPage`), and call it only on the steps the app's spec chose. Each time [step] changes the card
 * takes another ad of the pool, and gives the old one back only once it has the new one: the pool never hands out an
 * ad in use (P-01), so two steps next to each other never show the same ad (OB-02). Until the new ad comes it shows
 * the 250 dp placeholder (for about one frame when the pool has an ad ready; the height stays, so the Next button
 * does not move), or nothing while the pool is exhausted (P-06), and keeps asking. Like [NativeAdSlot], it takes no
 * space until ads are allowed (consent given, `is_show_<placement>` on, an ad unit), and gives its ad back when it
 * leaves the screen or the placement turns off (L-061). [onAdAvailabilityChange] works as for [NativeAdSlot]. It
 * records no `ad_opportunity` (P-07). Placements of another format are an [IllegalArgumentException].
 */
@Composable
fun NativeStepAd(
    placement: AdPlacement,
    step: Any,
    modifier: Modifier = Modifier,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
) {
    requireStepFormat(placement.format)
    val ads = LocalAdsController.current
    if (ads == null) {
        Box(modifier = modifier)
        return
    }
    key(placement) {
        NativeStepAdHost(
            allowed = rememberStepAllowed(placement, ads.canRequestAds, ads::isEnabled) { ads.unitId(it) },
            step = step,
            newStates = { steps -> ads.stepAd(placement, steps) },
            modifier = modifier,
            onAdAvailabilityChange = onAdAvailabilityChange,
        )
    }
}

/**
 * [NativeStepAd] without an ads controller: [step] goes into one stream of steps, read by the states [newStates]
 * builds once ([stepAdStates] in the app); drawn by the gate of every native slot at the size of the step card.
 */
@Composable
internal fun NativeStepAdHost(
    allowed: Boolean,
    step: Any,
    newStates: (steps: Flow<Any>) -> Flow<NativeAdState>,
    modifier: Modifier = Modifier,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
    adContent: @Composable (LoadedNativeAd) -> Unit = { BoundNativeAd(it, NativeAdSize.STEP) },
) {
    val steps = remember { MutableStateFlow(step) }
    SideEffect { steps.value = step }
    // A cold flow: nothing is claimed until the gate opens and the host collects it.
    val states = remember { newStates(steps) }
    NativeAdSlotGate(
        allowed = allowed,
        states = states,
        size = NativeAdSize.STEP,
        keepSpaceWhenEmpty = false,
        onAdAvailabilityChange = onAdAvailabilityChange,
        modifier = modifier,
        adContent = adContent,
    )
}

/**
 * The states of the step card on [pool] (OB-02, P-06, P-01a): Loading at each new step of [steps], then the ad
 * claimed for [holder]. The ad of the step before stays claimed until the new one comes, then goes back to the pool,
 * so no claim of the new step can get it. With no ad, asks again every 500 ms, or every 2 s while the pool is
 * exhausted, when it shows Unavailable (once per step). A step that changes while the card waits drops that wait and
 * keeps the old ad held. When the collection ends, every ad held goes back to the pool. Logs never hold the step key
 * (it may be data of the app).
 */
internal fun stepAdStates(
    pool: PooledNativeAds,
    holder: AdPlacement,
    steps: Flow<Any>,
    logger: AppLogger,
    placementKey: String,
): Flow<NativeAdState> = channelFlow {
    // The ad of the step on screen, and the ad of the step before, still held until the new one comes.
    var current: LoadedNativeAd? = null
    var previous: LoadedNativeAd? = null
    var firstStep = true
    try {
        steps.distinctUntilChanged().collectLatest {
            if (!firstStep) logger.info(TAG, "Step $placementKey: step changed")
            firstStep = false
            current?.let { shown ->
                // An older ad still held here was replaced before it went back: it is not on screen any more.
                previous?.let(pool::release)
                previous = shown
                current = null
            }
            send(NativeAdState.Loading)
            var unavailableShown = false
            while (true) {
                val ad = pool.claim(holder)
                if (ad != null) {
                    current = ad
                    logger.info(TAG, "Step $placementKey: got a new ad")
                    send(NativeAdState.Loaded(ad))
                    previous?.let(pool::release)
                    previous = null
                    break
                }
                if (pool.exhausted) {
                    if (!unavailableShown) {
                        unavailableShown = true
                        logger.info(TAG, "Step $placementKey: no ad (pool exhausted)")
                        send(NativeAdState.Unavailable)
                    }
                    delay(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS)
                } else {
                    delay(PoolCellPolicy.RETRY_MILLIS)
                }
            }
        }
    } finally {
        current?.let(pool::release)
        previous?.let(pool::release)
    }
}.buffer(Channel.RENDEZVOUS)

/**
 * The native page of a flow step (MC-06; plan T-032): the preloaded ad of the `native_fullscreen` slot of [placement],
 * drawn in [modifier], the area of the step the app gives it, with no close button and no countdown; the app keeps its
 * Next button outside that area, where it is on the other steps. Pass [shown] = `pagerState.settledPage == index`:
 * the page takes an ad from the slot only while it is the page on screen, and gives it up (the ad is destroyed and
 * the slot loads again) once it is not; a page built next to the one on screen takes nothing. While it shows its ad,
 * the ads that show by themselves are blocked (G-03, way 2). With no ad it draws nothing, reports false, and shows an
 * ad that comes later. It takes no ad until ads are allowed (consent given, `is_show_<placement>` on, an ad unit), and
 * gives it up when the placement turns off (L-061). It does not go through the gate of full-screen ads: no
 * `ads_interval`, no mark, no `ad_opportunity` (plan T-032, question 4). Build the list of steps with
 * [rememberNativeFullScreenStepReady], read once, so the page is there only when the slot has an ad. Placements of
 * another format are an [IllegalArgumentException].
 */
@Composable
fun NativeFullScreenStep(
    placement: AdPlacement,
    modifier: Modifier = Modifier,
    shown: Boolean = true,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
) {
    requireFullScreenStepFormat(placement.format)
    val ads = LocalAdsController.current
    if (ads == null) {
        Box(modifier = modifier)
        return
    }
    key(placement) {
        NativeFullScreenStepGate(
            allowed = rememberStepAllowed(placement, ads.canRequestAds, ads::isEnabled) { ads.unitId(it) },
            // A cold flow: nothing is taken until the gate opens and the page collects it.
            states = remember { ads.fullScreenStep(placement) },
            modifier = modifier,
            shown = shown,
            onAdAvailabilityChange = onAdAvailabilityChange,
        )
    }
}

/**
 * [NativeFullScreenStep] without an ads controller: collects [states] only while [allowed] and [shown]; draws the ad
 * with no controls and blocks the ads that show by themselves while it has one; draws nothing otherwise. Reports
 * availability like [NativeAdSlotGate]: true on an ad, false on no ad, and false when [allowed] turns false after true
 * was reported (L-061). A page that is only not [shown] reports nothing.
 */
@Composable
internal fun NativeFullScreenStepGate(
    allowed: Boolean,
    states: Flow<NativeAdState>,
    modifier: Modifier = Modifier,
    shown: Boolean = true,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
    adContent: @Composable (LoadedNativeAd) -> Unit = { FlowStepFullPage(it) },
) {
    // `af_native_ad_eligible` once each time the page comes on screen allowed (plan T-033).
    ReportNativeEligible(allowed && shown)
    val currentOnChange by rememberUpdatedState(onAdAvailabilityChange)
    val reported = remember { StepAvailability() }
    Box(modifier = modifier.testTag(NativeFlowStepTestTags.FULL_PAGE)) {
        if (!allowed) {
            SideEffect {
                if (reported.available) {
                    reported.available = false
                    currentOnChange(false)
                }
            }
        } else if (shown) {
            FullPageOnScreen(states, adContent) { available ->
                reported.available = available
                currentOnChange(available)
            }
        }
    }
}

/** The page while it is allowed and on screen: leaving the composition cancels the collection, which frees the ad. */
@Composable
private fun FullPageOnScreen(
    states: Flow<NativeAdState>,
    adContent: @Composable (LoadedNativeAd) -> Unit,
    onAvailability: (Boolean) -> Unit,
) {
    val state by states.collectAsState(initial = NativeAdState.Loading)
    val currentOnAvailability by rememberUpdatedState(onAvailability)
    // The value of this composition, not the delegate: the effect may run after a newer state arrived.
    val drawn = state
    LaunchedEffect(drawn) {
        when (drawn) {
            NativeAdState.Loading -> Unit
            is NativeAdState.Loaded -> currentOnAvailability(true)
            NativeAdState.Unavailable -> currentOnAvailability(false)
        }
    }
    if (drawn is NativeAdState.Loaded) {
        SuppressResumeAds(active = true)
        key(drawn.ad) { adContent(drawn.ad) }
    }
}

/** The last availability reported by [NativeFullScreenStepGate]; not state, nothing is drawn from it. */
private class StepAvailability {
    var available = false
}

/** A loaded ad as the page of a flow step: the full-screen layout with no controls, the SDK's media view in it. */
@Composable
private fun FlowStepFullPage(ad: LoadedNativeAd) {
    BoundNativeAd(ad, Modifier.fillMaxSize()) { binder ->
        NativeFullScreenAdLayout(
            assets = ad.assets,
            onClose = null,
            binder = binder,
            // The SDK draws AdChoices in the top right corner itself; the adChoices slot stays empty.
            media = { binder.Media(Modifier.fillMaxSize()) },
        )
    }
}

/**
 * The states of the page of a flow step: the ad [take] gives, held until the collection ends, then given to
 * [release]. With no ad, asks for a load with [ensureLoading], shows Unavailable once, and takes again every 500 ms,
 * so an ad that comes while the page is on screen is shown (P-06 pace).
 */
internal fun fullScreenStepStates(
    take: () -> LoadedNativeAd?,
    ensureLoading: () -> Unit,
    release: (LoadedNativeAd) -> Unit,
): Flow<NativeAdState> = flow {
    var unavailableShown = false
    while (true) {
        val ad = take()
        if (ad != null) {
            try {
                emit(NativeAdState.Loaded(ad))
                awaitCancellation()
            } finally {
                release(ad)
            }
        }
        ensureLoading()
        if (!unavailableShown) {
            unavailableShown = true
            emit(NativeAdState.Unavailable)
        }
        delay(PoolCellPolicy.RETRY_MILLIS)
    }
}

/** S-11: the page takes the preloaded ad out of the slot only while consent allows ads. */
internal fun stepFullScreenTake(canRequestAds: Boolean, take: () -> LoadedNativeAd?): LoadedNativeAd? =
    if (canRequestAds) take() else null

/**
 * Whether the page of a flow step of [placement] would have an ad now: ads allowed and an ad preloaded in the slot,
 * read again every 500 ms; false with no ads controller, and as soon as ads stop being allowed (L-061). Read it once
 * when building the list of steps, so the page is there only with an ad and the number of pages does not change while
 * the flow runs. Placements of another format are an [IllegalArgumentException].
 */
@Composable
fun rememberNativeFullScreenStepReady(placement: AdPlacement): Boolean {
    requireFullScreenStepFormat(placement.format)
    val ads = LocalAdsController.current ?: return false
    val allowed = rememberStepAllowed(placement, ads.canRequestAds, ads::isEnabled) { ads.unitId(it) }
    return rememberStepReady(allowed) { ads.stepFullScreenReady(placement) }
}

/** [rememberNativeFullScreenStepReady] without an ads controller: [ready] read every 500 ms while [allowed]. */
@Composable
internal fun rememberStepReady(allowed: Boolean, ready: () -> Boolean): Boolean {
    val currentReady by rememberUpdatedState(ready)
    val polled by produceState(initialValue = false, allowed) {
        if (!allowed) {
            value = false
            return@produceState
        }
        while (true) {
            value = currentReady()
            delay(PoolCellPolicy.RETRY_MILLIS)
        }
    }
    return allowed && polled
}

/**
 * The gate of the ads of a flow step (KB-1, MC-03): consent, the switch of [placement] (`is_show_<placement>`, no
 * switch of its own for the flow) and an ad unit; [enabled] and [unitId] are asked for [placement] only.
 */
@Composable
internal fun rememberStepAllowed(
    placement: AdPlacement,
    canRequestAds: StateFlow<Boolean>,
    enabled: (AdPlacement) -> Flow<Boolean>,
    unitId: (AdPlacement) -> String?,
): Boolean {
    val canRequest by canRequestAds.collectAsStateWithLifecycle()
    val on by remember(placement) { enabled(placement) }.collectAsStateWithLifecycle(initialValue = false)
    val unit = remember(placement) { unitId(placement) }
    return nativeSlotAllowed(canRequest, on, unit)
}

/** The step card draws `native_step` placements only. */
internal fun requireStepFormat(format: AdFormat) {
    require(format == AdFormat.NATIVE_STEP) { "$format is not drawn by NativeStepAd" }
}

/** The page of a flow step draws `native_fullscreen` placements only. */
internal fun requireFullScreenStepFormat(format: AdFormat) {
    require(format == AdFormat.NATIVE_FULL_SCREEN) { "$format is not drawn by NativeFullScreenStep" }
}

/**
 * The page of a flow step with fixed content, for previews, tests and the gallery screen: the layout of
 * [NativeFullScreenStep] with no controls, in [modifier]. Real ads go through [NativeFullScreenStep].
 */
@Composable
fun NativeFullScreenStepContent(
    assets: NativeAdAssets,
    modifier: Modifier = Modifier,
    media: @Composable BoxScope.() -> Unit = {},
    adChoices: @Composable () -> Unit = {},
) {
    NativeFullScreenAdLayout(assets, onClose = null, PreviewNativeAdBinder, modifier, media, adChoices)
}
