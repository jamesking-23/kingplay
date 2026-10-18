package com.kingplay.feature.settings

import com.kingplay.core.designsystem.theme.AccentTheme
import com.kingplay.core.designsystem.theme.ThemeConfig
import com.kingplay.core.designsystem.theme.ThemeMode
import com.kingplay.core.model.settings.AppAccent
import com.kingplay.core.model.settings.AppSettings
import com.kingplay.core.model.settings.AppThemeMode

fun AppThemeMode.toTheme(): ThemeMode = when (this) {
    AppThemeMode.SYSTEM -> ThemeMode.SYSTEM
    AppThemeMode.LIGHT -> ThemeMode.LIGHT
    AppThemeMode.DARK -> ThemeMode.DARK
    AppThemeMode.AMOLED -> ThemeMode.AMOLED
}

fun AppAccent.toTheme(): AccentTheme = when (this) {
    AppAccent.VIOLET -> AccentTheme.VIOLET
    AppAccent.OCEAN -> AccentTheme.OCEAN
    AppAccent.EMBER -> AccentTheme.EMBER
    AppAccent.EMERALD -> AccentTheme.EMERALD
}

/** The look the whole app uses, built from the saved settings. */
fun AppSettings.toThemeConfig(): ThemeConfig = ThemeConfig(
    mode = themeMode.toTheme(),
    useDynamicColor = dynamicColor,
    accent = accent.toTheme(),
    lowPowerVisuals = lowPowerVisuals,
)
