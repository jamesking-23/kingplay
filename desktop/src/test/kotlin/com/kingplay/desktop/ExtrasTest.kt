package com.kingplay.desktop

import com.kingplay.core.model.Track
import com.kingplay.core.model.backup.BackupCodec
import com.kingplay.core.model.settings.AppAccent
import com.kingplay.core.model.settings.AppThemeMode
import com.kingplay.desktop.cover.LruCache
import com.kingplay.desktop.cover.findFolderCover
import com.kingplay.desktop.cover.scaledSize
import com.kingplay.desktop.library.LibraryIndex
import com.kingplay.desktop.settings.DesktopSettings
import com.kingplay.desktop.settings.SettingsStore
import com.kingplay.desktop.ui.accentArgb
import com.kingplay.desktop.ui.isDark
import com.kingplay.desktop.userdata.BackupExport
import com.kingplay.desktop.userdata.BackupImport
import com.kingplay.desktop.userdata.ImportOutcome
import com.kingplay.desktop.userdata.UserData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

private fun tempDir(): File = Files.createTempDirectory("kp-x").toFile().also { it.deleteOnExit() }

class SettingsTest {
    @Test fun roundTripsAndClampsVolume() {
        val file = File(tempDir(), "s.json")
        val store = SettingsStore(file)
        val s = DesktopSettings(AppThemeMode.AMOLED, AppAccent.EMBER, 35)
        store.save(s)
        assertEquals(s, store.load())
        assertEquals(0, DesktopSettings().withVolume(-5).volumePercent)
        assertEquals(100, DesktopSettings().withVolume(500).volumePercent)
    }

    @Test fun missingDamagedOrUnknownValuesFallBackToDefaults() {
        val dir = tempDir()
        assertEquals(DesktopSettings(), SettingsStore(File(dir, "none.json")).load())
        File(dir, "bad.json").writeText("{{{")
        assertEquals(DesktopSettings(), SettingsStore(File(dir, "bad.json")).load())
        File(dir, "odd.json").writeText("""{"themeMode": "NEON", "accent": "EMBER", "volumePercent": 900}""")
        val odd = SettingsStore(File(dir, "odd.json")).load()
        assertEquals(AppThemeMode.SYSTEM, odd.themeMode)
        assertEquals(AppAccent.EMBER, odd.accent)
        assertEquals(100, odd.volumePercent)
    }

    @Test fun themeChoices() {
        assertTrue(isDark(AppThemeMode.DARK, systemDark = false))
        assertTrue(isDark(AppThemeMode.AMOLED, systemDark = false))
        assertFalse(isDark(AppThemeMode.LIGHT, systemDark = true))
        assertTrue(isDark(AppThemeMode.SYSTEM, systemDark = true))
        assertFalse(isDark(AppThemeMode.SYSTEM, systemDark = false))
        AppAccent.entries.forEach { assertNotEquals(accentArgb(it, true), accentArgb(it, false)) }
        assertEquals(AppAccent.entries.size, AppAccent.entries.map { accentArgb(it, true) }.distinct().size)
    }
}

class CoverLogicTest {
    @Test fun prefersCoverOverFolderAndIgnoresOtherFiles() {
        val dir = tempDir()
        File(dir, "notes.txt").writeText("x")
        File(dir, "Folder.JPG").writeText("x")
        File(dir, "cover.png").writeText("x")
        assertEquals("cover.png", findFolderCover(dir)?.name)
    }

    @Test fun noPictureOrMissingFolderMeansNull() {
        val dir = tempDir()
        File(dir, "song.mp3").writeText("x")
        assertNull(findFolderCover(dir))
        assertNull(findFolderCover(File(dir, "nope")))
    }

    @Test fun emptyImageFilesAreSkipped() {
        val dir = tempDir()
        File(dir, "cover.jpg").writeText("")
        File(dir, "front.jpg").writeText("x")
        assertEquals("front.jpg", findFolderCover(dir)?.name)
    }

    @Test fun lruDropsTheLeastRecentlyUsed() {
        val c = LruCache<Int, String>(2)
        c[1] = "a"
        c[2] = "b"
        c[1]
        c[3] = "c"
        assertTrue(c.contains(1))
        assertFalse(c.contains(2))
        assertEquals(2, c.size)
    }

    @Test fun scalingNeverEnlargesAndKeepsShape() {
        assertEquals(100 to 50, scaledSize(100, 50, 320))
        assertEquals(320 to 160, scaledSize(1000, 500, 320))
        assertEquals(160 to 320, scaledSize(500, 1000, 320))
        assertEquals(1 to 1, scaledSize(0, 5, 320))
        assertEquals(320 to 1, scaledSize(100_000, 10, 320))
    }
}

class ExportTest {
    private fun song(id: Long, title: String, path: String) = Track(
        id = id, uri = path, title = title, artistId = 1, artistName = "Ann", albumId = 1, albumTitle = "B",
        albumArtist = null, genre = null, durationMs = 1000, bitrateKbps = 0, sampleRateHz = 0, format = "mp3",
        trackNumber = 0, discNumber = 0, year = null, folderPath = path.substringBeforeLast('\\'),
        dateAddedMs = 0, dateModifiedMs = 0,
    )

    private val library = LibraryIndex(
        listOf(song(1, "One", "C:\\Music\\Rock\\one.mp3"), song(2, "Two", "C:\\Music\\Pop\\two.mp3")),
    )

    @Test fun exportIsMarkedDesktopAndUsesForwardSlashes() {
        val data = UserData().createPlaylist("Mix", 5, listOf(1, 2, 99)).first.toggleFavorite(2)
        val backup = BackupCodec.decode(BackupExport.build(library, data, now = 7))
        assertEquals(BackupCodec.SOURCE_DESKTOP, backup.source)
        assertEquals(7, backup.createdAtMs)
        val playlist = backup.playlists.single()
        assertEquals("Mix", playlist.name)
        assertEquals(listOf("C:/Music/Rock/one.mp3", "C:/Music/Pop/two.mp3"), playlist.tracks.map { it.path })
        assertEquals(listOf("C:/Music/Pop/two.mp3"), backup.favorites.map { it.path })
        assertEquals("Ann", playlist.tracks.first().artist)
    }

    @Test fun exportThenImportOnAnotherPcRestoresPlaylistsAndFavorites() {
        val data = UserData().createPlaylist("Mix", 5, listOf(1, 2)).first.toggleFavorite(1)
        val text = BackupExport.build(library, data, now = 1)
        val other = LibraryIndex(
            listOf(song(10, "One", "D:\\Songs\\Rock\\one.mp3"), song(20, "Two", "D:\\Songs\\Pop\\two.mp3")),
        )
        val done = BackupImport.import(text, other, UserData(), now = 2) as ImportOutcome.Done
        assertEquals(listOf(10L, 20L), done.data.playlists.single().trackIds)
        assertEquals(setOf(10L), done.data.favorites)
    }

    @Test fun emptyDataExportsAValidBackup() {
        val backup = BackupCodec.decode(BackupExport.build(LibraryIndex.Empty, UserData(), now = 1))
        assertTrue(backup.playlists.isEmpty() && backup.favorites.isEmpty())
    }
}
