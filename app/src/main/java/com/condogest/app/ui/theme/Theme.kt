package com.condogest.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Cyan400,
    onPrimary = DarkBg,
    primaryContainer = Cyan700,
    onPrimaryContainer = Color.White,
    secondary = Purple400,
    onSecondary = DarkBg,
    secondaryContainer = Purple700,
    onSecondaryContainer = Color.White,
    tertiary = Pink400,
    onTertiary = DarkBg,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = Red400,
    onError = DarkBg,
    outline = TextMuted,
    outlineVariant = Color(0xFF334155)
)

@Composable
fun CondoGestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
