package com.kingplay.core.designsystem.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.kingplay.core.designsystem.R

enum class AccentTheme(@StringRes val labelRes: Int, val swatch: Color) {
    VIOLET(R.string.kp_accent_violet, Color(0xFF6B4EFF)),
    OCEAN(R.string.kp_accent_ocean, Color(0xFF0061A4)),
    EMBER(R.string.kp_accent_ember, Color(0xFF9C4400)),
    EMERALD(R.string.kp_accent_emerald, Color(0xFF006C4C)),
}

private class AccentColors(
    val primary: Color,
    val onPrimary: Color,
    val container: Color,
    val onContainer: Color,
)

private val LightNeutralBackground = Color(0xFFFCFBFF)
private val LightNeutralOnSurface = Color(0xFF1B1B1F)
private val DarkNeutralBackground = Color(0xFF121214)
private val DarkNeutralOnSurface = Color(0xFFE5E1E6)

private fun light(accent: AccentTheme) = when (accent) {
    AccentTheme.VIOLET -> AccentColors(Color(0xFF6B4EFF), Color.White, Color(0xFFE6DEFF), Color(0xFF1F0070))
    AccentTheme.OCEAN -> AccentColors(Color(0xFF0061A4), Color.White, Color(0xFFD1E4FF), Color(0xFF001D36))
    AccentTheme.EMBER -> AccentColors(Color(0xFF9C4400), Color.White, Color(0xFFFFDBC8), Color(0xFF331200))
    AccentTheme.EMERALD -> AccentColors(Color(0xFF006C4C), Color.White, Color(0xFF89F8C7), Color(0xFF002114))
}

private fun dark(accent: AccentTheme) = when (accent) {
    AccentTheme.VIOLET -> AccentColors(Color(0xFFC8BFFF), Color(0xFF2D0A9E), Color(0xFF4A2DE0), Color(0xFFE6DEFF))
    AccentTheme.OCEAN -> AccentColors(Color(0xFF9ECAFF), Color(0xFF003258), Color(0xFF00497D), Color(0xFFD1E4FF))
    AccentTheme.EMBER -> AccentColors(Color(0xFFFFB68A), Color(0xFF532200), Color(0xFF763300), Color(0xFFFFDBC8))
    AccentTheme.EMERALD -> AccentColors(Color(0xFF6CDBAC), Color(0xFF003826), Color(0xFF005138), Color(0xFF89F8C7))
}

internal fun lightAccentScheme(accent: AccentTheme): ColorScheme {
    val c = light(accent)
    return lightColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.container,
        onPrimaryContainer = c.onContainer,
        secondary = c.primary,
        onSecondary = c.onPrimary,
        secondaryContainer = c.container,
        onSecondaryContainer = c.onContainer,
        tertiary = c.primary,
        onTertiary = c.onPrimary,
        tertiaryContainer = c.container,
        onTertiaryContainer = c.onContainer,
        background = LightNeutralBackground,
        onBackground = LightNeutralOnSurface,
        surface = LightNeutralBackground,
        onSurface = LightNeutralOnSurface,
    )
}

internal fun darkAccentScheme(accent: AccentTheme): ColorScheme {
    val c = dark(accent)
    return darkColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.container,
        onPrimaryContainer = c.onContainer,
        secondary = c.primary,
        onSecondary = c.onPrimary,
        secondaryContainer = c.container,
        onSecondaryContainer = c.onContainer,
        tertiary = c.primary,
        onTertiary = c.onPrimary,
        tertiaryContainer = c.container,
        onTertiaryContainer = c.onContainer,
        background = DarkNeutralBackground,
        onBackground = DarkNeutralOnSurface,
        surface = DarkNeutralBackground,
        onSurface = DarkNeutralOnSurface,
    )
}

/** True-black surfaces for OLED panels. Works on top of any dark scheme, including dynamic color. */
internal fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF0E0E0E),
    surfaceContainerHigh = Color(0xFF141414),
    surfaceContainerHighest = Color(0xFF1A1A1A),
)
