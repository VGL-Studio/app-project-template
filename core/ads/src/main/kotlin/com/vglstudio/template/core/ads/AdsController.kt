package com.vglstudio.template.core.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.vglstudio.template.core.analytics.AdMetrics
import com.vglstudio.template.core.logging.AppLogger
import com.vglstudio.template.core.network.InternetConnectivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The only class that talks to the ads SDKs (ADR-0004). :app calls [start] once in `Application.onCreate` (HD-01),
 * [gatherConsent], [onAppForeground] and [onAppBackground] from its activity, places [AdsHost] at the root of its UI
 * (it provides [LocalAdsController], G-04), and draws native full-screen ads with [FullScreenAdHost]. Features use
 * [AdSlot], [NativeAdSlot], [rememberShowFullScreenAd], [rememberAdSuppression], [SuppressResumeAds],
 * [rememberMarkLeavingApp] and [PrivacyOptionsButton], never this class or an SDK directly.
 *
 * Full-screen ads follow docs/ops/ads-business-rules.md, sections 3, 4 and 6 (plan T-022): one decision order
 * ([decideFullScreenShow]), a showing flag with a cap ([FullScreenAdPresence]), `ads_interval` marks per interval
 * group written when an interstitial closes ([FullScreenAdGuard], [MonotonicClock]), stuck recovery
 * ([StuckFullScreenRecovery]) and a completion that runs once ([FullScreenCompletion]). Lifecycle events of the app's
 * activities come from [AdsActivityTracker], so :app calls nothing more for them.
 *
 * Ad metrics follow sections 8.1 and 8.2 (plan T-023): every format listens to the SDK's paid events and its load,
 * click and show callbacks, and hands what it reads to [AdTelemetry], which decides what is logged through
 * [AdMetrics]. Probing the state ([isFullScreenAdReady], [loadFullScreenAdIfNeeded]) records nothing.
 *
 * Consent follows the PARTNER rules S-01 to S-19 ([ConsentCoordinator], [ConsentGate]): the ads SDK is initialized the
 * first time the consent gate opens, and every request asks the gate right before it is sent ([mayRequestNow]).
 *
 * Preloads follow S-20 to S-25 (plan T-021): no slot loads by itself; every preload, reload when Internet comes back
 * and native refill goes through the plan ([AdPreloadPlan], [AdPreloader]) once the SDK is ready. Failed loads retry
 * by F-05 and O-05 ([FullScreenReloads], [retryingNativeAdStates]).
 *
 * A native small slot (plan T-040) takes its ad from the single-ad source of its placement ([NativeAdSources],
 * O-01 to O-04) and replaces it in place every 60 s in foreground ([refreshingNativeAdStates], CL-02); every send of
 * it goes through one gate ([awaitSendAllowed], L-072). Native ads are not shown 55 minutes after loading (O-04).
 *
 * Native medium and in-collection cards come from pools (plan T-028, P-01 to P-08): one [NativeAdPool] per (format,
 * ad unit), managed by [NativeAdPools], filled one request at a time through the same send gate and by the preload
 * plan (HD-02, S-22, S-25). [NativeAdSlot] of a medium placement, a medium row of [rememberInListAds] and
 * [NativeAdCell] are cells of a pool ([pooledCell]): they claim an ad while on screen, give it back when they leave,
 * and record their opportunity (P-07). A load of a pool logs `ad_placement = pool` (PL-02).
 *
 * Banners follow B-01 to B-06 (plan T-029): [AdSlot] keeps every send in one loop ([runBannerLoads]) that waits for
 * the screen to show and asks [mayRequestNow] right before it sends; a collapsible banner reloads every 30 s.
 * Mediation adapters (AppLovin, plan T-015) are initialized by the SDK at the same time, with no code of their own;
 * their status is logged.
 *
 * The ad of the loading screen (plan T-024, KB-13) is [StartupAds]: [awaitStartupAd] waits within the limits of A-03
 * ([awaitStartupAd] of StartupAdWait.kt), and [showStartupAd] shows the app open ad of the app open slot (A-07 to
 * A-09) or the interstitial of `open_app_inter`. App-wide placements are switched by [placementSwitch].
 */
