package com.vglstudio.template.core.ads

import android.app.Activity

/**
 * The interstitial of AppLovin MAX for tests (plan T-069): keeps every call in [calls] in order and the activities
 * it was shown on, and answers through its listener on command ([emitLoaded], [emitLoadFailed] and the others).
 */
internal class FakeMaxInterstitialSdkAd(val adUnitId: String) : MaxInterstitialSdkAd {
    val calls = mutableListOf<String>()
    val shownOn = mutableListOf<Activity>()

    override var listener: MaxInterstitialListener? = null

    val loadCount: Int get() = calls.count { it == LOAD }
    val showCount: Int get() = calls.count { it == SHOW }

    override fun load() {
        calls += LOAD
    }

    override fun show(activity: Activity) {
        calls += SHOW
        shownOn += activity
    }

    override fun destroy() {
        calls += DESTROY
    }

    fun emitLoaded() = listener?.onLoaded()

    fun emitLoadFailed(code: Int) = listener?.onLoadFailed(code)

    fun emitDisplayed() = listener?.onDisplayed()

    fun emitHidden() = listener?.onHidden()

    fun emitDisplayFailed(code: Int) = listener?.onDisplayFailed(code)

    fun emitClicked() = listener?.onClicked()

    companion object {
        const val LOAD = "load"
        const val SHOW = "show"
        const val DESTROY = "destroy"
    }
}
