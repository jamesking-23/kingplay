package com.kingplay.core.designsystem.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

/** The design language: [KING_ART] hand-drawn paper (default) or [KK_UI] the original Material look. */
enum class UiStyle { KING_ART, KK_UI, KING_NEON, KING_GLASS, KING_CLASSIC }

data class ThemeConfig(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val accent: AccentTheme = AccentTheme.VIOLET,
    val density: LayoutDensity = LayoutDensity.COMFORTABLE,
    val lowPowerVisuals: Boolean = false,
    val uiStyle: UiStyle = UiStyle.KING_ART,
)

object KpTheme {
    val spacing: Spacing
        @Composable get() = LocalSpacing.current

    val lowPowerVisuals: Boolean
        @Composable get() = LocalLowPowerVisuals.current

    val uiStyle: UiStyle
        @Composable get() = LocalUiStyle.current
}

@Composable
fun KingPlayTheme(
    config: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (config.mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val context = LocalContext.current
    val kingArt = config.uiStyle == UiStyle.KING_ART
    // King Art has its own palette, so wallpaper colours and accents only apply to kk-ui.
    val dynamic = config.uiStyle == UiStyle.KK_UI && config.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val base: ColorScheme = when {
        config.uiStyle == UiStyle.KING_NEON -> kingNeonColorScheme(dark)
        config.uiStyle == UiStyle.KING_GLASS -> kingGlassColorScheme(dark)
        config.uiStyle == UiStyle.KING_CLASSIC -> kingClassicColorScheme(dark)
        kingArt -> kingArtColorScheme(dark)
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        dark -> darkAccentScheme(config.accent)
        else -> lightAccentScheme(config.accent)
    }
    val colorScheme = if (dark && config.mode == ThemeMode.AMOLED) base.toAmoled() else base

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(
        LocalUiStyle provides config.uiStyle,
        LocalSpacing provides if (config.density == LayoutDensity.COMPACT) CompactSpacing else ComfortableSpacing,
        LocalLowPowerVisuals provides effectiveLowPower(config.lowPowerVisuals, context.animatorScale()),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = when (config.uiStyle) {
                UiStyle.KING_ART -> KingArtTypography
                UiStyle.KING_NEON -> KingNeonTypography
                UiStyle.KING_GLASS -> KingGlassTypography
                UiStyle.KING_CLASSIC -> KingClassicTypography
                UiStyle.KK_UI -> KingPlayTypography
            },
            shapes = when (config.uiStyle) {
                UiStyle.KING_ART -> KingArtShapes
                UiStyle.KING_NEON -> KingNeonShapes
                UiStyle.KING_GLASS -> KingGlassShapes
                UiStyle.KING_CLASSIC -> KingClassicShapes
                UiStyle.KK_UI -> KingPlayShapes
            },
            content = content,
        )
    }
}

/** The system "remove animations" switch sets the animator scale to 0; honour it like the in-app low-power switch. */
private fun Context.animatorScale(): Float =
    runCatching { Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }
        .getOrDefault(1f)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
