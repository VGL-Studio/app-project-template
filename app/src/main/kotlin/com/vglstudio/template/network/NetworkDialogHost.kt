package com.vglstudio.template.network

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.vglstudio.template.R
import com.vglstudio.template.core.designsystem.component.NetworkDialogScreen
import com.vglstudio.template.core.network.NetworkDialogState

/**
 * Draws [content] and, while [state] is shown, the network dialog over it (spec network, KB-2). The dialog is drawn
 * in its own full-screen dialog window: as the top window it keeps touches and screen readers off the content behind
 * it, and system Back goes to its own dispatcher, where Back calls [onBack] instead of closing the dialog or
 * navigating. The content stays in its own window, so showing and hiding the dialog never rebuilds it.
 */
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
    if (state is NetworkDialogState.Shown) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                // Edge to edge, so the scrim also covers the status and navigation bars.
                decorFitsSystemWindows = false,
            ),
        ) {
            // The screen draws its own scrim; turn off the system dim so the screen is not darkened twice.
            val window = (LocalView.current.parent as? DialogWindowProvider)?.window
            SideEffect { window?.setDimAmount(0f) }
            NetworkDialogScreen(
                title = stringResource(R.string.network_lost_title),
                message = stringResource(R.string.network_lost_message),
                retryLabel = stringResource(R.string.network_lost_retry),
                stillOfflineMessage = if (state.stillOffline) {
                    stringResource(
                        R.string.network_lost_still_offline,
                    )
                } else {
                    null
                },
                checking = state.checking,
                onRetry = onRetry,
            )
            BackHandler(onBack = onBack)
        }
    }
}
