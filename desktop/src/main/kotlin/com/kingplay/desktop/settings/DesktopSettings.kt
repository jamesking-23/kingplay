package com.kingplay.desktop.settings

import com.kingplay.core.model.settings.AppAccent
import com.kingplay.core.model.settings.AppThemeMode
import com.kingplay.desktop.library.JsonFile
import java.io.File

/** What the PC app remembers about how it looks and sounds. (Dynamic colour is an Android-only idea.) */
data class DesktopSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accent: AppAccent = AppAccent.VIOLET,
    val volumePercent: Int = DEFAULT_VOLUME,
) {
    fun withVolume(percent: Int): DesktopSettings = copy(volumePercent = percent.coerceIn(0, MAX_VOLUME))

    companion object {
        const val DEFAULT_VOLUME = 80
        const val MAX_VOLUME = 100
    }
}

class SettingsStore(private val file: File) {
    fun load(): DesktopSettings {
        val root = JsonFile.read(file) ?: return DesktopSettings()
        val defaults = DesktopSettings()
        return DesktopSettings(
            themeMode = enumOrNull<AppThemeMode>(root["themeMode"]) ?: defaults.themeMode,
            accent = enumOrNull<AppAccent>(root["accent"]) ?: defaults.accent,
            volumePercent = (root["volumePercent"] as? Number)?.toInt()?.coerceIn(0, DesktopSettings.MAX_VOLUME)
                ?: defaults.volumePercent,
        )
    }

    fun save(settings: DesktopSettings) = JsonFile.write(
        file,
        mapOf(
            "themeMode" to settings.themeMode.name,
            "accent" to settings.accent.name,
            "volumePercent" to settings.volumePercent.toLong(),
        ),
    )

    private inline fun <reified E : Enum<E>> enumOrNull(value: Any?): E? =
        (value as? String)?.let { name -> enumValues<E>().firstOrNull { it.name == name } }
}
