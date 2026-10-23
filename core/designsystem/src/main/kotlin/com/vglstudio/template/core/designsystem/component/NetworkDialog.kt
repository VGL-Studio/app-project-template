package com.vglstudio.template.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Skeleton for the tests written first (T-017, step 3). */
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
    Box(modifier = modifier.testTag(NetworkDialogTestTags.CARD))
}

/** Skeleton for the tests written first (T-017, step 3). */
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
    Box(modifier = modifier.fillMaxSize().testTag(NetworkDialogTestTags.SCREEN)) {
        NetworkDialog(title, message, retryLabel, stillOfflineMessage, checking, onRetry)
    }
}

object NetworkDialogTestTags {
    const val SCREEN = "network_dialog"
    const val CARD = "network_dialog_card"
    const val ICON = "network_dialog_icon"
    const val RETRY = "network_dialog_retry"
    const val SPINNER = PrimaryButtonTestTags.SPINNER
    const val STILL_OFFLINE = "network_dialog_still_offline"
}
