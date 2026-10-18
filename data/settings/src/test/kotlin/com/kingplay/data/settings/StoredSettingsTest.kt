package com.kingplay.data.settings

import com.kingplay.core.model.settings.AppAccent
import com.kingplay.core.model.settings.AppSettings
import com.kingplay.core.model.settings.AppThemeMode
import com.kingplay.core.model.settings.AppUiTheme
import org.junit.Assert.assertEquals
import org.junit.Test

class StoredSettingsTest {
    @Test
    fun settingsSurviveStorage() {
        val settings = AppSettings(AppThemeMode.AMOLED, AppAccent.EMERALD, dynamicColor = false, lowPowerVisuals = true, uiTheme = AppUiTheme.KK_UI)
        assertEquals(settings, settings.toStored().toSettings())
    }

    @Test
    fun kingArtIsTheDefaultDesign() {
        assertEquals(AppUiTheme.KING_ART, AppSettings().uiTheme)
        assertEquals(AppUiTheme.KING_ART, StoredSettings("DARK", null, null, null).toSettings().uiTheme)
        assertEquals(AppUiTheme.KING_ART, StoredSettings(null, null, null, null, "NOPE").toSettings().uiTheme)
    }

    @Test
    fun nothingStoredMeansDefaults() {
        assertEquals(AppSettings(), StoredSettings(null, null, null, null).toSettings())
    }

    @Test
    fun valuesFromAnotherVersionFallBackPerField() {
        val read = StoredSettings("HOLOGRAM", "OCEAN", null, true).toSettings()
        assertEquals(AppThemeMode.SYSTEM, read.themeMode)
        assertEquals(AppAccent.OCEAN, read.accent)
        assertEquals(true, read.dynamicColor)
        assertEquals(true, read.lowPowerVisuals)
    }
}
