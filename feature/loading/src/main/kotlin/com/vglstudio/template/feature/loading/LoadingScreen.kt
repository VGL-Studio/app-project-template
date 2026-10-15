package com.vglstudio.template.feature.loading

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun LoadingScreen(uiState: LoadingUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {}
}

internal object LoadingTestTags {
    const val SCREEN = "loading_screen"
    const val PROGRESS = "loading_progress"
    const val PERCENT = "loading_percent"
    const val ERROR = "loading_error"
}