@Singleton
class AdsController @Inject internal constructor(
    @param:ApplicationContext private val context: Context,
    private val unitIds: AdUnitIds,
    consentDebug: ConsentDebugOptions,
    private val flags: AdsFlags,
    private val connectivity: InternetConnectivity,
    private val shortcutConfig: ConsentShortcutConfig,
    metrics: AdMetrics,
    internal val logger: AppLogger,
    private val firstOpenMark: FirstOpenMark,
    private val interDisplayCounter: InterDisplayCounter,
) : StartupAds {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Touched on the main thread only.
    private val clock: MonotonicClock = SystemMonotonicClock
    private val guard = FullScreenAdGuard()
    private val startedActivities = StartedActivityCounter()
    private val loadedInterstitials = mutableMapOf<AdPlacement, InterstitialAd>()
    private val presence = FullScreenAdPresence()
    private val stuck = StuckFullScreenRecovery()
    private val preloadedNativeFullScreen = nativeFullScreenCache<LoadedNativeAd>(
        onDiscard = {
            nativeFullScreenCells -= it
            it.destroy()
        },
    )

    /** The app open slot (A-07): a load replaces the ad kept before it, an ad an hour old is dropped when read. */
    private val loadedAppOpens = FreshAdCache<AppOpenAd>(APP_OPEN_MAX_AGE_MILLIS, onDiscard = { appOpenCells -= it })

    /** When the last app open ad closed, for the lock of A-08; null before the first one. */
    private var lastAppOpenClosedAt: Long? = null

    /** When [start] ran: the origin of the three limits of A-03 (question 4 of plan T-024); null before it. */
    private var startedAtMillis: Long? = null

    /**
     * Whether this process is the first open since install (A-01): the mark is read and written once, the first time
     * a switch of an app-wide placement is read, which is in [start] (question 6 of plan T-024).
     */
    private val firstOpenResult by lazy { scope.async { firstOpenMark.consume() } }
    private val firstOpen: Flow<Boolean> = flow { emit(firstOpenResult.await()) }

    /** The placement of the startup ad the keys choose now, collected from [start]; [startupChoiceKnown] once read. */
    private val startupChoice = MutableStateFlow<AdPlacement?>(null)
    private val startupChoiceKnown = MutableStateFlow(false)

    /** Loading and waiting for a retry, per full-screen placement; ready and idle are read from the loaded ads. */
    private val loadStates = mutableMapOf<AdPlacement, FullScreenLoadState>()

    /** The `load_state` label of the last load error per full-screen placement, cleared by a load (section 8.2). */
    private val lastLoadErrors = mutableMapOf<AdPlacement, String>()

    /** The placement a loaded full-screen ad is shown for, set at each show and read by its paid event (PL-03). */
    private val interstitialCells = mutableMapOf<InterstitialAd, PlacementCell>()
    private val nativeFullScreenCells = mutableMapOf<LoadedNativeAd, PlacementCell>()
    private val appOpenCells = mutableMapOf<AppOpenAd, PlacementCell>()

    /** The completion of the full-screen turn on screen, and of the native full-screen ad on screen. */
    private var currentTurn: FullScreenCompletion? = null
    private var nativeTurn: FullScreenCompletion? = null

    private val gate = ConsentGate()
    private var tracker: AdsActivityTracker? = null
    private val sdkStarted = MutableStateFlow(false)

    /** True while an app activity is started, as [AdsActivityTracker] reports it; native requests wait for it. */
    private val foregroundState = MutableStateFlow(false)

    /** When `MobileAds.initialize` was called (S-21), and whether its callback came. */
    private var sdkInitStartedAt: Long? = null
    private var sdkCallbackArrived = false

    /** Failures in a row per load slot: the metrics and the retry waits read the same counter (plan T-021). */
    private val loadCounter = LoadAttemptCounter()

    /** The switch of each full-screen placement, as last read in [start]; off until then (ADR-0014). */
    private val enabledNow = mutableMapOf<AdPlacement, Boolean>()

    /** Latest values read by the metrics, collected from [start]; before it nothing is served (S-19). */
    private val latestGlobalFlags = MutableStateFlow<AdsGlobalFlags?>(null)
    private val online = MutableStateFlow(true)

    /** Blocks the ads that show by themselves while a consent form is open (S-16, G-01); read by T-022, T-025. */
    internal val foregroundAdSuppressor = ForegroundAdSuppressor()

    /** The block of the ads that show by themselves, with the leave mark and the shell's report (G-02 to G-05). */
    internal val resumeSuppression = ResumeSuppression(foregroundAdSuppressor, logger)

    /** True while resume ads must not show (KB-23); read by the resume ad (T-025). */
    internal val resumeAdsSuppressed: StateFlow<Boolean> = resumeSuppression.resumeAdsSuppressed

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

    /** G-06: stuck recovery before the regather of consent (S-09), then the leave mark. */
    private val resumeSteps = ActivityResumeSteps(
        recoverStuck = { recoverStuckFullScreen(it) },
        regatherConsent = { coordinator.onActivityResumed(it) },
        releaseLeaveMark = { resumeSuppression.onActivityResumed() },
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

    /** The recorder of ad metrics (plan T-023); the banner of [AdSlot] and the native loads report to it too. */
    internal val telemetry = AdTelemetry(
        metrics = metrics,
        lifecycleSwitches = { latestGlobalFlags.value ?: DEFAULT_GLOBAL_FLAGS },
        serving = { canRequestAds.value },
        offline = { !online.value },
        counter = loadCounter,
        logger = logger,
    )

    /** How ads were allowed in this process (S-13); null before the first result (S-19). */
    val consentPath: StateFlow<ConsentPath?> =
        gate.state.map { it.path }.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * True once the consent round of this start has its final result, or the result was settled before. The loading
     * screen waits for it, at most 10 seconds (T-024).
     */
    val consentFlowFinished: StateFlow<Boolean> = coordinator.finished

    private val _sdkReady = MutableStateFlow(false)

    /**
     * True once the ads SDK has reported its initialization (`MobileAds.initialize` callback), or 35 s after it was
     * started (S-21). Only the preload plan waits for it; [canRequestAds] does not (question 10 of plan T-021).
     */
    val sdkReady: StateFlow<Boolean> = _sdkReady.asStateFlow()

    private val fullScreenReloads = FullScreenReloads(
        scope = scope,
        clock = clock,
        failuresInARow = { loadCounter.failuresInARow(preloadCounterKey(it.key)) },
        loadIfNeeded = { loadFullScreenAdIfNeeded(it) },
        foreground = foregroundState,
        logger = logger,
    )

    /** One slot of the preload plan per full-screen placement. */
    private val preloadSlots: List<PreloadSlot> = fullScreenPlacements().map { placement ->
        FullScreenPreloadSlot(
            placement = placement,
            enabled = { enabledNow[placement] == true },
            state = { fullScreenLoadState(placement) },
            load = { loadFullScreenAdIfNeeded(it) },
        )
    }

    private val preloader = AdPreloader(
        scope = scope,
        runner = PreloadPlanRunner(clock, logger),
        // Full-screen placements, then the slot of each native pool (HD-02, S-22); a pool slot is asked lazily.
        slotsOf = { format -> preloadSlots.filter { it.format == format } + nativePools.preloadSlots(format) },
        gate = {
            PreloadGate(
                sdkStarted = sdkStarted.value,
                sdkReady = _sdkReady.value,
                mayRequest = mayRequestNow(),
                consentDenied = gate.state.value.path == ConsentPath.DENIED_OR_REQUIRED,
            )
        },
        askConsentAgain = { coordinator.onPreloadRequested() },
        logger = logger,
    )

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
        startedAtMillis = clock.nowMillis()
        val activityTracker = AdsActivityTracker(
            appActivities,
            object : AdsActivityTracker.Listener {
                override fun onForegroundChanged(foreground: Boolean) {
                    foregroundState.value = foreground
                    coordinator.onForegroundChanged(foreground)
                }

                override fun onActivityResumed(activity: Activity) {
                    resumeSteps.onResumed(activity)
                    // S-25, after the steps of G-06.
                    preloader.onActivityResumed(presence.showing.value)
                }

                override fun onActivityPaused(activity: Activity) =
                    stuck.onActivityPaused(System.identityHashCode(activity), presence.current?.hostActivityId)

                override fun onActivityDestroyed(activity: Activity) = onAppActivityDestroyed(activity)
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
        scope.launch {
            connectivity.connected.collect {
                online.value = it
                coordinator.onInternetChanged(it)
            }
        }
        scope.launch { flags.global.collect { latestGlobalFlags.value = it } }
        scope.launch {
            combine(flags.global, firstOpen) { global, first -> startupAdPlacement(global, first) }.collect {
                startupChoice.value = it
                startupChoiceKnown.value = true
            }
        }
        collectPreloadTriggers()
    }

    /**
     * What asks the plan for a preload besides the SDK getting ready: a full-screen placement switched on remotely
     * (S-20), a change of the gate for a held preload (S-23), Internet back (S-24).
     */
    private fun collectPreloadTriggers() {
        // Full-screen placements and pooled ones: switching either on remotely asks for a preload (S-20).
        val placements = fullScreenPlacements() + pooledPlacements()
        scope.launch {
            combine(placements.map { placement -> isEnabled(placement).map { placement to it } }) { it.toMap() }
                .collect { switches ->
                    val switchedOn = switches.any { (placement, on) -> on && enabledNow[placement] != true }
                    enabledNow.putAll(switches)
                    if (switchedOn) preloader.request(PreloadReason.PLACEMENT_ENABLED)
                }
        }
        scope.launch {
            combine(canRequestAds, sdkReady, consentPath, foregroundState) { _, _, _, _ -> }
                .collect { preloader.onGateChanged() }
        }
        // The value read in start, so Internet is watched once (S-24, T-016).
        scope.launch { networkReturns(online).collect { preloader.onNetworkReturned() } }
    }

    /**
     * The startup ad of the loading screen (KB-13): waits within the limits of A-03, counted from [start], probing
     * without recording (A-04), and returns the turn to show, or null; null at once when the key of this open is off.
     * Null before [start] (tests, previews).
     */
    override suspend fun awaitStartupAd(): StartupAdTurn? = withContext(Dispatchers.Main.immediate) {
        val startedAt = startedAtMillis
        if (startedAt == null) {
            logger.info(TAG, "startup ad: ads not started")
            return@withContext null
        }
        startupChoiceKnown.first { it }
        val probe = object : StartupAdProbe {
            override fun nowMillis(): Long = clock.nowMillis()

            override val consentFinished: Boolean get() = consentFlowFinished.value

            override val consentOpen: Boolean get() = gate.state.value.allowed

            override val sdkReady: Boolean get() = _sdkReady.value

            override fun chosen(): AdPlacement? = startupChoice.value

            override fun isReady(placement: AdPlacement): Boolean = isFullScreenAdReady(placement)

            override fun ensureLoading(placement: AdPlacement) = loadFullScreenAdIfNeeded(placement)

            override fun recordNotReady(placement: AdPlacement) = telemetry.opportunity(
                placement.format,
                placement,
                FullScreenShowDecision.NOT_READY.opportunityResult(),
                opportunityLoadState(placement),
            )
        }
        awaitStartupAd(probe, startedAt, logger = logger)?.let { StartupAdTurn(it) }
    }

    /**
     * Shows [turn] ([rememberShowStartupAd]) in the scope of this controller, so a recreated activity does not cancel
     * it: `open_app` through the app open slot, `open_app_inter` as an interstitial that skips the `ads_interval` gate
     * and moves its mark when it closes (A-02). [onFinished] runs once on the main thread, with true only when the
     * interstitial was shown.
     */
    internal fun showStartupAd(activity: Activity, turn: StartupAdTurn, onFinished: (interShown: Boolean) -> Unit) {
        scope.launch {
            var interShown = false
            val completion = FullScreenCompletion(logger).apply { set { onFinished(interShown) } }
            if (activity.isFinishing || activity.isDestroyed) {
                logger.info(TAG, "Startup ad ${turn.placement.key} skipped: activity gone")
                completion.complete("activity_gone")
                return@launch
            }
            val finish = { completion.complete("startup_ad") }
            when (turn.placement.format) {
                AdFormat.APP_OPEN -> showAppOpen(activity, turn.placement, finish)
                AdFormat.INTERSTITIAL -> {
                    val shown = showInterstitial(activity, turn.placement, finish)
                    interShown = shown
                }
                else -> {
                    logger.warn(TAG, "Startup ad ${turn.placement.key}: format not shown")
                    completion.complete("format")
                }
            }
        }
    }

    /** Sets the preload plan (HD-02); call it in `Application.onCreate`, before consent has a result. */
    fun setPreloadPlan(plan: AdPreloadPlan) {
        preloader.setPlan(plan)
    }

    /** Milliseconds of the monotonic clock, for the load latency of the ad metrics. */
    internal fun metricsNowMillis(): Long = clock.nowMillis()

    /**
     * Moves when usable Internet comes back (S-24): a banner waiting for its retry sends at once, like a native slot.
     */
    internal val retryResets: StateFlow<Int> get() = preloader.nativeRetryResets

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

    /**
     * F-03, first step of every resume (G-06): the activity that showed the full-screen ad was paused and is resumed
     * while the ad still reads as showing. Lowers the flag, writes the mark of the ad's group now and runs the
     * waiting completion once; the ad's screen itself is left alone.
     */
    private fun recoverStuckFullScreen(activity: Activity) {
        val shown = presence.current
        if (!stuck.onActivityResumed(System.identityHashCode(activity), shown?.hostActivityId)) return
        val placement = shown?.placement ?: return
        recordIntervalMark(placement)
        logger.warn(TAG, "Full-screen ${placement.key} stuck: flag released")
        endCurrentTurn("stuck_recovered")
    }

    /**
     * N-04, HD-03: the activity that showed the full-screen ad is destroyed before the ad reported its close. Lowers
     * the flag and runs the waiting completion once (forced completion).
     */
    internal fun onAppActivityDestroyed(activity: Activity) {
        val shown = presence.current ?: return
        if (shown.hostActivityId != System.identityHashCode(activity)) return
        logger.warn(TAG, "Full-screen ${shown.placement.key}: host destroyed before close")
        endCurrentTurn("host_destroyed")
    }

    /** The consent gate opened: the first time starts the SDK, later times preload again (S-10, S-14). */
    private fun onGateOpened(firstTime: Boolean) {
        if (firstTime) {
            startSdk()
        } else if (sdkStarted.value) {
            logger.info(TAG, "Consent gate opened again: preload")
            preloader.request(PreloadReason.GATE_REOPENED)
        }
    }

    private fun startSdk() {
        sdkInitStartedAt = clock.nowMillis()
        // S-21: the SDK counts as ready 35 s after it was started, even without its callback.
        scope.launch {
            delay(SDK_READY_CAP_MILLIS)
            updateSdkReady("35 s cap")
        }
        scope.launch {
            // Google recommends starting the SDK off the main thread.
            withContext(Dispatchers.IO) {
                // The listener runs once every adapter is initialized or timed out (KB-9).
                MobileAds.initialize(context) { status ->
                    adapterStatusLines(status.adapterStatusMap).forEach { logger.info(TAG, it) }
                    scope.launch {
                        sdkCallbackArrived = true
                        updateSdkReady("callback")
                    }
                }
            }
            sdkStarted.value = true
            // The first preload of the plan, once the SDK is both started and ready (S-20, S-21).
            _sdkReady.first { it }
            preloader.request(PreloadReason.SDK_READY)
        }
    }

    /** Main thread. */
    private fun updateSdkReady(source: String) {
        if (_sdkReady.value || !sdkReadyAt(sdkInitStartedAt, sdkCallbackArrived, clock.nowMillis())) return
        logger.info(TAG, "SDK ready: $source")
        _sdkReady.value = true
    }

    private fun fullScreenPlacements() = AdPlacement.entries.filter { it.format.hasFullScreenSlot }

    private fun pooledPlacements() = AdPlacement.entries.filter { it.format.usesPool }

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
     * Shows the interstitial of [placement] if the full-screen gate allows it, and returns whether it was shown.
     * [onFinished] runs exactly once: at once when the turn is blocked or no ad is loaded (N-02, F-07), else when the
     * ad closes, fails to show, is found stuck or loses its activity (N-04). Call it at a natural break (end of a
     * level, after saving), never when the app opens or closes.
     */
    suspend fun showInterstitial(activity: Activity, placement: AdPlacement, onFinished: () -> Unit = {}): Boolean =
        withContext(Dispatchers.Main.immediate) {
            require(placement.format == AdFormat.INTERSTITIAL) { "${placement.key} is not a full-screen placement" }
            val decision = decideFullScreenTurn(placement)
            val loadState = opportunityLoadState(placement)
            if (decision != FullScreenShowDecision.SHOW) {
                return@withContext skipTurn(placement, decision, loadState, onFinished)
            }
            if (!presence.tryBegin(placement, System.identityHashCode(activity), clock.nowMillis())) {
                return@withContext skipTurn(placement, FullScreenShowDecision.BUSY, loadState, onFinished)
            }
            val ad = loadedInterstitials.remove(placement)
            if (ad == null) {
                presence.onClosed()
                return@withContext skipTurn(placement, FullScreenShowDecision.NOT_READY, loadState, onFinished)
            }
            interstitialCells.remove(ad)?.value = placement
            recordShownOpportunity(placement)
            val turn = beginTurn(onFinished)
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // First line (I-05): the mark is written before anything else can fail.
                    recordIntervalMark(placement)
                    endTurn(turn, "closed")
                    fullScreenReloads.onClosedOrShowFailed(placement)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    logger.warn(TAG, "Interstitial ${placement.key} failed to show: ${error.code}")
                    telemetry.showFailed(AdFormat.INTERSTITIAL, placement, error.code)
                    endTurn(turn, "show_failed")
                    fullScreenReloads.onClosedOrShowFailed(placement)
                }

                override fun onAdClicked() = telemetry.clicked(AdFormat.INTERSTITIAL, placement)
            }
            logger.info(TAG, "Interstitial shown: ${placement.key}")
            ad.show(activity)
            true
        }

    /**
     * Shows the app open ad of [placement] (A-05): the full-screen gate of [decideFullScreenShow], with the lock of
     * one second after the previous app open ad closed (A-08) in place of the `ads_interval` mark, which app open
     * never reads or moves (I-02). The ad leaves the slot when it shows, so an ad loaded while it is on screen is kept
     * (A-09); closing it or failing to show loads again at once (F-06). [onFinished] runs once, as for
     * [showInterstitial].
     */
    private suspend fun showAppOpen(activity: Activity, placement: AdPlacement, onFinished: () -> Unit): Boolean =
        withContext(Dispatchers.Main.immediate) {
            val enabled = isEnabled(placement).first()
            val now = clock.nowMillis()
            val decision = decideFullScreenShow(
                consentOpen = canRequestAds.value && gate.state.value.allowed,
                enabled = enabled,
                busy = presence.isBusy(now),
                ready = isFullScreenAdReady(placement),
                intervalPassed = appOpenLockPassed(lastAppOpenClosedAt, now),
            )
            val loadState = opportunityLoadState(placement)
            if (decision != FullScreenShowDecision.SHOW) {
                return@withContext skipTurn(placement, decision, loadState, onFinished)
            }
            if (!presence.tryBegin(placement, System.identityHashCode(activity), now)) {
                return@withContext skipTurn(placement, FullScreenShowDecision.BUSY, loadState, onFinished)
            }
            val ad = loadedAppOpens.take(placement, now)
            if (ad == null) {
                presence.onClosed()
                return@withContext skipTurn(placement, FullScreenShowDecision.NOT_READY, loadState, onFinished)
            }
            appOpenCells.remove(ad)?.value = placement
            recordShownOpportunity(placement)
            val turn = beginTurn(onFinished)
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // First line, as for interstitials; no `ads_interval` mark (I-02).
                    lastAppOpenClosedAt = clock.nowMillis()
                    endTurn(turn, "closed")
                    fullScreenReloads.onClosedOrShowFailed(placement)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    logger.warn(TAG, "App open ${placement.key} failed to show: ${error.code}")
                    telemetry.showFailed(AdFormat.APP_OPEN, placement, error.code)
                    endTurn(turn, "show_failed")
                    fullScreenReloads.onClosedOrShowFailed(placement)
                }

                override fun onAdClicked() = telemetry.clicked(AdFormat.APP_OPEN, placement)
            }
            logger.info(TAG, "App open shown: ${placement.key}")
            ad.show(activity)
            true
        }

    /**
     * Shows the full-screen ad of [placement], interstitial or native full screen, if the full-screen gate allows it,
     * and returns whether it was shown ([rememberShowFullScreenAd]). [onFinished] runs once, as for
     * [showInterstitial].
     */
    internal suspend fun showFullScreenAd(
        activity: Activity,
        placement: AdPlacement,
        onFinished: () -> Unit = {},
    ): Boolean = when (placement.format) {
        AdFormat.INTERSTITIAL -> showInterstitial(activity, placement, onFinished)
        AdFormat.NATIVE_FULL_SCREEN -> showNativeFullScreen(placement, onFinished)
        else -> throw IllegalArgumentException("${placement.key} is not a full-screen placement")
    }

    /**
     * Shows the preloaded native full-screen ad of [placement] through [FullScreenAdHost], with the same gate as
     * interstitials ([decideFullScreenTurn]); it reads and writes the `ads_interval` mark like one (question 2 of plan
     * T-022). Returns at once, showing nothing, when the gate says no; never waits for an ad to load (KB-5). It is
     * drawn by the composition, not by an activity of its own, so it has no host activity for stuck recovery.
     */
    private suspend fun showNativeFullScreen(placement: AdPlacement, onFinished: () -> Unit): Boolean =
        withContext(Dispatchers.Main.immediate) {
            val decision = decideFullScreenTurn(placement)
            val loadState = opportunityLoadState(placement)
            if (decision != FullScreenShowDecision.SHOW) {
                return@withContext skipTurn(placement, decision, loadState, onFinished)
            }
            val now = clock.nowMillis()
            if (!presence.tryBegin(placement, NO_HOST_ACTIVITY, now)) {
                return@withContext skipTurn(placement, FullScreenShowDecision.BUSY, loadState, onFinished)
            }
            val ad = preloadedNativeFullScreen.take(placement, now)
            if (ad == null) {
                presence.onClosed()
                return@withContext skipTurn(placement, FullScreenShowDecision.NOT_READY, loadState, onFinished)
            }
            nativeFullScreenCells.remove(ad)?.value = placement
            recordShownOpportunity(placement)
            nativeTurn = beginTurn(onFinished)
            _nativeFullScreenAd.value = ShownNativeFullScreenAd(placement, ad)
            logger.info(TAG, "Native full-screen shown: ${placement.key}")
            true
        }

    // Skeleton of plan T-026, step 3.
    internal val nativeFullScreenView: StateFlow<NativeFullScreenView?> = MutableStateFlow(null)

    internal fun closeNativeFullScreen() {}

    internal fun onNativeFullScreenBack() {}

    internal fun beginInterstitialTurn(
        placement: AdPlacement,
        hostActivityId: Int,
        combo: Boolean,
        onFinished: () -> Unit,
    ): FullScreenCompletion? = null

    internal fun onInterstitialClosed(placement: AdPlacement, turn: FullScreenCompletion) {}

    internal fun onInterstitialShowFailed(placement: AdPlacement, turn: FullScreenCompletion, code: Int) {}

    internal fun onInterstitialLost(placement: AdPlacement, turn: FullScreenCompletion, loadState: String) {}

    internal fun onAppActivityResumed(activity: Activity) {}

    internal fun onAppActivityPaused(activity: Activity) {}

    internal fun beginStandaloneNativeTurn(
        placement: AdPlacement,
        takeAd: () -> LoadedNativeAd?,
        onFinished: () -> Unit,
    ): Boolean = false

    internal fun openConsentGateForTest() {}

    /** The user closed the native full-screen ad (close button or back button). Main thread only. */
    internal fun closeNativeFullScreenAd() {
        val shown = _nativeFullScreenAd.value ?: return
        // First line (I-05), as for the close of an interstitial.
        recordIntervalMark(shown.placement)
        _nativeFullScreenAd.value = null
        shown.ad.destroy()
        val turn = nativeTurn
        nativeTurn = null
        if (turn != null) endTurn(turn, "closed") else presence.onClosed()
        logger.info(TAG, "Native full-screen closed")
        fullScreenReloads.onClosedOrShowFailed(shown.placement)
    }

    /**
     * The full-screen gate of [placement], shared by every full-screen format, in the order of [decideFullScreenShow]:
     * consent (S-11), the placement switches, another full-screen ad on screen (F-01, F-02), a loaded ad (F-04), then
     * the `ads_interval` mark of the placement's group unless it declares [AdPlacement.ignoreInterval] (I-01, I-04,
     * MC-04). Reads the flags, never loads.
     */
    internal suspend fun decideFullScreenTurn(placement: AdPlacement): FullScreenShowDecision =
        withContext(Dispatchers.Main.immediate) {
            val enabled = isEnabled(placement).first()
            val intervalMillis = flags.intervalSeconds(placement.intervalGroup).first() * 1_000
            val now = clock.nowMillis()
            decideFullScreenShow(
                consentOpen = canRequestAds.value && gate.state.value.allowed,
                enabled = enabled,
                busy = presence.isBusy(now),
                ready = isFullScreenAdReady(placement),
                intervalPassed = !placement.declaration.readsInterval() ||
                    guard.allows(placement.intervalGroup, intervalMillis, now),
            )
        }

    /**
     * Whether a full-screen ad of [placement] is loaded and may be shown now. Reads the state only, never loads
     * (F-04); for the loading screen (T-024) and the retry rules (T-021).
     */
    internal fun isFullScreenAdReady(placement: AdPlacement): Boolean =
        fullScreenLoadState(placement) == FullScreenLoadState.Ready

    /** Loads a full-screen ad of [placement] unless one is loading, loaded, or waiting for a retry (F-04). */
    internal fun loadFullScreenAdIfNeeded(placement: AdPlacement) {
        if (needsLoad(fullScreenLoadState(placement), clock.nowMillis(), canRequestAds.value)) preloadNow(placement)
    }

    private fun fullScreenLoadState(placement: AdPlacement): FullScreenLoadState = when {
        placement.format == AdFormat.INTERSTITIAL && placement in loadedInterstitials -> FullScreenLoadState.Ready
        placement.format == AdFormat.NATIVE_FULL_SCREEN &&
            preloadedNativeFullScreen.hasFresh(placement, clock.nowMillis()) -> FullScreenLoadState.Ready
        placement.format == AdFormat.APP_OPEN &&
            loadedAppOpens.hasFresh(placement, clock.nowMillis()) -> FullScreenLoadState.Ready
        else -> loadStates[placement] ?: FullScreenLoadState.Idle
    }

    /** `load_state` of an opportunity of [placement], taken at the decision (section 8.2). */
    private fun opportunityLoadState(placement: AdPlacement): String =
        loadStateAtDecision(fullScreenLoadState(placement), lastLoadErrors[placement])

    /** `shown` always goes with `ready` (section 8.2). */
    private fun recordShownOpportunity(placement: AdPlacement) {
        telemetry.opportunity(placement.format, placement, FullScreenShowDecision.SHOW.opportunityResult(), READY)
    }

    /** I-02, I-05: the mark of [placement]'s group moves now, if the placement pushes a mark (MC-04). */
    private fun recordIntervalMark(placement: AdPlacement) {
        if (placement.declaration.pushesIntervalMark()) guard.record(placement.intervalGroup, clock.nowMillis())
    }

    /** The completion of a turn that starts showing now; a turn left over after the cap of F-02 finishes first. */
    private fun beginTurn(onFinished: () -> Unit): FullScreenCompletion {
        currentTurn?.complete("cap_expired")
        return FullScreenCompletion(logger).also {
            it.set(onFinished)
            currentTurn = it
        }
    }

    /** Ends [turn]: lowers the flag only when it is still the turn on screen, then runs its completion once. */
    private fun endTurn(turn: FullScreenCompletion, reason: String) {
        if (currentTurn === turn) {
            currentTurn = null
            presence.onClosed()
        }
        turn.complete(reason)
    }

    /** Ends the turn on screen, from stuck recovery or forced completion. */
    private fun endCurrentTurn(reason: String) {
        val turn = currentTurn
        if (turn != null) endTurn(turn, reason) else presence.onClosed()
    }

    /**
     * A turn that does not show: logs [decision] and its opportunity with the [loadState] taken at the decision,
     * loads when no ad is ready (F-07), and finishes at once (N-02).
     */
    private fun skipTurn(
        placement: AdPlacement,
        decision: FullScreenShowDecision,
        loadState: String,
        onFinished: () -> Unit,
    ): Boolean {
        logger.info(TAG, "Full-screen ${placement.key} skipped: $decision")
        // Before the load of this turn changes the state (section 8.2).
        telemetry.opportunity(placement.format, placement, decision.opportunityResult(), loadState)
        if (decision == FullScreenShowDecision.NOT_READY) loadFullScreenAdIfNeeded(placement)
        FullScreenCompletion(logger).apply { set(onFinished) }.complete(decision.name.lowercase())
        return false
    }

    /**
     * Whether [placement] may load and show ads: the master switch `is_show_ads` and the placement's own switch
     * `is_show_<code>` (KB-7, ADR-0014); for an app-wide placement, whether the keys of this open choose it
     * ([placementSwitch], A-01, A-02). Every ad format, including native ones added later, must check this before
     * loading or showing an ad.
     */
    internal fun isEnabled(placement: AdPlacement): Flow<Boolean> = placementSwitch(placement, flags, firstOpen)

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
     * The single-ad sources of native small slots (plan T-040), one per (placement, format), shut down when the
     * placement is switched off ([isEnabled]) or consent withdrawn ([canRequestAds]). Each load gets fresh metrics
     * events with the use the source gives it.
     */
    private val nativeSources by lazy {
        NativeAdSources(
            enabled = ::isEnabled,
            canRequestAds = canRequestAds,
            newLoad = { placement, format, use -> nativeLoadOf(placement, format, use) },
            sendGate = { awaitSendAllowed(foregroundState, ::mayRequestNow) },
            clock = clock,
            scope = scope,
            logger = logger,
        )
    }

    /**
     * The pools of native medium and in-collection placements (plan T-028), one per (format, ad unit), closed when
     * every placement of a pool is switched off ([isEnabled]) or consent withdrawn ([canRequestAds]). Every send goes
     * through the send gate of native ads ([awaitSendAllowed], L-072).
     */
    private val nativePools: NativeAdPools by lazy {
        var made = 0
        NativeAdPools(
            placements = pooledPlacements(),
            unitId = ::unitId,
            enabled = ::isEnabled,
            canRequestAds = canRequestAds,
            newPool = { key, placement ->
                made++
                NativeAdPool(
                    key = key,
                    capacity = requireNotNull(key.format.poolCapacity()),
                    load = { poolLoad(key, placement) },
                    sendGate = { awaitSendAllowed(foregroundState, ::mayRequestNow) },
                    canLoadNow = { _sdkReady.value && foregroundState.value && mayRequestNow() },
                    canShowNow = { canRequestAds.value },
                    retryResets = preloader.nativeRetryResets,
                    clock = clock,
                    scope = scope,
                    logger = logger,
                    logKey = "${key.format.code}#$made",
                )
            },
            scope = scope,
            logger = logger,
        )
    }

    /**
     * One load of a pool: a fresh [PlacementCell] for the placement of the ad, set when a cell claims it (P-04); the
     * metrics of the load report `pool` with no `load_attempt` (PL-02). Null when no ad came.
     */
    private suspend fun poolLoad(key: NativePoolKey, placement: AdPlacement): PoolLoad? {
        val size = key.format.nativeLoadSize() ?: return null
        val holder = PlacementCell()
        val state = nativeLoadOnce(
            newEvents = {
                NativeAdMetrics(
                    format = key.format,
                    unitId = key.unitId,
                    load = telemetry.nativeLoadReport(placement, NativeLoadUse.POOL),
                    placement = { holder.value },
                )
            },
            newLoader = { events -> SdkNativeAdLoader(context, adRequest(), events) },
            unitId = key.unitId,
            size = size,
            timeoutMillis = NATIVE_LOAD_TIMEOUT_MILLIS,
            logger = logger,
            placementKey = POOL_LOG_KEY,
        )()
        return (state as? NativeAdState.Loaded)?.let { PoolLoad(it.ad, holder) }
    }

    /**
     * The states of one cell of the pool of [placement] ([pooledCellStates]); Unavailable when the placement has no
     * pool (no ad unit). The caller checks consent and [isEnabled] first, like [NativeAdSlot].
     */
    internal fun pooledCell(placement: AdPlacement): Flow<NativeAdState> =
        nativePools.pool(placement)?.let { pooledCellStates(it, placement, logger, placement.key) }
            ?: flowOf(NativeAdState.Unavailable)

    /** The `ad_opportunity` of one cell of a pool (P-07). */
    internal fun recordPoolCellOpportunity(placement: AdPlacement, result: String) {
        telemetry.poolCellOpportunity(placement, result)
    }

    /**
     * The states of one native ad of [placement], drawn as [format], for a slot on screen: loading, then loaded, or
     * unavailable and a new try 5 s later (O-05), sent in foreground only and when [mayRequestNow]; Internet back cuts
     * the wait short (S-24). Each try waits at most [NATIVE_LOAD_TIMEOUT_MILLIS] and gets its own metrics events
     * (T-023). A native small slot ([usesSingleAdSource]) claims its ad from the source of its placement and replaces
     * it in place every 60 s in foreground, the first time [firstRefreshDelayMillis] after the first ad (plan T-040);
     * a pooled format (medium) is a cell of its pool ([pooledCell], plan T-028); other formats keep their ad, destroyed
     * when the collection is cancelled. The caller checks consent and [isEnabled] first, like [NativeAdSlot].
     */
    internal fun nativeAd(
        placement: AdPlacement,
        format: AdFormat = placement.format,
        firstRefreshDelayMillis: Long = NativeAdPolicy.REFRESH_INTERVAL_MILLIS,
    ): Flow<NativeAdState> {
        val size = requireNotNull(format.nativeLoadSize()) { "${placement.key} is not a native placement" }
        if (format == placement.format && format.usesPool) return pooledCell(placement)
        return flow {
            val unitId = unitId(placement)
            if (unitId == null) {
                emit(NativeAdState.Unavailable)
                return@flow
            }
            if (format.usesSingleAdSource()) {
                emitAll(
                    singleAdSlotStates(
                        source = nativeSources.source(placement, format),
                        foreground = foregroundState,
                        mayRequestNow = ::mayRequestNow,
                        retryResets = preloader.nativeRetryResets,
                        clock = clock,
                        logger = logger,
                        placementKey = nativeSlotKey(placement, format),
                        firstTickDelayMillis = firstRefreshDelayMillis,
                    ),
                )
                return@flow
            }
            emitAll(
                retryingNativeAdStates(
                    load = nativeLoadOnce(
                        newEvents = { nativeAdMetrics(placement, unitId, NativeLoadUse.SINGLE_AD, format) },
                        newLoader = { events -> SdkNativeAdLoader(context, adRequest(), events) },
                        unitId = unitId,
                        size = size,
                        timeoutMillis = NATIVE_LOAD_TIMEOUT_MILLIS,
                        logger = logger,
                        placementKey = placement.key,
                    ),
                    foreground = foregroundState,
                    mayRequestNow = ::mayRequestNow,
                    retryResets = preloader.nativeRetryResets,
                    logger = logger,
                    placementKey = placement.key,
                ),
            )
        }
    }

    /** One load of a single-ad source: [nativeLoadOnce] with fresh metrics events of [use] (L-A07). */
    private fun nativeLoadOf(
        placement: AdPlacement,
        format: AdFormat,
        use: NativeLoadUse,
    ): suspend () -> NativeAdState {
        val unitId = unitId(placement) ?: return { NativeAdState.Unavailable }
        val size = format.nativeLoadSize() ?: return { NativeAdState.Unavailable }
        return nativeLoadOnce(
            newEvents = { nativeAdMetrics(placement, unitId, use, format) },
            newLoader = { events -> SdkNativeAdLoader(context, adRequest(), events) },
            unitId = unitId,
            size = size,
            timeoutMillis = NATIVE_LOAD_TIMEOUT_MILLIS,
            logger = logger,
            placementKey = nativeSlotKey(placement, format),
        )
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
            SdkNativeAdLoader(context, adRequest(), nativeAdMetrics(placement, unitId, NativeLoadUse.LIST_ROW)),
            unitId,
            size,
            NATIVE_LOAD_TIMEOUT_MILLIS,
            logger,
            placement.key,
        )
    }

    /**
     * The metrics of one native load of [placement] used as [use]: the load report comes from [nativeLoadReport]
     * (a list row gets its own report on each call, so rows loading at once do not mix: question 6 of plan T-023),
     * drawn as [format].
     */
    private fun nativeAdMetrics(
        placement: AdPlacement,
        unitId: String,
        use: NativeLoadUse,
        format: AdFormat = placement.format,
    ): NativeAdEvents = NativeAdMetrics(
        format = format,
        unitId = unitId,
        load = telemetry.nativeLoadReport(placement, use, format),
        placement = { placement },
    )

    /**
     * Hands the SDK callbacks of one native ad to [telemetry]: load result, paid event, impression with the
     * zero-value fallback of T-02 (a timer in [scope], cancelled when the ad is destroyed), click.
     */
    private inner class NativeAdMetrics(
        private val format: AdFormat,
        private val unitId: String,
        private val load: LoadReport,
        private val placement: () -> AdPlacement?,
        private val onLoadFailed: (label: String) -> Unit = {},
    ) : NativeAdEvents {
        private val fallback = NativeRevenueFallback()
        private var fallbackJob: Job? = null

        @Volatile private var adSource: String? = null

        @Volatile private var adapterClassName: String? = null

        override fun onRequestSent() = load.sent(clock.nowMillis())

        override fun onLoaded(ad: NativeAd) {
            val adapter = ad.responseInfo?.loadedAdapterResponseInfo
            adSource = adapter?.adSourceName
            adapterClassName = adapter?.adapterClassName
            load.succeeded(clock.nowMillis())
        }

        override fun onFailed(code: Int, sdkMessage: String?) {
            onLoadFailed(load.failed(code, sdkMessage, clock.nowMillis()))
        }

        override fun onPaid(value: AdValue) {
            if (!fallback.onPaidEvent()) return
            fallbackJob?.cancel()
            telemetry.revenue(
                format,
                unitId,
                placement,
                value.valueMicros,
                value.currencyCode,
                adSource,
                adapterClassName,
            )
        }

        override fun onImpression() {
            val due = fallback.onImpression(clock.nowMillis())
            fallbackJob?.cancel()
            fallbackJob = scope.launch {
                delay((due - clock.nowMillis()).coerceAtLeast(0))
                if (fallback.onDue(clock.nowMillis())) {
                    telemetry.revenueFallback(format, unitId, placement, adSource, adapterClassName)
                }
            }
        }

        override fun onClicked() = telemetry.clicked(format, placement())

        override fun onDestroyed() {
            fallbackJob?.cancel()
        }
    }

    /** Personalization follows the consent string of UMP only (question 8 of the ads spec): no `npa` extra. */
    internal fun adRequest(): AdRequest = AdRequest.Builder().build()

    /** Preloads one full-screen ad of [placement] now; each loader checks the switches and the gate itself. */
    private fun preloadNow(placement: AdPlacement) {
        when (placement.format) {
            AdFormat.NATIVE_FULL_SCREEN -> preloadNativeFullScreen(placement)
            AdFormat.APP_OPEN -> preloadAppOpen(placement)
            else -> preloadInterstitial(placement)
        }
    }

    /**
     * Loads one native full-screen ad of [placement] for later, unless a fresh one is kept, one is loading or a retry
     * is not due (F-04), or the placement is off. Media of any aspect ratio, video muted, AdChoices in the top right
     * corner. Sent in foreground only; a failure retries by O-05 ([FullScreenReloads]).
     */
    private fun preloadNativeFullScreen(placement: AdPlacement) {
        // The gate is asked right before sending, below.
        if (!needsLoad(fullScreenLoadState(placement), clock.nowMillis(), gateOpen = true)) return
        val unitId = unitId(placement) ?: return
        loadStates[placement] = FullScreenLoadState.Loading
        scope.launch {
            // O-05, L-A19: a native request, the first one too, is never sent in background; the load is put off until
            // the app is in foreground again. The slot stays Loading meanwhile, so the plan and loadIfNeeded do not
            // start a second load (F-04); the gate and the switch are read once the wait is over.
            if (!foregroundState.value) logger.info(TAG, "Native full-screen ${placement.key} waits for foreground")
            foregroundState.first { it }
            if (!canRequestAds.value || !isEnabled(placement).first()) {
                loadStates -= placement
                return@launch
            }
            val cell = PlacementCell()
            val events = NativeAdMetrics(
                format = AdFormat.NATIVE_FULL_SCREEN,
                unitId = unitId,
                load = telemetry.countedLoad(
                    AdFormat.NATIVE_FULL_SCREEN,
                    preloadCounterKey(placement.key),
                    loadPlacementOf(AdFormat.NATIVE_FULL_SCREEN, placement),
                ),
                placement = { cell.value },
                onLoadFailed = { lastLoadErrors[placement] = it },
            )
            // Asked right before sending (S-15): a closed gate ends the round, no request, no retry.
            if (!mayRequestNow()) {
                loadStates[placement] = fullScreenReloads.onSendBlocked(placement)
                logger.info(TAG, "Native full-screen ${placement.key} not requested: consent gate closed")
                return@launch
            }
            try {
                // O-06, shared with every native request (NativeRequestOptions.kt).
                val options = fullScreenNativeRequestOptions().toNativeAdOptions()
                val loader = AdLoader.Builder(context, unitId)
                    .forNativeAd { ad ->
                        loadStates -= placement
                        lastLoadErrors -= placement
                        ad.setOnPaidEventListener { events.onPaid(it) }
                        events.onLoaded(ad)
                        val loaded = SdkLoadedNativeAd(ad, events)
                        nativeFullScreenCells[loaded] = cell
                        preloadedNativeFullScreen.put(placement, loaded, clock.nowMillis())
                    }
                    .withAdListener(
                        object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                logger.warn(TAG, "Native full-screen ${placement.key} failed to load: ${error.code}")
                                onNativeFullScreenLoadFailed(placement, events, error.code, error.message)
                            }

                            override fun onAdImpression() = events.onImpression()

                            override fun onAdClicked() = events.onClicked()
                        },
                    )
                    .withNativeAdOptions(options)
                    .build()
                val request = adRequest()
                events.onRequestSent()
                loader.loadAd(request)
            } catch (e: Exception) {
                // O-09: the request could not be built or sent; never the message of the error.
                logger.warn(TAG, "Native full-screen ${placement.key} failed to load: 0 (request not built)")
                onNativeFullScreenLoadFailed(placement, events, 0, null)
            }
        }
    }

    /** The load of the native full-screen ad of [placement] failed: counted (T-023), then a retry (O-05). */
    private fun onNativeFullScreenLoadFailed(
        placement: AdPlacement,
        events: NativeAdEvents,
        code: Int,
        message: String?,
    ) {
        loadStates -= placement
        events.onFailed(code, message)
        loadStates[placement] = fullScreenReloads.onLoadFailed(placement, code)
    }

    /**
     * Loads one interstitial of [placement] for later, unless one is kept, one is loading or a retry is not due
     * (F-04), or the placement is off. Loaded with the resumed activity of the app when there is one (question 4 of
     * plan T-034). A failure retries by F-05 ([FullScreenReloads]).
     */
    private fun preloadInterstitial(placement: AdPlacement) {
        // The gate is asked right before sending, below.
        if (!needsLoad(fullScreenLoadState(placement), clock.nowMillis(), gateOpen = true)) return
        val unitId = unitId(placement) ?: return
        loadStates[placement] = FullScreenLoadState.Loading
        scope.launch {
            if (!isEnabled(placement).first()) {
                loadStates -= placement
                return@launch
            }
            // Asked right before sending (S-15): a closed gate ends the round, no request, no retry.
            if (!mayRequestNow()) {
                loadStates[placement] = fullScreenReloads.onSendBlocked(placement)
                logger.info(TAG, "Interstitial ${placement.key} not requested: consent gate closed")
                return@launch
            }
            val slot = preloadCounterKey(placement.key)
            val loadPlacement = loadPlacementOf(AdFormat.INTERSTITIAL, placement)
            val callback = object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadStates -= placement
                    lastLoadErrors -= placement
                    telemetry.loadSucceeded(AdFormat.INTERSTITIAL, slot, loadPlacement, clock.nowMillis())
                    val cell = PlacementCell()
                    val adapter = ad.responseInfo.loadedAdapterResponseInfo
                    ad.setOnPaidEventListener { value ->
                        telemetry.revenue(
                            AdFormat.INTERSTITIAL,
                            unitId,
                            { cell.value },
                            value.valueMicros,
                            value.currencyCode,
                            adapter?.adSourceName,
                            adapter?.adapterClassName,
                        )
                    }
                    interstitialCells[ad] = cell
                    loadedInterstitials[placement] = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    logger.warn(TAG, "Interstitial ${placement.key} failed to load: ${error.code}")
                    onInterstitialLoadFailed(placement, slot, loadPlacement, error.code, error.message)
                }
            }
            try {
                val request = adRequest()
                telemetry.requestSent(slot, clock.nowMillis())
                InterstitialAd.load(fullScreenLoadContext(tracker?.resumedActivity, context), unitId, request, callback)
            } catch (e: Exception) {
                // O-09: the request could not be built or sent; never the message of the error.
                logger.warn(TAG, "Interstitial ${placement.key} failed to load: 0 (request not built)")
                onInterstitialLoadFailed(placement, slot, loadPlacement, 0, null)
            }
        }
    }

    /**
     * Loads one app open ad of [placement] for the app open slot, unless one is kept, one is loading or a retry is
     * not due (F-04), or the placement is off; like [preloadInterstitial]. Its paid events carry the placement it is
     * shown for (PL-03). A failure retries by F-05 ([FullScreenReloads]).
     */
    private fun preloadAppOpen(placement: AdPlacement) {
        // The gate is asked right before sending, below.
        if (!needsLoad(fullScreenLoadState(placement), clock.nowMillis(), gateOpen = true)) return
        val unitId = unitId(placement) ?: return
        loadStates[placement] = FullScreenLoadState.Loading
        scope.launch {
            if (!isEnabled(placement).first()) {
                loadStates -= placement
                return@launch
            }
            // Asked right before sending (S-15): a closed gate ends the round, no request, no retry.
            if (!mayRequestNow()) {
                loadStates[placement] = fullScreenReloads.onSendBlocked(placement)
                logger.info(TAG, "App open ${placement.key} not requested: consent gate closed")
                return@launch
            }
            val slot = preloadCounterKey(placement.key)
            val loadPlacement = loadPlacementOf(AdFormat.APP_OPEN, placement)
            val callback = object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    loadStates -= placement
                    lastLoadErrors -= placement
                    telemetry.loadSucceeded(AdFormat.APP_OPEN, slot, loadPlacement, clock.nowMillis())
                    val cell = PlacementCell()
                    val adapter = ad.responseInfo.loadedAdapterResponseInfo
                    ad.setOnPaidEventListener { value ->
                        telemetry.revenue(
                            AdFormat.APP_OPEN,
                            unitId,
                            { cell.value },
                            value.valueMicros,
                            value.currencyCode,
                            adapter?.adSourceName,
                            adapter?.adapterClassName,
                        )
                    }
                    appOpenCells[ad] = cell
                    loadedAppOpens.put(placement, ad, clock.nowMillis())
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    logger.warn(TAG, "App open ${placement.key} failed to load: ${error.code}")
                    onFullScreenLoadFailed(AdFormat.APP_OPEN, placement, slot, loadPlacement, error.code, error.message)
                }
            }
            try {
                val request = adRequest()
                telemetry.requestSent(slot, clock.nowMillis())
                AppOpenAd.load(fullScreenLoadContext(tracker?.resumedActivity, context), unitId, request, callback)
            } catch (e: Exception) {
                // O-09: the request could not be built or sent; never the message of the error.
                logger.warn(TAG, "App open ${placement.key} failed to load: 0 (request not built)")
                onFullScreenLoadFailed(AdFormat.APP_OPEN, placement, slot, loadPlacement, 0, null)
            }
        }
    }

    /** A load of the interstitial of [placement] failed: counted first (T-023), then a retry (F-05). */
    private fun onInterstitialLoadFailed(
        placement: AdPlacement,
        slot: String,
        loadPlacement: String?,
        code: Int,
        message: String?,
    ) = onFullScreenLoadFailed(AdFormat.INTERSTITIAL, placement, slot, loadPlacement, code, message)

    /** A load of the interstitial or app open ad of [placement] failed: counted first (T-023), then a retry (F-05). */
    private fun onFullScreenLoadFailed(
        format: AdFormat,
        placement: AdPlacement,
        slot: String,
        loadPlacement: String?,
        code: Int,
        message: String?,
    ) {
        loadStates -= placement
        lastLoadErrors[placement] = telemetry.loadFailed(
            format,
            slot,
            loadPlacement,
            code,
            message,
            clock.nowMillis(),
        )
        loadStates[placement] = fullScreenReloads.onLoadFailed(placement, code)
    }

    private companion object {
        const val TAG = "Ads"

        /** `load_state` of a shown full-screen ad. */
        const val READY = "ready"

        /**
         * The global flags read by the metrics before [AdsFlags.global] first emits: the defaults of T-019, so both
         * lifecycle switches off (nothing is served then anyway, S-19).
         */
        val DEFAULT_GLOBAL_FLAGS = AdsGlobalFlags(
            adsEnabled = AdsSettings.IS_SHOW_ADS.default,
            showOpenAds = AdsSettings.IS_SHOW_ADS.default && AdsSettings.SHOW_OPEN_ADS.default,
            showOpenAdsFirstOpen = AdsSettings.IS_SHOW_ADS.default && AdsSettings.SHOW_OPEN_ADS_FIRST_OPEN.default,
            openAdType = OpenAdType.of(AdsSettings.OPEN_AD_TYPE.default),
            showResumeAds = AdsSettings.IS_SHOW_ADS.default && AdsSettings.SHOW_RESUM_ADS.default,
            resumeAdType = OpenAdType.of(AdsSettings.RESUME_AD_TYPE.default),
            adsIntervalSeconds = AdsSettings.ADS_INTERVAL.default.coerceAtLeast(0),
            showNativeAfterInter = AdsSettings.IS_SHOW_ADS.default && AdsSettings.IS_SHOW_NATIVE_AFTER_INTER.default,
            showNativeBottom = AdsSettings.IS_SHOW_NATIVE_BOTTOM.default,
            showNativeCollap = AdsSettings.IS_SHOW_NATIVE_BOTTOM.default && AdsSettings.IS_SHOW_NATIVE_COLLAP.default,
            expandCollapAfterSeconds = AdsSettings.TIME_EXPAND_COLLAP_AD.default.coerceAtLeast(0),
            consentShortcutEnabled = AdsSettings.IS_ENABLE_CONSENT_SHORTCUT.default,
            logFullScreenEvents = AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS.default,
            logSlotEvents = AdsSettings.IS_LOG_AD_SLOT_EVENTS.default,
        )

        /** Host of a full-screen ad drawn by the composition; `System.identityHashCode` gives 0 only for null. */
        const val NO_HOST_ACTIVITY = 0

        /** Time to wait for a native ad before its space is removed (kept from wave 2 until T-027). */
        const val NATIVE_LOAD_TIMEOUT_MILLIS = 10_000L

        /** Log key of the loads of a pool: never the ad unit (ADR-0019). */
        const val POOL_LOG_KEY = "pool"
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
    // App open (its own slot, T-024) and rewarded are not native. The pool of T-028 loads the in-collection cards
    // with portrait media; the native step waits for its layout (T-032).
    AdFormat.APP_OPEN -> null
    AdFormat.REWARDED -> null
    AdFormat.BANNER_COLLAPSIBLE -> null
    AdFormat.NATIVE_STEP -> null
    AdFormat.NATIVE_IN_COLLECTION -> NativeAdSize.CARD
}
