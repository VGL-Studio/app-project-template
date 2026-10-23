package com.vglstudio.template.core.ads

import android.content.Context
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MediaAspectRatio
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

/** Loads native ads with the Google Mobile Ads SDK. Main thread only, like the SDK. */
internal class SdkNativeAdLoader(private val context: Context, private val request: AdRequest) : NativeAdLoader {
    override fun load(
        unitId: String,
        size: NativeAdSize,
        onLoaded: (LoadedNativeAd) -> Unit,
        onFailed: (code: Int) -> Unit,
    ) {
        val options = NativeAdOptions.Builder()
            // The layouts keep the top right corner free for AdChoices.
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .apply {
                when (size) {
                    NativeAdSize.SMALL -> setRequestMultipleImages(false)
                    NativeAdSize.MEDIUM -> setMediaAspectRatio(MediaAspectRatio.ANY)
                }
            }
            .build()
        AdLoader.Builder(context, unitId)
            .forNativeAd { ad -> onLoaded(SdkLoadedNativeAd(ad)) }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) = onFailed(error.code)
                },
            )
            .withNativeAdOptions(options)
            .build()
            .loadAd(request)
    }
}

/** A native ad from the SDK; [nativeAd] is bound to the NativeAdView by [BoundNativeAd]. */
internal class SdkLoadedNativeAd(val nativeAd: NativeAd) : LoadedNativeAd {
    override val assets = NativeAdAssets(
        headline = nativeAd.headline.orEmpty(),
        advertiser = nativeAd.advertiser,
        body = nativeAd.body,
        callToAction = nativeAd.callToAction,
        // A drawable without its own size (a plain color) cannot become a bitmap: the icon block is drawn instead.
        icon = nativeAd.icon?.drawable?.let { runCatching { it.toBitmap().asImageBitmap() }.getOrNull() },
        hasMedia = nativeAd.mediaContent != null,
    )

    override fun destroy() = nativeAd.destroy()
}
