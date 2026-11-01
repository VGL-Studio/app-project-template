package com.vglstudio.template.core.ads

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

// Skeleton of the test-first commit (plan T-084, section 4): Material 3 defaults and no font files, so the match test
// fails for the right reason. The code commit fills in the values of the template.

private val AdsTestLightColors = lightColorScheme()
private val AdsTestDarkColors = darkColorScheme()

/** Font files of the test font family: (file name without extension, weight, style), in the order of the family. */
internal val AdsTestFontFiles: List<Triple<String, FontWeight, FontStyle>> = emptyList()

internal val AdsTestFontFamily: FontFamily = FontFamily.Default

private val AdsTestTypography: Typography = Typography()

@Composable
internal fun AdsTestTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) AdsTestDarkColors else AdsTestLightColors,
        typography = AdsTestTypography,
        content = content,
    )
}
