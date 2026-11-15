package com.vglstudio.template.core.ads

import android.app.Activity
import android.content.Context
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxInterstitialAd

/**
 * The callbacks of a MAX interstitial (plan T-069), rewarded (plan T-106) or app open ad (plan T-070), as
 * [MaxInterstitialSlot] and `AdsController` read them.
 */
internal interface MaxInterstitialListener {
    fun onLoaded()

    fun onLoadFailed(code: Int)

    fun onDisplayed()

    fun onHidden()

    fun onDisplayFailed(code: Int)

    fun onClicked()

    /** R-02: the user earned the reward of a rewarded ad (plan T-106). */
    fun onRewarded() {}
}

/**
 * The interstitial (plan T-069), rewarded (plan T-106) or app open ad (plan T-070) of AppLovin MAX as :core:ads uses
 * it; a fake in tests. Rewarded: one object per ad unit, as the SDK keeps it; interstitial and app open: one object
 * per build, kept by the slot until it drops it.
 */
internal interface MaxInterstitialSdkAd {
    var listener: MaxInterstitialListener?

    fun load()

    fun show(activity: Activity)

    fun destroy()
}

/**
 * The real `MaxInterstitialAd`. Built only once MAX is ready (the SDK is initialized), on the first load of the slot,
 * so the constructor never meets an uninitialized SDK (plan T-071, section 4.1).
 */
internal class AppLovinMaxInterstitial(adUnitId: String, context: Context) : MaxInterstitialSdkAd {
    private val ad = MaxInterstitialAd(adUnitId, context)

    override var listener: MaxInterstitialListener? = null

    init {
        ad.setListener(
            object : MaxAdListener {
                override fun onAdLoaded(maxAd: MaxAd) {
                    listener?.onLoaded()
                }

                override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                    listener?.onLoadFailed(error.code)
                }

                override fun onAdDisplayed(maxAd: MaxAd) {
                    listener?.onDisplayed()
                }

                override fun onAdHidden(maxAd: MaxAd) {
                    listener?.onHidden()
                }

                override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                    listener?.onDisplayFailed(error.code)
                }

                override fun onAdClicked(maxAd: MaxAd) {
                    listener?.onClicked()
                }
            },
        )
    }

    override fun load() = ad.loadAd()

    override fun show(activity: Activity) = ad.showAd(activity)

    override fun destroy() = ad.destroy()
}
