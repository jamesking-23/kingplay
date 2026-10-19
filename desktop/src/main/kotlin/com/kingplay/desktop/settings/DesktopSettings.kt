package com.kingplay.desktop.settings

import com.kingplay.core.model.EqPreset
import com.kingplay.core.model.settings.AppAccent
import com.kingplay.core.model.settings.AppThemeMode
import com.kingplay.core.model.settings.AppUiTheme
import com.kingplay.desktop.library.JsonFile
import java.io.File

/** What the PC app remembers about how it looks and sounds. (Dynamic colour is an Android-only idea.) */
data class DesktopSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accent: AppAccent = AppAccent.VIOLET,
    val volumePercent: Int = DEFAULT_VOLUME,
    val eqEnabled: Boolean = false,
    val eqPreampDb: Float = 0f,
    val eqGainsDb: List<Float> = FLAT_BANDS,
    val closeToTray: Boolean = false,
    val globalMediaKeys: Boolean = true,
    val uiTheme: AppUiTheme = AppUiTheme.KING_ART,
) {
    fun withVolume(percent: Int): DesktopSettings = copy(volumePercent = percent.coerceIn(0, MAX_VOLUME))

    companion object {
        const val DEFAULT_VOLUME = 80
        const val MAX_VOLUME = 100
        val FLAT_BANDS: List<Float> = List(EqPreset.BAND_COUNT) { 0f }
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
            eqEnabled = root["eqEnabled"] as? Boolean ?: false,
            eqPreampDb = clampGain((root["eqPreampDb"] as? Number)?.toFloat() ?: 0f),
            eqGainsDb = gains(root["eqGainsDb"]),
            closeToTray = root["closeToTray"] as? Boolean ?: false,
            globalMediaKeys = root["globalMediaKeys"] as? Boolean ?: true,
            uiTheme = enumOrNull<AppUiTheme>(root["uiTheme"]) ?: defaults.uiTheme,
        )
    }

    fun save(settings: DesktopSettings) = JsonFile.write(
        file,
        mapOf(
            "uiTheme" to settings.uiTheme.name,
            "themeMode" to settings.themeMode.name,
            "accent" to settings.accent.name,
            "volumePercent" to settings.volumePercent.toLong(),
            "eqEnabled" to settings.eqEnabled,
            "eqPreampDb" to settings.eqPreampDb,
            "eqGainsDb" to settings.eqGainsDb,
            "closeToTray" to settings.closeToTray,
            "globalMediaKeys" to settings.globalMediaKeys,
        ),
    )

    /** Exactly ten numbers or flat: a half-written list must never reach the equalizer. */
    private fun gains(value: Any?): List<Float> {
        val list = (value as? List<*>)?.map { (it as? Number)?.toFloat() }
        if (list == null || list.size != EqPreset.BAND_COUNT || list.any { it == null }) {
            return DesktopSettings.FLAT_BANDS
        }
        return list.map { clampGain(it!!) }
    }

    private inline fun <reified E : Enum<E>> enumOrNull(value: Any?): E? =
        (value as? String)?.let { name -> enumValues<E>().firstOrNull { it.name == name } }
}
