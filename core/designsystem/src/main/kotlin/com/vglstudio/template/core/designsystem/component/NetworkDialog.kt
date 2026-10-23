package com.vglstudio.template.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vglstudio.template.core.designsystem.R
import com.vglstudio.template.core.designsystem.theme.Spacing

// Width of a Material 3 basic dialog, as in the design (design network, section 3): the only size outside the
// system. On a narrow screen the card takes the width left by the screen's margins.
private val DialogWidth = 312.dp

// Opacity of the scrim behind the dialog, the Material 3 scrim of the design (design network, section 3).
private const val SCRIM_ALPHA = 0.32f

/**
 * The network dialog card (spec network KB-2, design network): wifi-off icon, title, message, after a retry that
 * found no network a line saying so, and "Retry" across the card. While [checking], the button shows a spinner and
 * ignores clicks. Text comes from the caller, so the app keeps its own wording and languages.
 */
@Composable
fun NetworkDialog(
    title: String,
    message: String,
    retryLabel: String,
    stillOfflineMessage: String?,
    checking: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .widthIn(max = DialogWidth)
            .fillMaxWidth()
            .background(colors.surfaceContainerHigh, MaterialTheme.shapes.extraLarge)
            // TalkBack announces the dialog by its title when it opens.
            .semantics { paneTitle = title }
            .testTag(NetworkDialogTestTags.CARD)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_wifi_off),
            contentDescription = null,
            modifier = Modifier.testTag(NetworkDialogTestTags.ICON),
            tint = colors.secondary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (stillOfflineMessage != null) {
            Text(
                text = stillOfflineMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    // Read out when it appears after a retry, without moving the TalkBack focus.
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag(NetworkDialogTestTags.STILL_OFFLINE),
            )
        }
        PrimaryButton(
            text = retryLabel,
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().testTag(NetworkDialogTestTags.RETRY),
            loading = checking,
        )
    }
}

/**
 * The network dialog centered on a scrim that covers the whole screen and takes every touch, so nothing behind it
 * can be pressed and a touch outside the card closes nothing. Scrolls when the screen is too short.
 */
@Composable
fun NetworkDialogScreen(
    title: String,
    message: String,
    retryLabel: String,
    stillOfflineMessage: String?,
    checking: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA))
            .pointerInput(Unit) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            }
            .testTag(NetworkDialogTestTags.SCREEN),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Shown edge to edge in its own window: keep the card clear of the system bars.
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NetworkDialog(
                title = title,
                message = message,
                retryLabel = retryLabel,
                stillOfflineMessage = stillOfflineMessage,
                checking = checking,
                onRetry = onRetry,
            )
        }
    }
}

/** Tags for UI tests of the network dialog. */
object NetworkDialogTestTags {
    const val SCREEN = "network_dialog"
    const val CARD = "network_dialog_card"
    const val ICON = "network_dialog_icon"
    const val RETRY = "network_dialog_retry"
    const val SPINNER = PrimaryButtonTestTags.SPINNER
    const val STILL_OFFLINE = "network_dialog_still_offline"
}
