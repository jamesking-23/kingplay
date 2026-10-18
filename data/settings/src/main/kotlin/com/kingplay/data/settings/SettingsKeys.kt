package com.kingplay.data.settings

import com.kingplay.core.model.settings.AppAccent
import com.kingplay.core.model.settings.AppSettings
import com.kingplay.core.model.settings.AppThemeMode
import com.kingplay.core.model.settings.AppUiTheme

/** Raw stored values and how they become [AppSettings]. Unknown or missing values fall back to the defaults. */
data class StoredSettings(
    val themeMode: String?,
    val accent: String?,
    val dynamicColor: Boolean?,
    val lowPowerVisuals: Boolean?,
    val uiTheme: String? = null,
)

fun StoredSettings.toSettings(): AppSettings {
    val defaults = AppSettings()
    return AppSettings(
        themeMode = AppThemeMode.entries.firstOrNull { it.name == themeMode } ?: defaults.themeMode,
        accent = AppAccent.entries.firstOrNull { it.name == accent } ?: defaults.accent,
        dynamicColor = dynamicColor ?: defaults.dynamicColor,
        lowPowerVisuals = lowPowerVisuals ?: defaults.lowPowerVisuals,
        uiTheme = AppUiTheme.entries.firstOrNull { it.name == uiTheme } ?: defaults.uiTheme,
    )
}

fun AppSettings.toStored() = StoredSettings(themeMode.name, accent.name, dynamicColor, lowPowerVisuals, uiTheme.name)
