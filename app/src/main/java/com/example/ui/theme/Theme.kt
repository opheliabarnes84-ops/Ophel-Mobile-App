package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FrostColorScheme = darkColorScheme(
    primary = FrostPrimary,
    onPrimary = FrostOnPrimary,
    primaryContainer = FrostPrimaryContainer,
    onPrimaryContainer = FrostOnPrimaryContainer,
    secondary = FrostSecondary,
    onSecondary = FrostOnSecondary,
    secondaryContainer = FrostSecondaryContainer,
    onSecondaryContainer = FrostOnSecondaryContainer,
    tertiary = FrostTertiary,
    onTertiary = FrostOnTertiary,
    tertiaryContainer = FrostTertiaryContainer,
    onTertiaryContainer = FrostOnTertiaryContainer,
    background = FrostDarkBackground,
    onBackground = FrostOnSurface,
    surface = FrostDarkSurface,
    onSurface = FrostOnSurface,
    surfaceVariant = FrostDarkSurfaceVariant,
    onSurfaceVariant = FrostOnSurfaceVariant,
    outline = FrostOutline
)

@Composable
fun FrostGuardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FrostColorScheme,
        typography = Typography,
        content = content
    )
}

// Backward-compatibility alias for template
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    FrostGuardTheme(content = content)
}
