package com.kingplay.feature.settings

import com.kingplay.core.designsystem.theme.AccentTheme
import com.kingplay.core.designsystem.theme.ThemeConfig
import com.kingplay.core.designsystem.theme.ThemeMode
import com.kingplay.core.model.settings.AppAccent
import com.kingplay.core.model.settings.AppSettings
import com.kingplay.core.model.settings.AppThemeMode
import com.kingplay.core.model.settings.AppUiTheme
import com.kingplay.core.designsystem.theme.UiStyle

fun AppThemeMode.toTheme(): ThemeMode = when (this) {
    AppThemeMode.SYSTEM -> ThemeMode.SYSTEM
    AppThemeMode.LIGHT -> ThemeMode.LIGHT
    AppThemeMode.DARK -> ThemeMode.DARK
    AppThemeMode.AMOLED -> ThemeMode.AMOLED
}

fun AppUiTheme.toStyle(): UiStyle = when (this) {
    AppUiTheme.KING_ART -> UiStyle.KING_ART
    AppUiTheme.KK_UI -> UiStyle.KK_UI
    AppUiTheme.KING_NEON -> UiStyle.KING_NEON
    AppUiTheme.KING_GLASS -> UiStyle.KING_GLASS
    AppUiTheme.KING_CLASSIC -> UiStyle.KING_CLASSIC
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
    uiStyle = uiTheme.toStyle(),
)
