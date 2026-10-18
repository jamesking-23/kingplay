package com.kingplay.desktop.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.kingplay.core.model.settings.AppThemeMode
import com.kingplay.desktop.settings.DesktopSettings

@Composable
fun KingPlayDesktopTheme(settings: DesktopSettings, content: @Composable () -> Unit) {
    val dark = isDark(settings.themeMode, isSystemInDarkTheme())
    val primary = Color(accentArgb(settings.accent, dark))
    val scheme = if (dark) {
        val background = if (settings.themeMode == AppThemeMode.AMOLED) Color.Black else Color(DARK_BACKGROUND)
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF14102F),
            primaryContainer = lerp(primary, background, CONTAINER_MIX),
            onPrimaryContainer = Color.White,
            background = background,
            onBackground = Color(0xFFE7E1E9),
            surface = background,
            onSurface = Color(0xFFE7E1E9),
            surfaceVariant = lerp(background, Color.White, SURFACE_LIFT),
            onSurfaceVariant = Color(0xFFC9C4D0),
            error = Color(0xFFFFB4AB),
        )
    } else {
        val background = Color(0xFFFFFBFF)
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = lerp(primary, background, CONTAINER_MIX),
            onPrimaryContainer = Color(0xFF1B1B1F),
            background = background,
            onBackground = Color(0xFF1B1B1F),
            surface = background,
            onSurface = Color(0xFF1B1B1F),
            surfaceVariant = Color(0xFFEDE8F0),
            onSurfaceVariant = Color(0xFF49454F),
            error = Color(0xFFBA1A1A),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

private const val DARK_BACKGROUND = 0xFF111015
private const val CONTAINER_MIX = 0.78f
private const val SURFACE_LIFT = 0.07f
