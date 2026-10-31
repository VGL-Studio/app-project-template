package com.vglstudio.template.core.ads

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

// Stubs of T-071 (step 3 of docs/auto-run.md): the tests compile and fail on their assertions.

internal interface MaxBannerSdkView {
    val view: View

    fun setCallbacks(onLoaded: () -> Unit, onFailed: (code: Int) -> Unit, onClicked: () -> Unit)

    fun loadAd()

    fun startAutoRefresh()

    fun stopAutoRefresh()

    fun destroy()
}

internal class AppLovinMaxBannerView(unitId: String, kind: BannerSizeKind, widthDp: Int, context: Context) :
    MaxBannerSdkView {
    override val view: View = View(context)

    override fun setCallbacks(onLoaded: () -> Unit, onFailed: (code: Int) -> Unit, onClicked: () -> Unit) = Unit

    override fun loadAd() = Unit

    override fun startAutoRefresh() = Unit

    override fun stopAutoRefresh() = Unit

    override fun destroy() = Unit
}

internal fun maxBannerHeightDp(context: Context, kind: BannerSizeKind, widthDp: Int): Int = 0

internal class MaxBannerAdView(
    private val newSdkView: () -> MaxBannerSdkView,
    private val placement: AdPlacement,
    private val format: AdFormat,
    private val telemetry: AdTelemetry,
    private val now: () -> Long,
    private val logger: AppLogger,
) : BannerAdView {
    override val results: Flow<BannerLoadResult> = MutableSharedFlow()

    override fun send() = Unit

    override fun pause() = Unit

    override fun resume() = Unit

    override fun destroy() = Unit

    @Composable
    override fun Content(showAd: Boolean, modifier: Modifier) = Unit
}
