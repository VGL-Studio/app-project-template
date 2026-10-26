package com.vglstudio.template.core.ads

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The heights the page of a flow step is laid out by (plan T-041), at the font scale of [flowStepMetrics]: the head
 * of the ad, the least height of the column layout, the least height of the page, one line of the body.
 */
internal data class FlowStepMetrics(
    val headerHeight: Dp,
    val columnMinHeight: Dp,
    val minHeight: Dp,
    val bodyLineHeight: Dp,
)

/** Empty frame (plan T-041, L-025): the numbers of font scale 1.0, whatever the font scale. */
@Suppress("UNUSED_PARAMETER")
internal fun flowStepMetrics(density: Density, typography: Typography): FlowStepMetrics =
    FlowStepMetrics(headerHeight = 48.dp, columnMinHeight = 360.dp, minHeight = 200.dp, bodyLineHeight = 20.dp)
