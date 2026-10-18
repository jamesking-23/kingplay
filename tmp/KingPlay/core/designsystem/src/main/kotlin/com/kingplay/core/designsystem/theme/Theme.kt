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

data class ThemeConfig(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val accent: AccentTheme = AccentTheme.VIOLET,
    val density: LayoutDensity = LayoutDensity.COMFORTABLE,
    val lowPowerVisuals: Boolean = false,
)

object KpTheme {
    val spacing: Spacing
        @Composable get() = LocalSpacing.current

    val lowPowerVisuals: Boolean
        @Composable get() = LocalLowPowerVisuals.current
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
    val dynamic = config.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val base: ColorScheme = when {
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
        LocalSpacing provides if (config.density == LayoutDensity.COMPACT) CompactSpacing else ComfortableSpacing,
        LocalLowPowerVisuals provides effectiveLowPower(config.lowPowerVisuals, context.animatorScale()),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KingPlayTypography,
            shapes = KingPlayShapes,
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
