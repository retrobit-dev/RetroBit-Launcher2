package com.retrobit.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*

enum class RetroBitThemeMode {
    LIGHT_EINK,
    DARK_EINK,
    POCKET_90S,
    STARK_1BIT
}

val LocalRetroBitColors = staticCompositionLocalOf { LightEInkPalette }

@Composable
fun RetroBitLauncherTheme(
    themeMode: RetroBitThemeMode = RetroBitThemeMode.LIGHT_EINK,
    content: @Composable () -> Unit
) {
    val colors = when (themeMode) {
        RetroBitThemeMode.LIGHT_EINK -> LightEInkPalette
        RetroBitThemeMode.DARK_EINK -> DarkEInkPalette
        RetroBitThemeMode.POCKET_90S -> Pocket90sPalette
        RetroBitThemeMode.STARK_1BIT -> Stark1BitPalette
    }

    val materialScheme = lightColorScheme(
        background = colors.background,
        surface = colors.surface,
        onBackground = colors.inkPrimary,
        onSurface = colors.inkPrimary,
        primary = colors.inkPrimary,
        secondary = colors.inkSecondary
    )

    CompositionLocalProvider(LocalRetroBitColors provides colors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = RetroBitTypography,
            content = content
        )
    }
}
