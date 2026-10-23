package com.vglstudio.template.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** Whether a collapsible ad is collapsed. It can only collapse: there is no way to expand it again (ADR-0022). */
@Stable
class CollapsibleAdState internal constructor(collapsed: Boolean) {
    var collapsed by mutableStateOf(collapsed)
        private set

    fun collapse() {
        // Stub (T-012, step 3).
    }

    internal companion object {
        val Saver: Saver<CollapsibleAdState, Boolean> = Saver(save = { it.collapsed }, restore = ::CollapsibleAdState)
    }
}

@Composable
fun rememberCollapsibleAdState(): CollapsibleAdState =
    rememberSaveable(saver = CollapsibleAdState.Saver) { CollapsibleAdState(collapsed = false) }

@Composable
fun CollapsibleNativeAdCard(
    assets: NativeAdAssets,
    collapsed: Boolean,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Stub (T-012, step 3).
}

@Composable
internal fun CollapsibleNativeAdLayout(
    assets: NativeAdAssets,
    collapsed: Boolean,
    onCollapse: () -> Unit,
    binder: NativeAdBinder,
    showAdChoicesMarker: Boolean,
    modifier: Modifier = Modifier,
) {
    // Stub (T-012, step 3).
}

internal sealed interface CollapsibleAdContent {
    data object Loading : CollapsibleAdContent
    data class Ready(val assets: NativeAdAssets) : CollapsibleAdContent
    data object Hidden : CollapsibleAdContent
}

@Composable
internal fun CollapsibleNativeAdContent(
    content: CollapsibleAdContent,
    state: CollapsibleAdState,
    onCollapsed: () -> Unit,
    modifier: Modifier = Modifier,
    renderReady: @Composable (assets: NativeAdAssets, collapsed: Boolean, onCollapse: () -> Unit) -> Unit =
        { assets, collapsed, onCollapse -> CollapsibleNativeAdCard(assets, collapsed, onCollapse) },
) {
    // Stub (T-012, step 3).
}

@Composable
fun CollapsibleNativeAdSlot(placement: AdPlacement, modifier: Modifier = Modifier) {
    // Stub (T-012, step 3).
}
