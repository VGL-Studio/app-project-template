package com.vglstudio.template.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.vglstudio.template.core.designsystem.R

/** Brand typeface, Inter 4.1 (SIL OFL 1.1, see third_party/inter). Each app may replace it (ADR-0008). */
internal val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/** Material 3 type scale in Inter. Sizes and line heights stay on the Material 3 scale. */
internal val AppTypography: Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.copy(fontFamily = InterFamily),
        displayMedium = displayMedium.copy(fontFamily = InterFamily),
        displaySmall = displaySmall.copy(fontFamily = InterFamily),
        headlineLarge = headlineLarge.copy(fontFamily = InterFamily),
        headlineMedium = headlineMedium.copy(fontFamily = InterFamily),
        headlineSmall = headlineSmall.copy(fontFamily = InterFamily),
        titleLarge = titleLarge.copy(fontFamily = InterFamily),
        // Error titles in the design are SemiBold (600).
        titleMedium = titleMedium.copy(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontFamily = InterFamily),
        bodyLarge = bodyLarge.copy(fontFamily = InterFamily),
        bodyMedium = bodyMedium.copy(fontFamily = InterFamily),
        bodySmall = bodySmall.copy(fontFamily = InterFamily),
        labelLarge = labelLarge.copy(fontFamily = InterFamily),
        labelMedium = labelMedium.copy(fontFamily = InterFamily),
        labelSmall = labelSmall.copy(fontFamily = InterFamily),
    )
}
