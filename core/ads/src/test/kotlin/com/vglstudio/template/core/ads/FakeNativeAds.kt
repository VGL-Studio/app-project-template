package com.vglstudio.template.core.ads

/** The illustration texts of the design, used by every native ad test. */
internal val sampleNativeAdAssets = NativeAdAssets(
    headline = "Headline of the ad",
    advertiser = "Advertiser or short body",
    body = "Body text of the ad, two lines at most so the call to action stays in view.",
    callToAction = "Install",
    icon = null,
    hasMedia = true,
)

/** A loaded ad that counts how many times it was destroyed. */
internal class FakeLoadedNativeAd(override val assets: NativeAdAssets = sampleNativeAdAssets) : LoadedNativeAd {
    var destroyCount = 0
        private set

    override fun destroy() {
        destroyCount++
    }
}

/** A loader whose requests stay open until the test answers them with [deliver] or [fail]. */
internal class FakeNativeAdLoader : NativeAdLoader {
    class Request(
        val unitId: String,
        val size: NativeAdSize,
        val onLoaded: (LoadedNativeAd) -> Unit,
        val onFailed: (Int) -> Unit,
    )

    val requests = mutableListOf<Request>()

    override fun load(
        unitId: String,
        size: NativeAdSize,
        onLoaded: (LoadedNativeAd) -> Unit,
        onFailed: (code: Int) -> Unit,
    ) {
        requests += Request(unitId, size, onLoaded, onFailed)
    }

    /** Answers the last request with [ad]. */
    fun deliver(ad: LoadedNativeAd) = requests.last().onLoaded(ad)

    /** Answers the last request with an SDK error [code]. */
    fun fail(code: Int) = requests.last().onFailed(code)
}
