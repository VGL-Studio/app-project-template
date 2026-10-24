package com.vglstudio.template.core.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MediaAspectRatio
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.vglstudio.template.core.logging.AppLogger
import com.vglstudio.template.core.network.InternetConnectivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The only class that talks to the ads SDKs (ADR-0004). :app calls [start] once in `Application.onCreate` (HD-01),
 * [gatherConsent], [onUserInteraction], [onAppForeground] and [onAppBackground] from its activity, provides this
 * controller to the UI through [LocalAdsController], and draws native full-screen ads with [FullScreenAdHost]. Features
 * use [AdSlot], [NativeAdSlot], [rememberShowFullScreenAd] and [PrivacyOptionsButton], never this class or an SDK
 * directly.
 *
 * Consent follows the PARTNER rules S-01 to S-19 ([ConsentCoordinator], [ConsentGate]): the ads SDK is initialized the
 * first time the consent gate opens, and every request asks the gate right before it is sent ([mayRequestNow]).
 * Mediation adapters (AppLovin, plan T-015) are initialized by the SDK at the same time, with no code of their own;
 * their status is logged.
 */
@Singleton
class AdsController @Inject internal constructor(
    @param:ApplicationContext private val context: Context,
    private val unitIds: AdUnitIds,
    consentDebug: ConsentDebugOptions,
    private val flags: AdsFlags,
    private val connectivity: InternetConnectivity,
    private val shortcutConfig: ConsentShortcutConfig,
    internal val logger: AppLogger,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Touched on the main thread only.
    private val guard = FullScreenAdGuard()
    private val startedActivities = StartedActivityCounter()
    private val loadedInterstitials = mutableMapOf<AdPlacement, InterstitialAd>()
    private var fullScreenInProgress = false
    private val presence = FullScreenAdPresence()
    private val preloadedNativeFullScreen = FreshAdCache<LoadedNativeAd>(onDiscard = { it.destroy() })
    private val nativeFullScreenLoading = mutableSetOf<AdPlacement>()

    private val gate = ConsentGate()
    private var tracker: AdsActivityTracker? = null
    private val sdkStarted = MutableStateFlow(false)

    /** Blocks the ads that show by themselves while a consent form is open (S-16, G-01); read by T-022, T-025. */
    internal val foregroundAdSuppressor = ForegroundAdSuppressor()

    private val coordinator = ConsentCoordinator(
        platform = UmpConsentPlatform(context, consentDebug),
        gate = gate,
        suppressor = foregroundAdSuppressor,
        isActivityGone = { it.isFinishing || it.isDestroyed || it.isChangingConfigurations },
        isResumed = { it === tracker?.resumedActivity },
        fullScreenShowing = { presence.showing.value },
        allowReset = consentDebug.allowConsentReset,
        logger = logger,
        onGateOpened = ::onGateOpened,
    )

    /**
     * True while a full-screen ad (interstitial or native full screen) is on screen. The network popup (ADR-0024,
     * T-017) waits for it to close.
     */
    val fullScreenAdShowing: StateFlow<Boolean> = presence.showing

    private val _nativeFullScreenAd = MutableStateFlow<ShownNativeFullScreenAd?>(null)

    /** The native full-screen ad on screen, drawn by [FullScreenAdHost]; null when none. */
    internal val nativeFullScreenAd: StateFlow<ShownNativeFullScreenAd?> = _nativeFullScreenAd.asStateFlow()

    /**
     * True while the consent gate is open and the SDK is started. Turns false at once when the gate closes (S-10,
     * S-14), so slots drop the ads they show (S-11, S-15). False before [start] (S-19).
     */
    val canRequestAds: StateFlow<Boolean> =
        combine(gate.state, sdkStarted) { state, started -> state.allowed && started }
            .stateIn(scope, SharingStarted.Eagerly, false)

    /** How ads were allowed in this process (S-13); null before the first result (S-19). */
    val consentPath: StateFlow<ConsentPath?> =
        gate.state.map { it.path }.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * True once the consent round of this start has its final result, or the result was settled before. The loading
     * screen waits for it, at most 10 seconds (T-024).
     */
    val consentFlowFinished: StateFlow<Boolean> = coordinator.finished

    private val _sdkReady = MutableStateFlow(false)

    /** True once the ads SDK has reported its initialization (`MobileAds.initialize` callback). */
    val sdkReady: StateFlow<Boolean> = _sdkReady.asStateFlow()

    /**
     * True when the user must be able to reopen the consent form ([PrivacyOptionsButton]).
     * This is also how the module knows the user is in a region where the law requires consent.
     */
    val privacyOptionsRequired: StateFlow<Boolean> = coordinator.privacyOptionsRequired

    /**
     * Starts :core:ads, once per process, from `Application.onCreate` before any screen (S-01, HD-01): follows the
     * activities that [appActivities] accepts, the consent shortcut (S-12, HD-04) and Internet (S-09). A second call
     * does nothing.
     */
    fun start(application: Application, appActivities: AppActivityFilter) {
        if (!coordinator.start()) {
            logger.info(TAG, "Ads already started")
            return
        }
        logger.info(TAG, "Ads started")
        val activityTracker = AdsActivityTracker(
            appActivities,
            object : AdsActivityTracker.Listener {
                override fun onForegroundChanged(foreground: Boolean) = coordinator.onForegroundChanged(foreground)

                override fun onActivityResumed(activity: Activity) = onAppActivityResumed(activity)
            },
        )
        tracker = activityTracker
        application.registerActivityLifecycleCallbacks(activityTracker)
        scope.launch {
            flags.global
                .map { shortcutActive(shortcutConfig.enabledInBuild, it.consentShortcutEnabled) }
                .distinctUntilChanged()
                .collect { coordinator.onShortcutChanged(it) }
        }
        scope.launch { connectivity.connected.collect { coordinator.onInternetChanged(it) } }
    }

    /**
     * Updates the consent status and shows the consent form when required (S-02). Call it in the activity's
     * `onCreate` at every start.
     */
    fun gatherConsent(activity: Activity) {
        coordinator.gather(activity)
    }

    /** Opens the privacy options form, so the user can change their consent at any time (S-14). */
    fun showPrivacyOptionsForm(activity: Activity) {
        coordinator.showPrivacyOptions(activity)
    }

    /** Clears the stored consent, staging only (S-18, [rememberResetConsent]). */
    internal fun resetConsent() {
        coordinator.reset()
    }

    /**
     * Whether a request may be sent right now (S-08, S-15): the gate is open, the SDK started, and a right that only
     * ERROR_ALLOWED gave is used in foreground only. Asked right before every request is sent.
     */
    internal fun mayRequestNow(): Boolean = sdkStarted.value && coordinator.mayRequestNow(tracker?.foreground ?: false)

    /** An app activity is resumed (from [AdsActivityTracker]). */
    private fun onAppActivityResumed(activity: Activity) {
        // The regather of consent (S-09). T-022 puts the release of a stuck flag before this line (G-06).
        coordinator.onActivityResumed(activity)
    }

    /** The consent gate opened: the first time starts the SDK, later times preload again (S-10, S-14). */
    private fun onGateOpened(firstTime: Boolean) {
        if (firstTime) {
            startSdk()
        } else if (sdkStarted.value) {
            logger.info(TAG, "Consent gate opened again: preload")
            fullScreenPlacements().forEach(::preloadNow)
        }
    }

    private fun startSdk() {
        scope.launch {
            // Google recommends starting the SDK off the main thread.
            withContext(Dispatchers.IO) {
                // The listener runs once every adapter is initialized or timed out (KB-9).
                MobileAds.initialize(context) { status ->
                    adapterStatusLines(status.adapterStatusMap).forEach { logger.info(TAG, it) }
                    _sdkReady.value = true
                }
            }
            sdkStarted.value = true
            fullScreenPlacements().forEach(::preloadWhenEnabled)
        }
    }

    private fun fullScreenPlacements() = AdPlacement.entries.filter { it.format.isFullScreen }

    /**
     * Opens AdMob's Ad Inspector ([rememberOpenAdInspector], staging ad gallery only). Google opens it only on an
     * emulator or a registered test device; elsewhere [onResult] gets [AdInspectorResult.Failed].
     */
    internal fun openAdInspector(activity: Activity, onResult: (AdInspectorResult) -> Unit) {
        if (!canRequestAds.value) {
            logger.info(TAG, "Ad Inspector skipped: ads not started")
            onResult(AdInspectorResult.NotReady)
            return
        }
        logger.info(TAG, "Ad Inspector opened")
        MobileAds.openAdInspector(activity) { error ->
            val result = adInspectorResult(error?.code)
            when (result) {
                is AdInspectorResult.Failed -> logger.warn(TAG, "Ad Inspector failed: ${result.code}")
                else -> logger.info(TAG, "Ad Inspector closed")
            }
            onResult(result)
        }
    }

    /** Call from `Activity.onUserInteraction`: full-screen ads wait until the user stops touching the screen. */
    fun onUserInteraction() {
        guard.onUserInteraction(SystemClock.elapsedRealtime())
    }

    /**
     * Call from `Activity.onStart`. Counts the started activities of the app, so switching between activities or
     * rotating the screen is not a return to the app (kept for the resume ad, T-025).
     */
    fun onAppForeground() {
        if (startedActivities.onStart()) logger.info(TAG, "App in foreground")
    }

    /** Call from `Activity.onStop`. */
    fun onAppBackground() {
        if (startedActivities.onStop()) logger.info(TAG, "App in background")
    }

    /**
     * Shows the full-screen ad of [placement] if the guardrails allow it, and returns whether it was shown.
     * Call it at a natural break (end of a level, after saving), never when the app opens or closes.
     */
    suspend fun showInterstitial(activity: Activity, placement: AdPlacement): Boolean =
        withContext(Dispatchers.Main.immediate) {
            require(placement.format == AdFormat.INTERSTITIAL) { "${placement.key} is not a full-screen placement" }
            // A second call while the first one reads the flags or waits is dropped.
            if (fullScreenInProgress) return@withContext false
            fullScreenInProgress = true
            try {
                if (!awaitFullScreenTurn(placement)) return@withContext false
                val ad = loadedInterstitials.remove(placement)
                if (ad == null) {
                    preloadInterstitial(placement)
                    return@withContext false
                }
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        presence.onClosed()
                        preloadInterstitial(placement)
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Log.w(TAG, "Interstitial ${placement.key} failed to show: ${error.message}")
                        presence.onClosed()
                        preloadInterstitial(placement)
                    }
                }
                // Counted before showing: an ad that fails to show still counts (fewer ads, never more).
                onFullScreenShown()
                presence.onShowing()
                ad.show(activity)
                true
            } finally {
                fullScreenInProgress = false
            }
        }

    /**
     * Shows the full-screen ad of [placement], interstitial or native full screen, if the guardrails allow it, and
     * returns whether it was shown ([rememberShowFullScreenAd]).
     */
    internal suspend fun showFullScreenAd(activity: Activity, placement: AdPlacement): Boolean =
        when (placement.format) {
            AdFormat.INTERSTITIAL -> showInterstitial(activity, placement)
            AdFormat.NATIVE_FULL_SCREEN -> showNativeFullScreen(placement)
            else -> throw IllegalArgumentException("${placement.key} is not a full-screen placement")
        }

    /**
     * Shows the preloaded native full-screen ad of [placement] through [FullScreenAdHost], with the same guardrails
     * and frequency rules as interstitials ([awaitFullScreenTurn], [onFullScreenShown]). Returns at once, showing
     * nothing, when another full-screen ad is in progress or on screen, or when no fresh ad is preloaded (it then
     * starts loading one for the next time): never waits for an ad to load (KB-5).
     */
    private suspend fun showNativeFullScreen(placement: AdPlacement): Boolean =
        withContext(Dispatchers.Main.immediate) {
            if (fullScreenInProgress || presence.showing.value) {
                logger.info(TAG, "Native full-screen skipped: ANOTHER_SHOWING")
                return@withContext false
            }
            if (!preloadedNativeFullScreen.hasFresh(placement, SystemClock.elapsedRealtime())) {
                logger.info(TAG, "Native full-screen skipped: NOT_READY")
                preloadNativeFullScreen(placement)
                return@withContext false
            }
            fullScreenInProgress = true
            try {
                if (!awaitFullScreenTurn(placement)) return@withContext false
                val ad = preloadedNativeFullScreen.take(placement, SystemClock.elapsedRealtime())
                if (ad == null) {
                    // Expired while waiting for the user to stop touching the screen.
                    preloadNativeFullScreen(placement)
                    return@withContext false
                }
                // Counted and reported right before showing, like interstitials.
                onFullScreenShown()
                presence.onShowing()
                _nativeFullScreenAd.value = ShownNativeFullScreenAd(placement, ad)
                logger.info(TAG, "Native full-screen shown: ${placement.key}")
                true
            } finally {
                fullScreenInProgress = false
            }
        }

    /** The user closed the native full-screen ad (close button or back button). Main thread only. */
    internal fun closeNativeFullScreenAd() {
        val shown = _nativeFullScreenAd.value ?: return
        _nativeFullScreenAd.value = null
        shown.ad.destroy()
        presence.onClosed()
        logger.info(TAG, "Native full-screen closed")
        preloadNativeFullScreen(shown.placement)
    }

    /**
     * Whether a full-screen ad of [placement] may be shown now, shared by every full-screen format: consent,
     * [isEnabled], then the in-memory guardrails ([FullScreenAdGuard]) with `ads_interval`, waiting a little while the
     * user is touching the screen. The interval lives in this process only (I-03). When it returns true, the caller
     * shows the ad and calls [onFullScreenShown] right before.
     */
    internal suspend fun awaitFullScreenTurn(placement: AdPlacement): Boolean =
        withContext(Dispatchers.Main.immediate) {
            // The show gate of consent (S-11), then the placement switches.
            if (!canRequestAds.value || !gate.state.value.allowed || !isEnabled(placement).first()) {
                return@withContext false
            }
            val minIntervalMillis = flags.global.first().adsIntervalSeconds * 1_000
            val deadline = SystemClock.elapsedRealtime() + FullScreenAdGuard.MAX_WAIT_MILLIS
            while (true) {
                val now = SystemClock.elapsedRealtime()
                when (val decision = guard.decide(now, minIntervalMillis)) {
                    FullScreenAdGuard.Decision.TooSoon -> return@withContext false
                    is FullScreenAdGuard.Decision.Wait ->
                        if (now + decision.millis > deadline) return@withContext false else delay(decision.millis)
                    FullScreenAdGuard.Decision.Show -> break
                }
            }
            true
        }

    /** Records a full-screen ad about to be shown, for every full-screen format. Main thread only. */
    internal fun onFullScreenShown() {
        guard.onShown(SystemClock.elapsedRealtime())
    }

    /**
     * Whether [placement] may load and show ads: the master switch `is_show_ads` and the placement's own switch
     * `is_show_<code>` (KB-7, ADR-0014). Every ad format, including native ones added later, must check this before
     * loading or showing an ad.
     */
    internal fun isEnabled(placement: AdPlacement): Flow<Boolean> = flags.placementEnabled(placement)

    /**
     * N of the in-list ads of [placement] ([rememberInListAds]): `native_every_<code>`, or null while no ad row may
     * be added (no consent yet, master switch or placement off, bad remote value, or a placement that declares no
     * [AdPlacement.nativeEvery]).
     */
    internal fun inListEvery(placement: AdPlacement): Flow<Int?> {
        if (placement.nativeEvery == null) {
            logger.warn(TAG, "In-list placement has no nativeEvery: ${placement.key}")
            return flowOf(null)
        }
        return combine(
            canRequestAds,
            isEnabled(placement),
            flags.nativeEvery(placement),
        ) { canRequest, enabled, everyCount -> everyCount?.let { inListEvery(canRequest && enabled, it) } }
            .distinctUntilChanged()
    }

    internal fun unitId(placement: AdPlacement): String? = unitIds.of(placement).also {
        if (it == null) Log.e(TAG, "No ad unit for placement ${placement.key}; it shows no ad")
    }

    /**
     * The states of one native ad of [placement] for a slot on screen ([nativeAdStates]): loading, then loaded or
     * unavailable after [NATIVE_LOAD_TIMEOUT_MILLIS]. The ad is destroyed when the collection is cancelled.
     * The caller checks consent and [isEnabled] first, like [NativeAdSlot].
     */
    internal fun nativeAd(placement: AdPlacement): Flow<NativeAdState> {
        val size = requireNotNull(placement.format.nativeLoadSize()) { "${placement.key} is not a native placement" }
        return flow {
            val unitId = unitId(placement)
            if (unitId == null) {
                emit(NativeAdState.Unavailable)
                return@flow
            }
            if (!mayRequestNow()) {
                logger.info(TAG, "Native ${placement.key} not requested: consent gate closed")
                emit(NativeAdState.Unavailable)
                return@flow
            }
            emitAll(
                nativeAdStates(
                    SdkNativeAdLoader(context, adRequest()),
                    unitId,
                    size,
                    NATIVE_LOAD_TIMEOUT_MILLIS,
                    logger,
                    placement.key,
                ),
            )
        }
    }

    /**
     * The user collapsed the collapsible native ad of [placement] ([CollapsibleNativeAdSlot]). Logged only: no
     * analytics event (spec section 6, ADR-0022).
     */
    internal fun onNativeCollapsed(placement: AdPlacement) {
        logger.info(TAG, "native_collapsed placement=${placement.key}")
    }

    /**
     * Loads one native ad of [placement] ([loadOneNativeAd]): Loaded or Unavailable (also when there is no ad unit).
     * The caller owns a Loaded ad and destroys it; one ad per item of a list (T-011). The caller checks consent and
     * [isEnabled] first, like [NativeAdSlot].
     */
    internal suspend fun loadNativeAd(placement: AdPlacement): NativeAdState {
        val size = requireNotNull(placement.format.nativeLoadSize()) { "${placement.key} is not a native placement" }
        val unitId = unitId(placement) ?: return NativeAdState.Unavailable
        if (!mayRequestNow()) {
            logger.info(TAG, "Native ${placement.key} not requested: consent gate closed")
            return NativeAdState.Unavailable
        }
        return loadOneNativeAd(
            SdkNativeAdLoader(context, adRequest()),
            unitId,
            size,
            NATIVE_LOAD_TIMEOUT_MILLIS,
            logger,
            placement.key,
        )
    }

    /** Personalization follows the consent string of UMP only (question 8 of the ads spec): no `npa` extra. */
    internal fun adRequest(): AdRequest = AdRequest.Builder().build()

    /** Placements are off by default (ADR-0014): load an ad only once the placement is switched on remotely. */
    private fun preloadWhenEnabled(placement: AdPlacement) {
        scope.launch {
            isEnabled(placement).distinctUntilChanged().filter { it }.collect { preloadNow(placement) }
        }
    }

    /** Preloads one full-screen ad of [placement] now; each loader checks the switches and the gate itself. */
    private fun preloadNow(placement: AdPlacement) {
        when (placement.format) {
            AdFormat.NATIVE_FULL_SCREEN -> preloadNativeFullScreen(placement)
            else -> preloadInterstitial(placement)
        }
    }

    /**
     * Loads one native full-screen ad of [placement] for later, unless a fresh one is kept or loading, or the
     * placement is off. Media of any aspect ratio, video muted, AdChoices in the top right corner.
     */
    private fun preloadNativeFullScreen(placement: AdPlacement) {
        if (placement in nativeFullScreenLoading) return
        if (preloadedNativeFullScreen.hasFresh(placement, SystemClock.elapsedRealtime())) return
        val unitId = unitId(placement) ?: return
        nativeFullScreenLoading += placement
        scope.launch {
            if (!canRequestAds.value || !isEnabled(placement).first()) {
                nativeFullScreenLoading -= placement
                return@launch
            }
            val options = NativeAdOptions.Builder()
                .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                .setMediaAspectRatio(MediaAspectRatio.ANY)
                .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                .build()
            val loader = AdLoader.Builder(context, unitId)
                .forNativeAd { ad ->
                    nativeFullScreenLoading -= placement
                    preloadedNativeFullScreen.put(placement, SdkLoadedNativeAd(ad), SystemClock.elapsedRealtime())
                }
                .withAdListener(
                    object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            nativeFullScreenLoading -= placement
                            logger.warn(TAG, "Native full-screen ${placement.key} failed to load: ${error.code}")
                        }
                    },
                )
                .withNativeAdOptions(options)
                .build()
            // Asked right before sending (S-15): a closed gate ends the round, no request, no retry.
            if (!mayRequestNow()) {
                nativeFullScreenLoading -= placement
                logger.info(TAG, "Native full-screen ${placement.key} not requested: consent gate closed")
                return@launch
            }
            loader.loadAd(adRequest())
        }
    }

    private fun preloadInterstitial(placement: AdPlacement) {
        val unitId = unitId(placement) ?: return
        scope.launch {
            if (placement in loadedInterstitials || !isEnabled(placement).first()) return@launch
            // Asked right before sending (S-15): a closed gate ends the round, no request, no retry.
            if (!mayRequestNow()) {
                logger.info(TAG, "Interstitial ${placement.key} not requested: consent gate closed")
                return@launch
            }
            InterstitialAd.load(
                context,
                unitId,
                adRequest(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        loadedInterstitials[placement] = ad
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Interstitial ${placement.key} failed to load: ${error.message}")
                    }
                },
            )
        }
    }

    private companion object {
        const val TAG = "Ads"

        /** Time to wait for a native ad before its space is removed (kept from wave 2 until T-027). */
        const val NATIVE_LOAD_TIMEOUT_MILLIS = 10_000L
    }
}

