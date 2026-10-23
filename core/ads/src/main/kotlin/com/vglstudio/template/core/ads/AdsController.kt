package com.vglstudio.template.core.ads

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.vglstudio.template.core.config.RemoteSettings
import com.vglstudio.template.core.logging.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The only class that talks to the ads SDKs (ADR-0004). :app calls [gatherConsent], [onUserInteraction],
 * [onAppForeground] and [onAppBackground] from its activity and provides this controller to the UI through
 * [LocalAdsController]. Features use [AdSlot], [NativeAdSlot], [rememberShowInterstitial] and [PrivacyOptionsButton],
 * never this class or an SDK directly.
 *
 * The ads SDK is initialized only once the consent form allows ads to be requested. Mediation adapters (AppLovin,
 * plan T-015) are initialized by the SDK at the same time, with no code of their own; their status is logged.
 */
@Singleton
class AdsController @Inject internal constructor(
    @param:ApplicationContext private val context: Context,
    private val unitIds: AdUnitIds,
    private val consentDebug: ConsentDebugOptions,
    private val settings: RemoteSettings,
    historyStore: FullScreenAdHistoryStore,
    internal val logger: AppLogger,
) {
    private val consentInformation by lazy { UserMessagingPlatform.getConsentInformation(context) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val initialized = AtomicBoolean(false)
    private val firstOpenRecorded = AtomicBoolean(false)

    // Touched on the main thread only.
    private val guard = FullScreenAdGuard()
    private val pacer = FullScreenAdPacer(historyStore, SystemAdClock, logger)
    private val startedActivities = StartedActivityCounter()
    private val loadedInterstitials = mutableMapOf<AdPlacement, InterstitialAd>()
    private var fullScreenInProgress = false

    private val _canRequestAds = MutableStateFlow(false)

    /** True once consent allows ads and the SDK is started. */
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)

    /**
     * True when the user must be able to reopen the consent form ([PrivacyOptionsButton]).
     * This is also how the module knows the user is in a region where the law requires consent.
     */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /**
     * Updates the consent status and shows the consent form when required. Call it in the activity's
     * `onCreate` at every start. Consent given in an earlier session lets ads start while the update runs.
     */
    fun gatherConsent(activity: Activity) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            consentParameters(activity),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    error?.let { Log.w(TAG, "Consent form failed: ${it.errorCode} ${it.message}") }
                    onConsentFlowFinished()
                }
            },
            { error ->
                Log.w(TAG, "Consent update failed: ${error.errorCode} ${error.message}")
                onConsentFlowFinished()
            },
        )
        onConsentUpdated()
    }

    /** Opens the privacy options form, so the user can change their consent at any time. */
    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            error?.let { Log.w(TAG, "Privacy options form failed: ${it.errorCode} ${it.message}") }
            onConsentUpdated()
        }
    }

    /** Call from `Activity.onUserInteraction`: full-screen ads wait until the user stops touching the screen. */
    fun onUserInteraction() {
        guard.onUserInteraction(SystemClock.elapsedRealtime())
    }

    /**
     * Call from `Activity.onStart`. A new ads session starts when the app comes back after a long time in
     * background; switching between activities of the app or rotating the screen keeps the session.
     */
    fun onAppForeground() {
        if (startedActivities.onStart()) pacer.onForeground()
    }

    /** Call from `Activity.onStop`. */
    fun onAppBackground() {
        if (startedActivities.onStop()) pacer.onBackground()
    }

    /**
     * Shows the full-screen ad of [placement] if the guardrails allow it, and returns whether it was shown.
     * Call it at a natural break (end of a level, after saving), never when the app opens or closes.
     */
    suspend fun showInterstitial(activity: Activity, placement: AdPlacement): Boolean =
        withContext(Dispatchers.Main.immediate) {
            require(placement.format == AdFormat.INTERSTITIAL) { "${placement.key} is not a full-screen placement" }
            // A second call while the first one reads the history or waits is dropped.
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
                    override fun onAdDismissedFullScreenContent() = preloadInterstitial(placement)

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Log.w(TAG, "Interstitial ${placement.key} failed to show: ${error.message}")
                        preloadInterstitial(placement)
                    }
                }
                // Counted before showing: an ad that fails to show still counts (fewer ads, never more).
                onFullScreenShown()
                ad.show(activity)
                true
            } finally {
                fullScreenInProgress = false
            }
        }

    /**
     * Whether a full-screen ad of [placement] may be shown now, shared by every full-screen format: consent,
     * [isEnabled], the frequency rules kept on the device ([FullScreenAdPacer]), then the in-memory guardrails
     * ([FullScreenAdGuard]), waiting a little while the user is touching the screen. When it returns true, the
     * caller shows the ad and calls [onFullScreenShown] right before.
     */
    internal suspend fun awaitFullScreenTurn(placement: AdPlacement): Boolean =
        withContext(Dispatchers.Main.immediate) {
            if (!canRequestAds.value || !isEnabled(placement).first()) return@withContext false
            val limits = fullScreenAdLimits(settings).first()
            if (pacer.check(limits) != FullScreenAdVerdict.Allowed) return@withContext false
            val deadline = SystemClock.elapsedRealtime() + FullScreenAdGuard.MAX_WAIT_MILLIS
            while (true) {
                val now = SystemClock.elapsedRealtime()
                when (val decision = guard.decide(now, limits.minIntervalMillis)) {
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
        scope.launch { pacer.onShown() }
    }

    /**
     * Whether [placement] may load and show ads: the master switch `ads_enabled` and the placement's own switch
     * (KB-7, ADR-0014). Every ad format, including native ones added later, must check this before loading or
     * showing an ad.
     */
    internal fun isEnabled(placement: AdPlacement): Flow<Boolean> = placementActive(settings, placement)

    /**
     * N of the in-list ads of [placement] ([rememberInListAds]): `ads_inlist_every_count`, or null while no ad row may
     * be added (no consent yet, master switch or placement off, bad remote value).
     */
    internal fun inListEvery(placement: AdPlacement): Flow<Int?> = combine(
        canRequestAds,
        isEnabled(placement),
        settings.value(AdsSettings.INLIST_EVERY_COUNT),
    ) { canRequest, enabled, everyCount -> inListEvery(canRequest && enabled, everyCount) }
        .distinctUntilChanged()

    internal fun unitId(placement: AdPlacement): String? = unitIds.of(placement).also {
        if (it == null) Log.e(TAG, "No ad unit for placement ${placement.key}; it shows no ad")
    }

    /**
     * The states of one native ad of [placement] for a slot on screen ([nativeAdStates]): loading, then loaded or
     * unavailable after `ads_native_load_timeout_seconds`. The ad is destroyed when the collection is cancelled.
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
            emitAll(
                nativeAdStates(
                    SdkNativeAdLoader(context, adRequest()),
                    unitId,
                    size,
                    nativeLoadTimeoutMillis(),
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
        return loadOneNativeAd(
            SdkNativeAdLoader(context, adRequest()),
            unitId,
            size,
            nativeLoadTimeoutMillis(),
            logger,
            placement.key,
        )
    }

    private suspend fun nativeLoadTimeoutMillis(): Long =
        settings.value(AdsSettings.NATIVE_LOAD_TIMEOUT_SECONDS).first().coerceIn(0L, Long.MAX_VALUE / 1_000) * 1_000

    internal suspend fun adRequest(): AdRequest {
        val nonPersonalized = requestsNonPersonalizedAds(
            inRegulatedRegion = privacyOptionsRequired.value,
            personalizedInRegulatedRegions = settings.value(AdsSettings.PERSONALIZED_IN_REGULATED_REGIONS).first(),
        )
        val builder = AdRequest.Builder()
        if (nonPersonalized) {
            builder.addNetworkExtrasBundle(AdMobAdapter::class.java, Bundle().apply { putString("npa", "1") })
        }
        return builder.build()
    }

    private fun consentParameters(activity: Activity): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        if (consentDebug.forceEea) {
            builder.setConsentDebugSettings(
                ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .setForceTesting(true)
                    .build(),
            )
        }
        return builder.build()
    }

    /**
     * The consent flow of this start is over (form closed or not needed, or the update failed). The first time in
     * the process, saves the first app open if none is saved yet: the grace period counts from opening the app,
     * whatever the user chose and whichever activity asked for consent.
     */
    private fun onConsentFlowFinished() {
        onConsentUpdated()
        if (firstOpenRecorded.compareAndSet(false, true)) scope.launch { pacer.recordFirstOpen() }
    }

    private fun onConsentUpdated() {
        _privacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus == PrivacyOptionsRequirementStatus.REQUIRED
        if (consentInformation.canRequestAds() && initialized.compareAndSet(false, true)) {
            scope.launch {
                // Google recommends starting the SDK off the main thread.
                withContext(Dispatchers.IO) {
                    // The listener runs once every adapter is initialized or timed out (KB-9).
                    MobileAds.initialize(context) { status ->
                        adapterStatusLines(status.adapterStatusMap).forEach { logger.info(TAG, it) }
                    }
                }
                _canRequestAds.value = true
                AdPlacement.entries.filter { it.format == AdFormat.INTERSTITIAL }.forEach(::preloadWhenEnabled)
            }
        }
    }

    /** Placements are off by default (ADR-0014): load an ad only once the placement is switched on remotely. */
    private fun preloadWhenEnabled(placement: AdPlacement) {
        scope.launch {
            isEnabled(placement).distinctUntilChanged().filter { it }.collect { preloadInterstitial(placement) }
        }
    }

    private fun preloadInterstitial(placement: AdPlacement) {
        val unitId = unitId(placement) ?: return
        scope.launch {
            if (placement in loadedInterstitials || !isEnabled(placement).first()) return@launch
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
    }
}

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
}
