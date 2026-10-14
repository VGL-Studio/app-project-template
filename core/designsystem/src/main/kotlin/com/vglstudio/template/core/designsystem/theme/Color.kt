package com.vglstudio.template.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette. Each app cloned from the template replaces these values (ADR-0008).
internal val BrandPrimary = Color(0xFF2F5BEA)
internal val BrandOnPrimary = Color(0xFFFFFFFF)
internal val BrandPrimaryDark = Color(0xFFB4C5FF)
internal val BrandOnPrimaryDark = Color(0xFF00287A)

internal val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
)

internal val DarkColors = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = BrandOnPrimaryDark,
)
