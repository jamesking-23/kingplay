package com.kingplay.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

/**
 * Swaps in a colour the person picked as the main colour. The text colour on top of it is chosen for contrast, and the
 * container tint is the colour faded into the surface so it reads in both light and dark.
 */
internal fun ColorScheme.withAccent(accent: Color, dark: Boolean): ColorScheme {
    val on = if (accent.luminance() > LIGHT_THRESHOLD) Color(0xFF111111) else Color.White
    val container = accent.copy(alpha = if (dark) CONTAINER_ALPHA_DARK else CONTAINER_ALPHA_LIGHT).compositeOver(surface)
    val onContainer = if (container.luminance() > LIGHT_THRESHOLD) Color(0xFF111111) else Color.White
    return copy(
        primary = accent,
        onPrimary = on,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        surfaceTint = accent,
    )
}

private const val LIGHT_THRESHOLD = 0.45f
private const val CONTAINER_ALPHA_DARK = 0.35f
private const val CONTAINER_ALPHA_LIGHT = 0.22f

/** Colours offered in settings (ARGB). */
val AccentChoices: List<Long> = listOf(
    0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047, 0xFF00ACC1, 0xFF1E88E5,
    0xFF5E35B1, 0xFFD81B60, 0xFF6D4C41, 0xFF546E7A, 0xFFFF2BD6, 0xFF1DB954,
)
