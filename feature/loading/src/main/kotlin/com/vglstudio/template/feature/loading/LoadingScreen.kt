package com.vglstudio.template.feature.loading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
internal fun LoadingScreen(uiState: LoadingUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().testTag(LoadingTestTags.SCREEN))
}

internal object LoadingTestTags {
    const val SCREEN = "loading_screen"
    const val PROGRESS = "loading_progress"
    const val PERCENT = "loading_percent"
    const val ERROR = "loading_error"
}
