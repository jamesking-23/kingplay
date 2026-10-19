package com.kingplay.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Three more designs. Each has its own colours, corner language, typeface and now-playing artwork treatment:
 *   KingNeon    - night-club: near-black, hot magenta and cyan, sharp corners, monospace type, glowing cover.
 *   KingGlass   - calm and airy: blue-white / deep navy, very round corners, light sans type, soft floating cover.
 *   KingClassic - vintage hi-fi: cream paper / walnut, brass and amber, serif type, a framed record-sleeve cover.
 */

private fun c(hex: Long) = Color(hex)

/* ---------- KingNeon ---------- */

internal fun kingNeonColorScheme(dark: Boolean): ColorScheme = if (dark) {
    darkColorScheme(
        primary = c(0xFFFF2BD6), onPrimary = c(0xFF1A0016),
        primaryContainer = c(0xFF4A0A40), onPrimaryContainer = c(0xFFFFB3F2),
        secondary = c(0xFF00F0FF), onSecondary = c(0xFF00282C),
        secondaryContainer = c(0xFF00424A), onSecondaryContainer = c(0xFFA8F8FF),
        tertiary = c(0xFFC6FF00), onTertiary = c(0xFF1C2600),
        background = c(0xFF07070D), onBackground = c(0xFFEDEDFF),
        surface = c(0xFF0B0B16), onSurface = c(0xFFEDEDFF),
        surfaceVariant = c(0xFF1B1B33), onSurfaceVariant = c(0xFFA6A6D6),
        surfaceContainerLowest = c(0xFF05050A), surfaceContainerLow = c(0xFF0E0E1A),
        surfaceContainer = c(0xFF121224), surfaceContainerHigh = c(0xFF18182E), surfaceContainerHighest = c(0xFF1F1F3A),
        outline = c(0xFF6B6BA8), outlineVariant = c(0xFF2E2E52),
        error = c(0xFFFF5C7A), onError = c(0xFF2B000A),
    )
} else {
    lightColorScheme(
        primary = c(0xFFB0009A), onPrimary = c(0xFFFFFFFF),
        primaryContainer = c(0xFFFFD6F6), onPrimaryContainer = c(0xFF3A0030),
        secondary = c(0xFF006C77), onSecondary = c(0xFFFFFFFF),
        secondaryContainer = c(0xFFB3F4FB), onSecondaryContainer = c(0xFF002024),
        tertiary = c(0xFF4D6600), onTertiary = c(0xFFFFFFFF),
        background = c(0xFFF3F0FF), onBackground = c(0xFF14112B),
        surface = c(0xFFF8F6FF), onSurface = c(0xFF14112B),
        surfaceVariant = c(0xFFE4DFFA), onSurfaceVariant = c(0xFF4A4670),
        surfaceContainerLowest = c(0xFFFFFFFF), surfaceContainerLow = c(0xFFF1EEFF),
        surfaceContainer = c(0xFFEAE6FF), surfaceContainerHigh = c(0xFFE2DDFA), surfaceContainerHighest = c(0xFFDAD4F5),
        outline = c(0xFF7A75A3), outlineVariant = c(0xFFC8C2E6),
        error = c(0xFFB3123B), onError = c(0xFFFFFFFF),
    )
}

