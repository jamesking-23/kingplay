package com.kingplay.core.model.settings

/** Theme choices, kept here as plain enums so the settings can be stored and backed up without the UI toolkit. */
enum class AppThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

enum class AppAccent { VIOLET, OCEAN, EMBER, EMERALD }

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accent: AppAccent = AppAccent.VIOLET,
    val dynamicColor: Boolean = true,
    val lowPowerVisuals: Boolean = false,
)
