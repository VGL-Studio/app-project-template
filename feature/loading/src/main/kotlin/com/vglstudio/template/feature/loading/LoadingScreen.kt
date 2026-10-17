package com.vglstudio.template.feature.loading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.template.core.designsystem.R as DesignSystemR
import com.vglstudio.template.core.designsystem.component.BrandLogo
import com.vglstudio.template.core.designsystem.component.PrimaryButton
import com.vglstudio.template.core.designsystem.component.ProgressBar
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.core.designsystem.theme.Spacing
import kotlin.math.roundToInt

/** Stateful entry point used by navigation. Calls [onFinished] once when the state becomes Finished. */
@Composable
internal fun LoadingRoute(onFinished: () -> Unit, viewModel: LoadingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnFinished by rememberUpdatedState(onFinished)
    if (uiState == LoadingUiState.Finished) {
        // Draw nothing: with the flag off, the app opens straight on the start screen (spec loading, KB-4).
        LaunchedEffect(Unit) { currentOnFinished() }
        return
    }
    // The name of the running app (app_name of :app), so the feature neither depends on :app nor hardcodes it.
    val context = LocalContext.current
    val appName = remember(context) { context.applicationInfo.loadLabel(context.packageManager).toString() }
    LoadingScreen(uiState = uiState, appName = appName, onRetry = viewModel::onRetry)
}

/**
 * Stateless screen: everything it shows comes from [uiState]. Tested on the JVM and by screenshots.
 * Layout follows docs/features/loading/design/README.md, section 3: the content starts at a quarter of the
 * screen height in every state, so the logo does not move when the error block appears below the bar.
 */
@Composable
internal fun LoadingScreen(
    uiState: LoadingUiState,
    appName: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = when (uiState) {
        is LoadingUiState.InProgress -> uiState.progress
        is LoadingUiState.Failed -> uiState.progress
        LoadingUiState.Finished -> 1f
    }
    val failed = uiState is LoadingUiState.Failed
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Surface(modifier = modifier.fillMaxSize().testTag(LoadingTestTags.SCREEN)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = maxHeight * CONTENT_TOP_FRACTION)
                    .padding(horizontal = Spacing.xl + Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BrandLogo()
                Spacer(modifier = Modifier.height(Spacing.lg))
                Text(
                    text = appName,
                    style = typography.headlineLarge,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(LoadingTestTags.APP_NAME),
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(DesignSystemR.string.brand_tagline),
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(Spacing.xl + Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(if (failed) R.string.loading_status_failed else R.string.loading_status),
                        style = typography.labelLarge,
                        color = if (failed) colors.error else colors.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.loading_percent, (progress * PERCENT).roundToInt()),
                        style = typography.bodyMedium,
                        color = if (failed) colors.error else colors.primary,
                        modifier = Modifier.testTag(LoadingTestTags.PERCENT),
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.md))
                if (failed) {
                    ProgressBar(
                        progress = { progress },
                        modifier = Modifier.testTag(LoadingTestTags.PROGRESS),
                        color = colors.error,
                        trackColor = colors.errorContainer,
                    )
                    Spacer(modifier = Modifier.height(Spacing.lg + Spacing.xs))
                    LoadingError(onRetry = onRetry)
                } else {
                    ProgressBar(progress = { progress }, modifier = Modifier.testTag(LoadingTestTags.PROGRESS))
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    Text(
                        text = stringResource(R.string.loading_guidance),
                        style = typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            LoadingFooter(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.xl))
        }
    }
}

/** Error block below the progress bar: title with an alert icon, guidance, and the retry button. */
@Composable
private fun LoadingError(onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().testTag(LoadingTestTags.ERROR),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_loading_alert),
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.size(ALERT_ICON_SIZE),
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = stringResource(R.string.loading_error_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.loading_error),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(
            text = stringResource(R.string.loading_retry),
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().testTag(LoadingTestTags.RETRY),
            icon = painterResource(R.drawable.ic_loading_retry),
        )
    }
}

/** Footer pinned to the bottom of the screen: lock icon and privacy line. */
@Composable
private fun LoadingFooter(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_loading_lock),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(LOCK_ICON_SIZE),
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = stringResource(R.string.loading_footer),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

private const val PERCENT = 100

// The content starts at about a quarter of the screen height (design: 220 of 891).
private const val CONTENT_TOP_FRACTION = 0.25f
private val ALERT_ICON_SIZE = 18.dp
private val LOCK_ICON_SIZE = 12.dp

internal object LoadingTestTags {
    const val SCREEN = "loading_screen"
    const val PROGRESS = "loading_progress"
    const val PERCENT = "loading_percent"
    const val ERROR = "loading_error"
    const val APP_NAME = "loading_app_name"
    const val RETRY = "loading_retry"
}

// Sample app name of the design, for previews only.
private const val PREVIEW_APP_NAME = "Todo App"

@Preview
@Composable
private fun LoadingScreenPreview() {
    AppTheme {
        LoadingScreen(uiState = LoadingUiState.InProgress(0.68f), appName = PREVIEW_APP_NAME, onRetry = {})
    }
}

@Preview
@Composable
private fun LoadingScreenFailedPreview() {
    AppTheme {
        LoadingScreen(uiState = LoadingUiState.Failed(0.68f), appName = PREVIEW_APP_NAME, onRetry = {})
    }
}
