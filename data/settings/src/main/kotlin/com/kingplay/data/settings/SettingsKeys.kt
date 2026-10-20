package com.kingplay.data.settings

import com.kingplay.core.model.settings.AppAccent
import com.kingplay.core.model.settings.AppFont
import com.kingplay.core.model.settings.AppSettings
import com.kingplay.core.model.settings.AppThemeMode
import com.kingplay.core.model.settings.AppUiTheme
import com.kingplay.core.model.settings.HapticLevel
import com.kingplay.core.model.settings.HapticSettings

/** Raw stored values and how they become [AppSettings]. Unknown or missing values fall back to the defaults. */
data class StoredSettings(
    val themeMode: String?,
    val accent: String?,
    val dynamicColor: Boolean?,
    val lowPowerVisuals: Boolean?,
    val uiTheme: String? = null,
    val font: String? = null,
    val textScale: Float? = null,
    val customAccent: Long? = null,
    val hapticsEnabled: Boolean? = null,
    val hapticPlay: String? = null,
    val hapticTransport: String? = null,
    val hapticToggle: String? = null,
    val hapticTap: String? = null,
    val nickname: String? = null,
    val profilePhoto: String? = null,
)

private fun level(name: String?, fallback: HapticLevel) = HapticLevel.entries.firstOrNull { it.name == name } ?: fallback

fun StoredSettings.toSettings(): AppSettings {
    val defaults = AppSettings()
    val h = defaults.haptics
    return AppSettings(
        themeMode = AppThemeMode.entries.firstOrNull { it.name == themeMode } ?: defaults.themeMode,
        accent = AppAccent.entries.firstOrNull { it.name == accent } ?: defaults.accent,
        dynamicColor = dynamicColor ?: defaults.dynamicColor,
        lowPowerVisuals = lowPowerVisuals ?: defaults.lowPowerVisuals,
        uiTheme = AppUiTheme.entries.firstOrNull { it.name == uiTheme } ?: defaults.uiTheme,
        font = AppFont.entries.firstOrNull { it.name == font } ?: defaults.font,
        textScale = (textScale ?: defaults.textScale).coerceIn(AppSettings.MIN_TEXT_SCALE, AppSettings.MAX_TEXT_SCALE),
        customAccent = customAccent,
        haptics = HapticSettings(
            enabled = hapticsEnabled ?: h.enabled,
            play = level(hapticPlay, h.play),
            transport = level(hapticTransport, h.transport),
            toggle = level(hapticToggle, h.toggle),
            tap = level(hapticTap, h.tap),
        ),
        nickname = nickname.orEmpty().trim().take(AppSettings.MAX_NICKNAME),
        profilePhoto = profilePhoto?.ifBlank { null },
    )
}

fun AppSettings.toStored() = StoredSettings(
    themeMode = themeMode.name,
    accent = accent.name,
    dynamicColor = dynamicColor,
    lowPowerVisuals = lowPowerVisuals,
    uiTheme = uiTheme.name,
    font = font.name,
    textScale = textScale,
    customAccent = customAccent,
    hapticsEnabled = haptics.enabled,
    hapticPlay = haptics.play.name,
    hapticTransport = haptics.transport.name,
    hapticToggle = haptics.toggle.name,
    hapticTap = haptics.tap.name,
    nickname = nickname,
    profilePhoto = profilePhoto,
)
