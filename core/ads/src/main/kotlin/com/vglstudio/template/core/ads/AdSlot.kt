package com.vglstudio.template.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Reserves the place of an ad in a screen. Features call this, never an ads SDK directly.
 * For now it renders nothing; the AdMob implementation (test ad units only in staging) comes later.
 */
@Composable
fun AdSlot(
    placement: AdPlacement,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier)
}
