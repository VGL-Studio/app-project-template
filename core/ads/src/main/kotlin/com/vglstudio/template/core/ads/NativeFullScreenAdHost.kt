package com.vglstudio.template.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Draws the native full-screen ad of the running app over the current screen, in a full-screen dialog: the screen
 * below keeps its state, and closing the ad returns to it (KB-5). :app places it once, inside its theme, after the
 * navigation host. It draws nothing in tests and previews (no [LocalAdsController]).
 */
@Composable
fun FullScreenAdHost() {
    val ads = LocalAdsController.current ?: return
    val shown by ads.nativeFullScreenAd.collectAsStateWithLifecycle()
    val current = shown ?: return
    NativeFullScreenAdDialog(onClose = ads::closeNativeFullScreenAd) {
        BoundNativeAd(current.ad, Modifier.fillMaxSize()) { binder ->
            NativeFullScreenAdLayout(
                assets = current.ad.assets,
                onClose = ads::closeNativeFullScreenAd,
                binder = binder,
                // The SDK draws AdChoices in the top right corner itself; the adChoices slot stays empty.
                media = { binder.Media(Modifier.fillMaxSize()) },
            )
        }
    }
}

/** The back button closes the ad; a touch outside means nothing (the dialog covers the whole window). */
internal fun nativeFullScreenDialogProperties(): DialogProperties = DialogProperties(
    dismissOnBackPress = true,
    dismissOnClickOutside = false,
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false,
)

/**
 * A dialog over the whole window. It is its own window with its own back dispatcher, so the back button reaches
 * [onClose], not the activity below.
 */
@Composable
internal fun NativeFullScreenAdDialog(onClose: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = nativeFullScreenDialogProperties()) {
        Box(modifier = Modifier.fillMaxSize()) { content() }
    }
}
