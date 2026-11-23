package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DiscordBlurple,
    onPrimary = Color.White,
    primaryContainer = DiscordBlurpleContainer,
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = DiscordGreen,
    onSecondary = Color.White,
    secondaryContainer = DiscordGreenContainer,
    onSecondaryContainer = Color(0xFFBDF5D3),
    tertiary = DiscordAmber,
    onTertiary = Color(0xFF1F1600),
    background = DiscordObsidian,
    onBackground = DiscordTextPrimary,
    surface = DiscordCharcoal,
    onSurface = DiscordTextPrimary,
    surfaceVariant = DiscordSurfaceElevated,
    onSurfaceVariant = DiscordTextSecondary,
    surfaceContainerLowest = DiscordObsidian,
    surfaceContainerLow = DiscordInputSurface,
    surfaceContainer = DiscordCardSurface,
    surfaceContainerHigh = DiscordSurfaceElevated,
    surfaceContainerHighest = DiscordButtonGray,
    outline = DiscordOutline,
    outlineVariant = DiscordOutlineSubtle,
    error = DiscordCrimson,
    onError = Color.White,
    errorContainer = DiscordCrimsonContainer,
    onErrorContainer = Color(0xFFFFDADA),
)

private val LightColorScheme = lightColorScheme(
    primary = DiscordBlurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E3FF),
    onPrimaryContainer = Color(0xFF151C63),
    secondary = DiscordGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1F4DF),
    onSecondaryContainer = Color(0xFF0A381D),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFD0D4DC),
    error = DiscordCrimson,
    onError = Color.White,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun WatchRpcTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
