package com.vglstudio.template.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** A native ad that covers the whole screen. */
@Composable
fun NativeFullScreenAdScreen(
    assets: NativeAdAssets,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    media: @Composable BoxScope.() -> Unit = {},
    adChoices: @Composable () -> Unit = {},
) {
    Box(modifier = modifier)
}
