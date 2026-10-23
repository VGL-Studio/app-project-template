package com.vglstudio.template.core.ads

import android.app.Activity
import androidx.compose.runtime.Composable

/** Returns an action that shows the full-screen ad of [placement] (interstitial or native full screen). */
@Composable
fun rememberShowFullScreenAd(placement: AdPlacement): () -> Unit = {}

@Composable
internal fun rememberFullScreenAdAction(
    placement: AdPlacement,
    show: suspend (AdsController, Activity, AdPlacement) -> Unit,
): () -> Unit = {}
