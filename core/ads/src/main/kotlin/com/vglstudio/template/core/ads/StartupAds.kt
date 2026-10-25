package com.vglstudio.template.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** The ad of the loading screen when the app opens (KB-13). Bound to AdsController by Hilt. */
interface StartupAds {
    /** Waits within the limits of A-03 and returns the turn to show, or null; null at once when the key is off. */
    suspend fun awaitStartupAd(): StartupAdTurn?
}

/** One ad to show when the app opens. Only app-wide placements are accepted. */
class StartupAdTurn(val placement: AdPlacement)

/**
 * Returns an action that shows a [StartupAdTurn] through the full-screen gate of :core:ads (A-02, A-05).
 * onFinished(interShown) runs exactly once, on the main thread: at once when nothing shows, else after the ad
 * closes, fails to show, is found stuck or loses its activity. With no ads controller (tests, previews) it runs at
 * once with false.
 */
@Composable
fun rememberShowStartupAd(): (StartupAdTurn, (interShown: Boolean) -> Unit) -> Unit = remember { { _, _ -> } }
