package com.kingplay.desktop.ui

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
import com.kingplay.core.model.settings.AppUiTheme

/** KingNeon, KingGlass and KingClassic on Windows: same palettes, corners and type families as on Android. */

private fun c(hex: Long) = Color(hex)

private class Pal(
    val primary: Long, val onPrimary: Long, val primaryContainer: Long, val onPrimaryContainer: Long,
    val secondary: Long, val onSecondary: Long,
    val bg: Long, val onBg: Long, val surface: Long, val variant: Long, val onVariant: Long,
    val container: Long, val outline: Long, val outlineVariant: Long, val error: Long,
)

private val NeonDark = Pal(
    0xFFFF2BD6, 0xFF1A0016, 0xFF4A0A40, 0xFFFFB3F2, 0xFF00F0FF, 0xFF00282C,
    0xFF07070D, 0xFFEDEDFF, 0xFF0B0B16, 0xFF1B1B33, 0xFFA6A6D6, 0xFF121224, 0xFF6B6BA8, 0xFF2E2E52, 0xFFFF5C7A,
)
private val NeonLight = Pal(
    0xFFB0009A, 0xFFFFFFFF, 0xFFFFD6F6, 0xFF3A0030, 0xFF006C77, 0xFFFFFFFF,
    0xFFF3F0FF, 0xFF14112B, 0xFFF8F6FF, 0xFFE4DFFA, 0xFF4A4670, 0xFFEAE6FF, 0xFF7A75A3, 0xFFC8C2E6, 0xFFB3123B,
)
private val GlassDark = Pal(
    0xFF9DAAFF, 0xFF0B1226, 0xFF2B3680, 0xFFDDE1FF, 0xFF5EEAD4, 0xFF00201B,
    0xFF0B1226, 0xFFE6EBFF, 0xFF0F1830, 0xFF1F2B4F, 0xFFB4BEE0, 0xFF16213F, 0xFF7C88B5, 0xFF2C3A66, 0xFFFFB4AB,
)
private val GlassLight = Pal(
    0xFF4C5BE6, 0xFFFFFFFF, 0xFFDDE1FF, 0xFF0A1260, 0xFF007F86, 0xFFFFFFFF,
    0xFFEAF2FF, 0xFF16213A, 0xFFF4F8FF, 0xFFDCE6FA, 0xFF43506F, 0xFFE6EFFF, 0xFF74819F, 0xFFC3D0EA, 0xFFBA1A1A,
)
private val ClassicDark = Pal(
    0xFFE3B35A, 0xFF2B1A00, 0xFF5A3D0A, 0xFFFFDEA0, 0xFFD9976A, 0xFF3A1A00,
    0xFF1B120B, 0xFFF1E4CC, 0xFF21160D, 0xFF3A2A1B, 0xFFD0BB98, 0xFF2D1F14, 0xFF9C8660, 0xFF4A3724, 0xFFFFB4A8,
)
private val ClassicLight = Pal(
    0xFF6B3F1D, 0xFFFFFFFF, 0xFFFFDDBB, 0xFF2B1500, 0xFF7A5C10, 0xFFFFFFFF,
    0xFFF4EBDA, 0xFF2B1D10, 0xFFFAF3E4, 0xFFE9DCC2, 0xFF51402B, 0xFFEFE5D0, 0xFF857050, 0xFFD3C3A3, 0xFFA3261A,
)

internal fun isKingStyle(ui: AppUiTheme): Boolean =
    ui == AppUiTheme.KING_NEON || ui == AppUiTheme.KING_GLASS || ui == AppUiTheme.KING_CLASSIC

internal fun kingStyleScheme(ui: AppUiTheme, dark: Boolean, black: Boolean): ColorScheme {
    val p = when (ui) {
        AppUiTheme.KING_NEON -> if (dark) NeonDark else NeonLight
        AppUiTheme.KING_GLASS -> if (dark) GlassDark else GlassLight
        else -> if (dark) ClassicDark else ClassicLight
    }
    val bg = if (dark && black) Color.Black else c(p.bg)
    val surface = if (dark && black) Color.Black else c(p.surface)
    return if (dark) {
        darkColorScheme(
            primary = c(p.primary), onPrimary = c(p.onPrimary), primaryContainer = c(p.primaryContainer),
            onPrimaryContainer = c(p.onPrimaryContainer), secondary = c(p.secondary), onSecondary = c(p.onSecondary),
            background = bg, onBackground = c(p.onBg), surface = surface, onSurface = c(p.onBg),
            surfaceVariant = c(p.variant), onSurfaceVariant = c(p.onVariant), surfaceContainer = c(p.container),
            outline = c(p.outline), outlineVariant = c(p.outlineVariant), error = c(p.error),
        )
    } else {
        lightColorScheme(
            primary = c(p.primary), onPrimary = c(p.onPrimary), primaryContainer = c(p.primaryContainer),
            onPrimaryContainer = c(p.onPrimaryContainer), secondary = c(p.secondary), onSecondary = c(p.onSecondary),
            background = bg, onBackground = c(p.onBg), surface = surface, onSurface = c(p.onBg),
            surfaceVariant = c(p.variant), onSurfaceVariant = c(p.onVariant), surfaceContainer = c(p.container),
            outline = c(p.outline), outlineVariant = c(p.outlineVariant), error = c(p.error),
        )
    }
}

internal fun kingStyleShapes(ui: AppUiTheme): Shapes = when (ui) {
    AppUiTheme.KING_NEON -> Shapes(
        RoundedCornerShape(1.dp), RoundedCornerShape(2.dp), RoundedCornerShape(3.dp),
        RoundedCornerShape(4.dp), RoundedCornerShape(6.dp),
    )
    AppUiTheme.KING_GLASS -> Shapes(
        RoundedCornerShape(12.dp), RoundedCornerShape(18.dp), RoundedCornerShape(24.dp),
        RoundedCornerShape(32.dp), RoundedCornerShape(44.dp),
    )
    else -> Shapes(
        RoundedCornerShape(2.dp), RoundedCornerShape(4.dp), RoundedCornerShape(6.dp),
        RoundedCornerShape(8.dp), RoundedCornerShape(10.dp),
    )
}

private fun TextStyle.with(family: FontFamily, weight: FontWeight?) =
    copy(fontFamily = family, fontWeight = weight ?: fontWeight)

internal fun kingStyleTypography(ui: AppUiTheme): Typography {
    val base = Typography()
    val (family, heading) = when (ui) {
        AppUiTheme.KING_NEON -> FontFamily.Monospace to FontWeight.Bold
        AppUiTheme.KING_GLASS -> FontFamily.SansSerif to FontWeight.Light
        else -> FontFamily.Serif to FontWeight.Bold
    }
    return Typography(
        displayLarge = base.displayLarge.with(family, heading), displayMedium = base.displayMedium.with(family, heading),
        displaySmall = base.displaySmall.with(family, heading), headlineLarge = base.headlineLarge.with(family, heading),
        headlineMedium = base.headlineMedium.with(family, heading), headlineSmall = base.headlineSmall.with(family, heading),
        titleLarge = base.titleLarge.with(family, heading), titleMedium = base.titleMedium.with(family, heading),
        titleSmall = base.titleSmall.with(family, heading), bodyLarge = base.bodyLarge.with(family, null),
        bodyMedium = base.bodyMedium.with(family, null), bodySmall = base.bodySmall.with(family, null),
        labelLarge = base.labelLarge.with(family, heading), labelMedium = base.labelMedium.with(family, null),
        labelSmall = base.labelSmall.with(family, null),
    )
}
