package com.vglstudio.template.core.ads

import android.content.Context
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd

/**
 * The SDK callbacks of one native ad that the ad metrics read (plan T-023). Only reads data of the SDK; every
 * decision is made by [AdTelemetry].
 */
internal interface NativeAdEvents {
    fun onRequestSent()

    fun onLoaded(ad: NativeAd)

    /** [sdkMessage] is read on the device only, never sent (section 8.2). */
    fun onFailed(code: Int, sdkMessage: String?)

    fun onPaid(value: AdValue)

    fun onImpression()

    fun onClicked()

    /** The ad is destroyed: the zero-value fallback of T-02 is cancelled. */
    fun onDestroyed()
}

/**
 * Loads native ads with the Google Mobile Ads SDK. Main thread only, like the SDK. [events] gets the callbacks the
 * ad metrics read; one loader loads one ad.
 */
internal class SdkNativeAdLoader(
    private val context: Context,
    private val request: AdRequest,
    private val events: NativeAdEvents? = null,
) : NativeAdLoader {
    override fun load(
        unitId: String,
        size: NativeAdSize,
        onLoaded: (LoadedNativeAd) -> Unit,
        onFailed: (code: Int) -> Unit,
    ) {
        val options = nativeRequestOptions(size).toNativeAdOptions()
        val loader = AdLoader.Builder(context, unitId)
            .forNativeAd { ad ->
                events?.let { metrics ->
                    // Attached at load (T-01), before the ad can be shown.
                    ad.setOnPaidEventListener { metrics.onPaid(it) }
                    metrics.onLoaded(ad)
                }
                onLoaded(SdkLoadedNativeAd(ad, events))
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        events?.onFailed(error.code, error.message)
                        onFailed(error.code)
                    }

                    override fun onAdImpression() {
                        events?.onImpression()
                    }

                    override fun onAdClicked() {
                        events?.onClicked()
                    }
                },
            )
            .withNativeAdOptions(options)
            .build()
        events?.onRequestSent()
        loader.loadAd(request)
    }
}

/** A native ad from the SDK; [nativeAd] is bound to the NativeAdView by [BoundNativeAd]. */
internal class SdkLoadedNativeAd(val nativeAd: NativeAd, private val events: NativeAdEvents? = null) :
    LoadedNativeAd {
    override val assets = NativeAdAssets(
        headline = nativeAd.headline.orEmpty(),
        advertiser = nativeAd.advertiser,
        body = nativeAd.body,
        callToAction = nativeAd.callToAction,
        // A drawable without its own size (a plain color) cannot become a bitmap: the icon block is drawn instead.
        icon = nativeAd.icon?.drawable?.let { runCatching { it.toBitmap().asImageBitmap() }.getOrNull() },
        hasMedia = nativeAd.mediaContent != null,
        starRating = nativeAd.starRating,
    )

    override fun destroy() {
        events?.onDestroyed()
        nativeAd.destroy()
    }
}
