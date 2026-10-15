package com.vglstudio.template.feature.loading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.template.core.designsystem.component.PrimaryButton
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.core.designsystem.theme.Spacing
import kotlin.math.roundToInt

/** Stateful entry point used by navigation. Calls [onFinished] once when the state becomes Finished. */
@Composable
internal fun LoadingRoute(onFinished: () -> Unit, viewModel: LoadingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnFinished by rememberUpdatedState(onFinished)
    if (uiState == LoadingUiState.Finished) {
        LaunchedEffect(Unit) { currentOnFinished() }
    }
    LoadingScreen(uiState = uiState, onRetry = viewModel::onRetry)
}

/** Stateless screen: everything it shows comes from [uiState]. Tested on the JVM and by screenshots. */
@Composable
internal fun LoadingScreen(uiState: LoadingUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val progress = when (uiState) {
        is LoadingUiState.InProgress -> uiState.progress
        is LoadingUiState.Failed -> uiState.progress
        LoadingUiState.Finished -> 1f
    }
    Surface(modifier = modifier.fillMaxSize().testTag(LoadingTestTags.SCREEN)) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.loading_message),
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().testTag(LoadingTestTags.PROGRESS),
            )
            Text(
                text = stringResource(R.string.loading_percent, (progress * PERCENT).roundToInt()),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(LoadingTestTags.PERCENT),
            )
            if (uiState is LoadingUiState.Failed) {
                Text(
                    text = stringResource(R.string.loading_error),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag(LoadingTestTags.ERROR),
                )
                PrimaryButton(text = stringResource(R.string.loading_retry), onClick = onRetry)
            }
        }
    }
}

private const val PERCENT = 100

internal object LoadingTestTags {
    const val SCREEN = "loading_screen"
    const val PROGRESS = "loading_progress"
    const val PERCENT = "loading_percent"
    const val ERROR = "loading_error"
}

@Preview
@Composable
private fun LoadingScreenPreview() {
    AppTheme {
        LoadingScreen(uiState = LoadingUiState.InProgress(0.5f), onRetry = {})
    }
}
