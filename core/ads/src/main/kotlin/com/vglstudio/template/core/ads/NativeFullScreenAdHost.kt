package com.vglstudio.template.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

/** Draws the native full-screen ad of the running app; place it once in :app. */
@Composable
fun FullScreenAdHost() = Unit

internal fun nativeFullScreenDialogProperties(): DialogProperties = DialogProperties()

@Composable
internal fun NativeFullScreenAdDialog(onClose: () -> Unit, content: @Composable () -> Unit) {
    content()
}
