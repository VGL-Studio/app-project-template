package com.vglstudio.template.core.ads

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow

/** Skeleton of T-031. */
@Stable
class RewardedAction internal constructor() {
    val dialogVisible: Boolean get() = false

    fun start() {}

    fun watch() {}

    fun dismiss() {}
}

@Composable
fun rememberRewardedAction(placement: AdPlacement, onGranted: () -> Unit): RewardedAction = remember {
    RewardedAction()
}

@Composable
internal fun rememberRewardedActionWith(
    placement: AdPlacement,
    enabled: Flow<Boolean>?,
    start: (onOutcome: (RewardedOutcome) -> Unit) -> Unit,
    onDialogEvent: (String) -> Unit,
    onGranted: () -> Unit,
    notify: (String) -> Unit,
): RewardedAction = remember { RewardedAction() }

internal fun showRewardedNotice(context: Context, text: String) {}

@Composable
fun RewardedAdDialog(
    action: RewardedAction,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {}

@Composable
internal fun RewardedAdDialogCard(
    title: String,
    onWatch: () -> Unit,
    onNoThanks: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Box(modifier)
}

object RewardedAdDialogTestTags {
    const val CARD = "rewarded_dialog_card"
    const val WATCH = "rewarded_dialog_watch"
    const val NO_THANKS = "rewarded_dialog_no_thanks"
    const val CONTENT = "rewarded_dialog_content"
}
