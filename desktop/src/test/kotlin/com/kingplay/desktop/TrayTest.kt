package com.kingplay.desktop

import com.kingplay.core.model.repository.PlaybackState
import com.kingplay.desktop.settings.DesktopSettings
import com.kingplay.desktop.settings.SettingsStore
import com.kingplay.desktop.ui.CloseAction
import com.kingplay.desktop.ui.closeAction
import com.kingplay.desktop.ui.trayTooltip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class TrayTest {
    @Test fun tooltipNamesTheSongAndMarksPause() {
        assertEquals("KingPlay", trayTooltip(PlaybackState()))
        assertEquals("KingPlay", trayTooltip(PlaybackState(title = "  ")))
        assertEquals("Song — Ann", trayTooltip(PlaybackState(title = "Song", artist = "Ann", isPlaying = true)))
        assertEquals("Song (paused)", trayTooltip(PlaybackState(title = "Song", artist = " ")))
    }

    @Test fun longTooltipsAreCut() {
        val shown = trayTooltip(PlaybackState(title = "x".repeat(300), isPlaying = true))
        assertTrue(shown.length <= 120)
        assertTrue(shown.endsWith("…"))
    }

    @Test fun closingHidesOnlyWhenAskedAndATrayExists() {
        assertEquals(CloseAction.HideToTray, closeAction(closeToTray = true, trayAvailable = true))
        assertEquals(CloseAction.Quit, closeAction(closeToTray = true, trayAvailable = false))
        assertEquals(CloseAction.Quit, closeAction(closeToTray = false, trayAvailable = true))
    }

    @Test fun closeToTrayIsRemembered() {
        val file = File(Files.createTempDirectory("kp-tray").toFile(), "s.json")
        val store = SettingsStore(file)
        assertFalse(store.load().closeToTray)
        store.save(DesktopSettings(closeToTray = true))
        assertTrue(store.load().closeToTray)
    }
}
