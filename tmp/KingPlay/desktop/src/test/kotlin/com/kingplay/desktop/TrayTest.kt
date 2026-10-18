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

class ReorderTest {
    @Test fun dragTargetCountsWholeRowsAndStaysInside() {
        assertEquals(3, com.kingplay.desktop.ui.dragTarget(2, 60f, 56f, 10))
        assertEquals(2, com.kingplay.desktop.ui.dragTarget(2, 20f, 56f, 10))
        assertEquals(1, com.kingplay.desktop.ui.dragTarget(2, -60f, 56f, 10))
        assertEquals(0, com.kingplay.desktop.ui.dragTarget(2, -5000f, 56f, 10))
        assertEquals(9, com.kingplay.desktop.ui.dragTarget(2, 5000f, 56f, 10))
        assertEquals(2, com.kingplay.desktop.ui.dragTarget(2, 100f, 0f, 10))
        assertEquals(4, com.kingplay.desktop.ui.dragTarget(4, 100f, 56f, 0))
    }

    @Test fun stepsStopAtTheEnds() {
        assertEquals(null, com.kingplay.desktop.ui.stepTarget(0, -1, 3))
        assertEquals(1, com.kingplay.desktop.ui.stepTarget(0, 1, 3))
        assertEquals(null, com.kingplay.desktop.ui.stepTarget(2, 1, 3))
        assertEquals(1, com.kingplay.desktop.ui.stepTarget(2, -1, 3))
        assertEquals(null, com.kingplay.desktop.ui.stepTarget(5, -1, 3))
    }

    @Test fun playlistMoveUsesRealPositions() {
        val (data, id) = com.kingplay.desktop.userdata.UserData().createPlaylist("p", 1L, listOf(10L, 11L, 12L, 13L))
        val moved = data.moveInPlaylist(id, 0, 3)
        assertEquals(listOf(11L, 12L, 13L, 10L), moved.playlist(id)!!.trackIds)
        assertEquals(data, data.moveInPlaylist(id, 0, 9))
    }
}
