package com.billiardsdraw.vinceproshop.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkScheme =
    darkColorScheme(
        primary = DoozyAccent,
        onPrimary = DoozyTextPrimary,
        secondary = DoozyAccentHover,
        background = DoozyBackground,
        onBackground = DoozyTextPrimary,
        surface = DoozySurface,
        onSurface = DoozyTextPrimary,
        surfaceVariant = DoozySurfaceVariant,
        onSurfaceVariant = DoozyTextMuted,
    )

private val LightScheme =
    lightColorScheme(
        primary = LightAccent,
        onPrimary = LightSurface,
        secondary = DoozyAccent,
        background = LightBackground,
        onBackground = LightText,
        surface = LightSurface,
        onSurface = LightText,
    )

@Suppress("ModifierRequired", "ktlint:standard:function-naming")
@Composable
fun VinceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = VinceTypography,
        content = content,
    )
}
