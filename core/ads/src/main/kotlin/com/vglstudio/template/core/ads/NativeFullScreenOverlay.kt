package com.vglstudio.template.core.ads

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

// Skeleton of plan T-026, step 3: draws nothing.

@Composable
internal fun NativeFullScreenOverlayContent(view: NativeFullScreenView, onClose: () -> Unit, animated: Boolean) {}

@Composable
internal fun NativeFullScreenOverlayHost(
    view: StateFlow<NativeFullScreenView?>,
    onClose: () -> Unit,
    onBack: () -> Unit,
) {}
