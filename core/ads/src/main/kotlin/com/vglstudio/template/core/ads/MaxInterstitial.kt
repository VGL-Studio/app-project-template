package com.vglstudio.template.core.ads

import android.app.Activity
import android.content.Context
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxInterstitialAd

/** The callbacks of a MAX interstitial, as [MaxInterstitialSlot] and `AdsController` read them (plan T-069). */
internal interface MaxInterstitialListener {
    fun onLoaded()

    fun onLoadFailed(code: Int)

    fun onDisplayed()

    fun onHidden()

    fun onDisplayFailed(code: Int)

    fun onClicked()
}

/** The interstitial of AppLovin MAX as :core:ads uses it (plan T-069); a fake in tests. One object per ad unit. */
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
