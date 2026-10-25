package com.vglstudio.template.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** One banner of the SDK, as the slot sees it. */
internal interface BannerAdView {
    /** Results of the loads, sent by the app or refreshed by the SDK; no old result is replayed. */
    val results: Flow<BannerLoadResult>

    fun send()

    fun pause()

    fun resume()

    fun destroy()

    @Composable
    fun Content(showAd: Boolean, modifier: Modifier)
}

@Suppress("UNUSED_PARAMETER")
internal fun bannerAdRequest(refresh: BannerRefresh, requestId: () -> String): AdRequest = AdRequest.Builder().build()

@Suppress("UNUSED_PARAMETER", "unused")
internal class SdkBannerAdView(
    private val adView: AdView,
    private val refresh: BannerRefresh,
    private val placement: AdPlacement,
    private val unitId: String,
    private val telemetry: AdTelemetry,
    private val now: () -> Long,
    private val load: (AdRequest) -> Unit = adView::loadAd,
    private val newRequestId: () -> String = { UUID.randomUUID().toString() },
) : BannerAdView {
    override val results: Flow<BannerLoadResult> = emptyFlow()

    override fun send() = Unit

    override fun pause() = Unit

    override fun resume() = Unit

    override fun destroy() = Unit

    @Composable
    override fun Content(showAd: Boolean, modifier: Modifier) = Unit
}
