package com.vglstudio.template.feature.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.template.core.ads.AdPlacement
import com.vglstudio.template.core.ads.NativeAdAssets
import com.vglstudio.template.core.ads.NativeAdCard
import com.vglstudio.template.core.ads.NativeAdSize
import com.vglstudio.template.core.ads.NativeAdSlot
import com.vglstudio.template.core.ads.PrivacyOptionsButton
import com.vglstudio.template.core.designsystem.component.PrimaryButton
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.core.designsystem.theme.Spacing

/** Stateful entry point used by navigation. Connects the ViewModel to the stateless screen. */
@Composable
internal fun SampleRoute(viewModel: SampleViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SampleScreen(uiState = uiState, onIncrement = viewModel::onIncrement)
}

/**
 * Stateless screen: everything it shows comes from [uiState]. Tested on the JVM and by screenshots.
 * [bottomAd] draws the native ad pinned to the bottom, only when the feature is ready; tests pass a fake
 * [NativeAdCard] because they have no ads controller (ADR-0013).
 * Public so the screenshot test of the network dialog in :app can draw it under the dialog, as the design does
 * (plan T-017); other modules still open this feature only through its navigation destination.
 */
@Composable
fun SampleScreen(
    uiState: SampleUiState,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    bottomAd: @Composable (Modifier) -> Unit = { NativeAdSlot(AdPlacement.SAMPLE_NATIVE_SMALL, it) },
) {
    Surface(modifier = modifier.fillMaxSize().testTag(SampleTestTags.SCREEN)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.sample_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                when (uiState) {
                    SampleUiState.Loading -> CircularProgressIndicator()
                    SampleUiState.Disabled -> Text(text = stringResource(R.string.sample_disabled))
                    is SampleUiState.Ready -> ReadyContent(uiState, onIncrement)
                }
                // Only visible where the consent form applies; an app usually puts it in its settings screen.
                PrivacyOptionsButton()
            }
            if (uiState is SampleUiState.Ready) {
                // Pinned to the bottom, apart from the scrolling content, never right under the button (design).
                bottomAd(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md),
                )
            }
        }
    }
}

@Composable
private fun ReadyContent(uiState: SampleUiState.Ready, onIncrement: () -> Unit) {
    Text(text = stringResource(R.string.sample_count_label))
    Text(
        text = uiState.count.toString(),
        style = MaterialTheme.typography.displayMedium,
        modifier = Modifier.testTag(SampleTestTags.COUNT),
    )
    PrimaryButton(
        text = stringResource(R.string.sample_increment),
        onClick = onIncrement,
        modifier = Modifier.fillMaxWidth(),
    )
    if (uiState.saveFailed) {
        Text(
            text = stringResource(R.string.sample_save_failed),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag(SampleTestTags.SAVE_ERROR),
        )
    }
}

internal object SampleTestTags {
    const val SCREEN = "sample_screen"
    const val COUNT = "sample_count"
    const val SAVE_ERROR = "sample_save_error"
}

@Preview
@Composable
private fun SampleScreenPreview() {
    AppTheme {
        SampleScreen(uiState = SampleUiState.Ready(count = 3), onIncrement = {})
    }
}

@Preview
@Composable
private fun SampleScreenWithAdPreview() {
    AppTheme {
        SampleScreen(
            uiState = SampleUiState.Ready(count = 3),
            onIncrement = {},
            bottomAd = {
                NativeAdCard(
                    assets = NativeAdAssets(
                        headline = "Headline of the ad",
                        advertiser = "Advertiser or short body",
                        body = null,
                        callToAction = "Install",
                        icon = null,
                        hasMedia = false,
                    ),
                    size = NativeAdSize.SMALL,
                    modifier = it,
                )
            },
        )
    }
}
