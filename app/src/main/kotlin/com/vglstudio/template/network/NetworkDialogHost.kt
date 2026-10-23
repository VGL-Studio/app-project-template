package com.vglstudio.template.network

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vglstudio.template.core.network.NetworkDialogState

/** Skeleton for the tests written first (T-017, step 3): draws only [content]. */
@Composable
fun NetworkDialogHost(
    state: NetworkDialogState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        content()
    }
}
