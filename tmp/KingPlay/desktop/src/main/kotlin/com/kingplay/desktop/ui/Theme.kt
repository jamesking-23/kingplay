package com.kingplay.desktop.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KingPlayColors = darkColorScheme(
    primary = Color(0xFFB4A7FF),
    onPrimary = Color(0xFF1D1250),
    primaryContainer = Color(0xFF352A7A),
    onPrimaryContainer = Color(0xFFE5DEFF),
    background = Color(0xFF111015),
    onBackground = Color(0xFFE7E1E9),
    surface = Color(0xFF111015),
    onSurface = Color(0xFFE7E1E9),
    surfaceVariant = Color(0xFF1D1B22),
    onSurfaceVariant = Color(0xFFC9C4D0),
    error = Color(0xFFFFB4AB),
)

@Composable
fun KingPlayDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KingPlayColors, content = content)
}
