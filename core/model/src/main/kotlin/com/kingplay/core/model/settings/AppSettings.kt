package com.kingplay.core.model.settings

/** Theme choices, kept here as plain enums so the settings can be stored and backed up without the UI toolkit. */
enum class AppThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

/**
 * The overall design language, like switching between two apps that share one engine.
 * [KING_ART] is the hand-drawn paper look (default); [KK_UI] is the original Material design.
 */
enum class AppUiTheme { KING_ART, KK_UI }

enum class AppAccent { VIOLET, OCEAN, EMBER, EMERALD }

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accent: AppAccent = AppAccent.VIOLET,
    val dynamicColor: Boolean = true,
    val lowPowerVisuals: Boolean = false,
    val uiTheme: AppUiTheme = AppUiTheme.KING_ART,
)
