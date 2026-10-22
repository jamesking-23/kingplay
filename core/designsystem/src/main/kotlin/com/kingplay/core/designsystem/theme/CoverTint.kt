package com.kingplay.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.core.graphics.ColorUtils

/** The colour taken from the playing song's cover, already tuned to read well; null when there is none. */
val LocalCoverTint = staticCompositionLocalOf<Color?> { null }

private const val MIN_SATURATION = 0.45f
private const val MAX_SATURATION = 0.9f
private const val DARK_LIGHTNESS = 0.68f
private const val LIGHT_LIGHTNESS = 0.38f
private const val SURFACE_MIX_DARK = 0.10f
private const val SURFACE_MIX_LIGHT = 0.07f
private const val SECONDARY_MIX = 0.4f
private const val GRADIENT_TOP_MIX = 0.34f
private const val GRADIENT_END = 0.62f

/** Keeps the hue of [argb] but moves lightness and saturation to a range that has contrast on the current background. */
fun tuneCoverColor(argb: Int, dark: Boolean): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    hsl[1] = hsl[1].coerceIn(MIN_SATURATION, MAX_SATURATION)
    hsl[2] = if (dark) DARK_LIGHTNESS else LIGHT_LIGHTNESS
    return Color(ColorUtils.HSLToColor(hsl))
}

/**
 * Blends the design's own colours with a cover colour: the main colour follows the cover, and the surfaces take a
 * small amount of it. Shapes, type and layout are untouched, so the design stays itself.
 */
internal fun ColorScheme.withCover(tint: Color, dark: Boolean): ColorScheme {
    val mix = if (dark) SURFACE_MIX_DARK else SURFACE_MIX_LIGHT
    fun soft(c: Color) = lerp(c, tint, mix)
    return withAccent(tint, dark).copy(
        secondary = lerp(secondary, tint, SECONDARY_MIX),
        tertiary = lerp(tertiary, tint, SECONDARY_MIX),
        background = soft(background),
        surface = soft(surface),
        surfaceVariant = soft(surfaceVariant),
        surfaceContainerLowest = soft(surfaceContainerLowest),
        surfaceContainerLow = soft(surfaceContainerLow),
        surfaceContainer = soft(surfaceContainer),
        surfaceContainerHigh = soft(surfaceContainerHigh),
        surfaceContainerHighest = soft(surfaceContainerHighest),
    )
}

/**
 * The backdrop behind the whole app: the design's background, fading from a wash of the cover colour at the top.
 * Screens draw on top of it with transparent containers, so the colour blends into the design instead of replacing it.
 */
@Composable
fun CoverBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme.background
    val tint = LocalCoverTint.current
    val brush = if (tint == null) {
        Brush.verticalGradient(listOf(base, base))
    } else {
        Brush.verticalGradient(
            0f to lerp(base, tint, GRADIENT_TOP_MIX),
            GRADIENT_END to base,
            1f to base,
        )
    }
    Box(modifier = modifier.background(brush)) { content() }
}