/** The native full-screen ad on screen and its placement. */
internal class ShownNativeFullScreenAd(val placement: AdPlacement, val ad: LoadedNativeAd)

/**
 * The size a native ad of this format is loaded at, `null` for formats that are not native. Exhaustive on purpose,
 * with no `else`: a new [AdFormat] must decide here how it loads.
 */
internal fun AdFormat.nativeLoadSize(): NativeAdSize? = when (this) {
    AdFormat.BANNER -> null
    AdFormat.INTERSTITIAL -> null
    AdFormat.NATIVE_SMALL -> NativeAdSize.SMALL
    AdFormat.NATIVE_MEDIUM -> NativeAdSize.MEDIUM
    // Opens expanded, with media, so it loads as medium.
    AdFormat.NATIVE_COLLAPSIBLE -> NativeAdSize.MEDIUM
    // Preloaded by its own loader with full-screen options, never through nativeAd or loadNativeAd.
    AdFormat.NATIVE_FULL_SCREEN -> null
    // Not shown by any function yet: app open and rewarded are not native; the pool of T-027, T-028 loads the
    // native step and in-collection cards.
    AdFormat.APP_OPEN -> null
    AdFormat.REWARDED -> null
    AdFormat.BANNER_COLLAPSIBLE -> null
    AdFormat.NATIVE_STEP -> null
    AdFormat.NATIVE_IN_COLLECTION -> null
}