internal val KingNeonShapes = Shapes(
    extraSmall = RoundedCornerShape(1.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(3.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(6.dp),
)

/* ---------- KingGlass ---------- */

internal fun kingGlassColorScheme(dark: Boolean): ColorScheme = if (dark) {
    darkColorScheme(
        primary = c(0xFF9DAAFF), onPrimary = c(0xFF0B1226),
        primaryContainer = c(0xFF2B3680), onPrimaryContainer = c(0xFFDDE1FF),
        secondary = c(0xFF5EEAD4), onSecondary = c(0xFF00201B),
        secondaryContainer = c(0xFF124A42), onSecondaryContainer = c(0xFFB5F5EA),
        tertiary = c(0xFFFFB4D1), onTertiary = c(0xFF3A0020),
        background = c(0xFF0B1226), onBackground = c(0xFFE6EBFF),
        surface = c(0xFF0F1830), onSurface = c(0xFFE6EBFF),
        surfaceVariant = c(0xFF1F2B4F), onSurfaceVariant = c(0xFFB4BEE0),
        surfaceContainerLowest = c(0xFF080D1D), surfaceContainerLow = c(0xFF111B36),
        surfaceContainer = c(0xFF16213F), surfaceContainerHigh = c(0xFF1C2849), surfaceContainerHighest = c(0xFF233156),
        outline = c(0xFF7C88B5), outlineVariant = c(0xFF2C3A66),
        error = c(0xFFFFB4AB), onError = c(0xFF690005),
    )
} else {
    lightColorScheme(
        primary = c(0xFF4C5BE6), onPrimary = c(0xFFFFFFFF),
        primaryContainer = c(0xFFDDE1FF), onPrimaryContainer = c(0xFF0A1260),
        secondary = c(0xFF007F86), onSecondary = c(0xFFFFFFFF),
        secondaryContainer = c(0xFFB5F2F5), onSecondaryContainer = c(0xFF002022),
        tertiary = c(0xFFA4306A), onTertiary = c(0xFFFFFFFF),
        background = c(0xFFEAF2FF), onBackground = c(0xFF16213A),
        surface = c(0xFFF4F8FF), onSurface = c(0xFF16213A),
        surfaceVariant = c(0xFFDCE6FA), onSurfaceVariant = c(0xFF43506F),
        surfaceContainerLowest = c(0xFFFFFFFF), surfaceContainerLow = c(0xFFEFF5FF),
        surfaceContainer = c(0xFFE6EFFF), surfaceContainerHigh = c(0xFFDEE9FC), surfaceContainerHighest = c(0xFFD5E2F8),
        outline = c(0xFF74819F), outlineVariant = c(0xFFC3D0EA),
        error = c(0xFFBA1A1A), onError = c(0xFFFFFFFF),
    )
}

internal val KingGlassShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(44.dp),
)

/* ---------- KingClassic ---------- */

internal fun kingClassicColorScheme(dark: Boolean): ColorScheme = if (dark) {
    darkColorScheme(
        primary = c(0xFFE3B35A), onPrimary = c(0xFF2B1A00),
        primaryContainer = c(0xFF5A3D0A), onPrimaryContainer = c(0xFFFFDEA0),
        secondary = c(0xFFD9976A), onSecondary = c(0xFF3A1A00),
        secondaryContainer = c(0xFF5B3418), onSecondaryContainer = c(0xFFFFDBC4),
        tertiary = c(0xFF9CC28A), onTertiary = c(0xFF10260A),
        background = c(0xFF1B120B), onBackground = c(0xFFF1E4CC),
        surface = c(0xFF21160D), onSurface = c(0xFFF1E4CC),
        surfaceVariant = c(0xFF3A2A1B), onSurfaceVariant = c(0xFFD0BB98),
        surfaceContainerLowest = c(0xFF150D07), surfaceContainerLow = c(0xFF261910),
        surfaceContainer = c(0xFF2D1F14), surfaceContainerHigh = c(0xFF35261A), surfaceContainerHighest = c(0xFF3F2E20),
        outline = c(0xFF9C8660), outlineVariant = c(0xFF4A3724),
        error = c(0xFFFFB4A8), onError = c(0xFF600F00),
    )
} else {
    lightColorScheme(
        primary = c(0xFF6B3F1D), onPrimary = c(0xFFFFFFFF),
        primaryContainer = c(0xFFFFDDBB), onPrimaryContainer = c(0xFF2B1500),
        secondary = c(0xFF7A5C10), onSecondary = c(0xFFFFFFFF),
        secondaryContainer = c(0xFFF8E0A0), onSecondaryContainer = c(0xFF251A00),
        tertiary = c(0xFF3F6B2E), onTertiary = c(0xFFFFFFFF),
        background = c(0xFFF4EBDA), onBackground = c(0xFF2B1D10),
        surface = c(0xFFFAF3E4), onSurface = c(0xFF2B1D10),
        surfaceVariant = c(0xFFE9DCC2), onSurfaceVariant = c(0xFF51402B),
        surfaceContainerLowest = c(0xFFFFFBF2), surfaceContainerLow = c(0xFFF6EEDC),
        surfaceContainer = c(0xFFEFE5D0), surfaceContainerHigh = c(0xFFE8DCC4), surfaceContainerHighest = c(0xFFE0D2B8),
        outline = c(0xFF857050), outlineVariant = c(0xFFD3C3A3),
        error = c(0xFFA3261A), onError = c(0xFFFFFFFF),
    )
}

internal val KingClassicShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp),
)

/* ---------- type ---------- */

private val baseType = Typography()

private fun TextStyle.fam(family: FontFamily, weight: FontWeight? = null, spacing: Float? = null): TextStyle =
    copy(
        fontFamily = family,
        fontWeight = weight ?: fontWeight,
        letterSpacing = spacing?.sp ?: letterSpacing,
    )

private fun typographyOf(family: FontFamily, heading: FontWeight, body: FontWeight, track: Float): Typography =
    Typography(
        displayLarge = baseType.displayLarge.fam(family, heading, track),
        displayMedium = baseType.displayMedium.fam(family, heading, track),
        displaySmall = baseType.displaySmall.fam(family, heading, track),
        headlineLarge = baseType.headlineLarge.fam(family, heading, track),
        headlineMedium = baseType.headlineMedium.fam(family, heading, track),
        headlineSmall = baseType.headlineSmall.fam(family, heading, track),
        titleLarge = baseType.titleLarge.fam(family, heading, track),
        titleMedium = baseType.titleMedium.fam(family, heading, track),
        titleSmall = baseType.titleSmall.fam(family, heading, track),
        bodyLarge = baseType.bodyLarge.fam(family, body, track),
        bodyMedium = baseType.bodyMedium.fam(family, body, track),
        bodySmall = baseType.bodySmall.fam(family, body, track),
        labelLarge = baseType.labelLarge.fam(family, heading, track),
        labelMedium = baseType.labelMedium.fam(family, body, track),
        labelSmall = baseType.labelSmall.fam(family, body, track),
    )

internal val KingNeonTypography = typographyOf(FontFamily.Monospace, FontWeight.Bold, FontWeight.Normal, 0.2f)
internal val KingGlassTypography = typographyOf(FontFamily.SansSerif, FontWeight.Light, FontWeight.Normal, 0.4f)
internal val KingClassicTypography = typographyOf(FontFamily.Serif, FontWeight.Bold, FontWeight.Normal, 0.1f)
