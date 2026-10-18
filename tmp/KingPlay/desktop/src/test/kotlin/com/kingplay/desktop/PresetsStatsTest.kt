package com.kingplay.desktop

import com.kingplay.core.model.EqPreset
import com.kingplay.core.model.Track
import com.kingplay.core.model.backup.BackupCodec
import com.kingplay.core.model.backup.BackupData
import com.kingplay.core.model.backup.BackupPlayStat
import com.kingplay.core.model.backup.BackupPreset
import com.kingplay.core.model.backup.BackupTrack
import com.kingplay.core.model.settings.AppSettings
import com.kingplay.desktop.library.LibraryIndex
import com.kingplay.desktop.settings.DesktopSettings
import com.kingplay.desktop.settings.asUserPreset
import com.kingplay.desktop.settings.matchingUserPreset
import com.kingplay.desktop.settings.withBand
import com.kingplay.desktop.settings.withUserPreset
import com.kingplay.desktop.userdata.BackupExport
import com.kingplay.desktop.userdata.BackupImport
import com.kingplay.desktop.userdata.ImportOutcome
import com.kingplay.desktop.userdata.ImportSummary
import com.kingplay.desktop.userdata.PlayStat
import com.kingplay.desktop.userdata.UserData
import com.kingplay.desktop.userdata.UserDataStore
import com.kingplay.desktop.userdata.UserEqPreset
import com.kingplay.desktop.userdata.importNotice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun song(id: Long, name: String) = Track(
    id = id, uri = "C:\\Music\\$name", title = name, artistId = 1, artistName = "Ann", albumId = 1, albumTitle = "B",
    albumArtist = null, genre = null, durationMs = 1000, bitrateKbps = 0, sampleRateHz = 0, format = "mp3",
    trackNumber = 0, discNumber = 0, year = null, folderPath = "C:\\Music", dateAddedMs = 0, dateModifiedMs = 0,
)

private val GAINS = List(EqPreset.BAND_COUNT) { it.toFloat() - 4f }

class UserEqPresetTest {
    @Test fun savingReplacesSameNameIgnoringCaseAndRefusesBadInput() {
        val a = UserEqPreset("Warm", 1f, GAINS)
        val data = UserData().withEqPreset(a).withEqPreset(UserEqPreset(" warm ", 2f, GAINS))
        assertEquals(listOf(UserEqPreset("warm", 2f, GAINS)), data.eqPresets)
        assertEquals(data, data.withEqPreset(UserEqPreset("  ", 0f, GAINS)))
        assertEquals(data, data.withEqPreset(UserEqPreset("short", 0f, listOf(1f))))
        assertTrue(data.withoutEqPreset("WARM").eqPresets.isEmpty())
    }

    @Test fun listStopsAtTheLimit() {
        var data = UserData()
        repeat(UserData.MAX_EQ_PRESETS + 5) { data = data.withEqPreset(UserEqPreset("p$it", 0f, GAINS)) }
        assertEquals(UserData.MAX_EQ_PRESETS, data.eqPresets.size)
        assertEquals(data, data.withEqPreset(UserEqPreset("another", 0f, GAINS)))
        assertEquals("p0", data.withEqPreset(UserEqPreset("P0", 3f, GAINS)).eqPresets.last().name.lowercase())
    }

    @Test fun presetsSurviveTheFileAndBadOnesAreDropped() {
        val dir = java.nio.file.Files.createTempDirectory("kp-eq").toFile()
        val store = UserDataStore(java.io.File(dir, "u.json"))
        store.save(UserData().withEqPreset(UserEqPreset("Warm", -2.5f, GAINS)))
        assertEquals(listOf(UserEqPreset("Warm", -2.5f, GAINS)), store.load().eqPresets)
        val damaged = mapOf(
            "eqPresets" to listOf(
                mapOf("name" to "ok", "preampDb" to 99, "gainsDb" to List(10) { 50 }),
                mapOf("name" to "short", "gainsDb" to listOf(1, 2)),
                mapOf("name" to "", "gainsDb" to List(10) { 0 }),
                mapOf("name" to "OK", "gainsDb" to List(10) { 0 }),
                "junk",
            ),
        )
        val loaded = UserDataStore.decode(damaged).eqPresets
        assertEquals(listOf("ok"), loaded.map { it.name })
        assertEquals(EqPreset.MAX_GAIN_DB, loaded.single().preampDb, 0f)
        assertTrue(loaded.single().gainsDb.all { it == EqPreset.MAX_GAIN_DB })
    }

    @Test fun settingsMatchSavedPresetsUntilASliderMoves() {
        val preset = UserEqPreset("Warm", 1f, GAINS)
        val settings = DesktopSettings().withUserPreset(preset)
        assertEquals(preset, settings.matchingUserPreset(listOf(preset)))
        assertEquals(null, settings.withBand(0, 0f).matchingUserPreset(listOf(preset)))
        assertEquals(UserEqPreset("Mine", 1f, GAINS), settings.asUserPreset("  Mine "))
    }
}

class BackupPresetsStatsTest {
    private val library = LibraryIndex(listOf(song(1, "a.mp3"), song(2, "b.mp3")))

    @Test fun exportCarriesPresetsAndCountsForKnownSongsOnly() {
        val data = UserData(stats = mapOf(1L to PlayStat(4, 99), 2L to PlayStat(0, 0), 9L to PlayStat(7, 1)))
            .withEqPreset(UserEqPreset("Warm", 1f, GAINS))
        val back = BackupCodec.decode(BackupExport.build(library, data, now = 5))
        assertEquals(listOf(BackupPreset("Warm", 1f, GAINS)), back.presets)
        assertEquals(listOf(BackupPlayStat(BackupTrack("C:/Music/a.mp3", "a.mp3", "Ann"), 4, 99)), back.playStats)
    }

    @Test fun importIsIdempotentAndKeepsLargerCounts() {
        val text = BackupExport.build(
            library,
            UserData(stats = mapOf(1L to PlayStat(4, 99))).withEqPreset(UserEqPreset("Warm", 1f, GAINS)),
            now = 1,
        )
        val first = BackupImport.import(text, library, UserData(), now = 2) as ImportOutcome.Done
        assertEquals(PlayStat(4, 99), first.data.stats[1L])
        assertEquals(1, first.summary.presetsAdded)
        assertEquals(1, first.summary.statsMatched)
        val again = BackupImport.import(text, library, first.data, now = 3) as ImportOutcome.Done
        assertEquals(first.data.stats, again.data.stats)
        assertEquals(0, again.summary.presetsAdded)
        val higher = UserData(stats = mapOf(1L to PlayStat(10, 50)))
        val merged = (BackupImport.import(text, library, higher, now = 3) as ImportOutcome.Done).data.stats[1L]
        assertEquals(PlayStat(10, 99), merged)
    }

    @Test fun existingPresetOfTheSameNameIsKept() {
        val text = BackupCodec.encode(
            BackupData(
                0, AppSettings(), emptyList(), emptyList(), listOf(BackupPreset("WARM", 5f, GAINS)), emptyList(),
                emptyMap(),
            ),
        )
        val mine = UserData().withEqPreset(UserEqPreset("warm", 1f, GAINS))
        val done = BackupImport.import(text, library, mine, now = 1) as ImportOutcome.Done
        assertEquals(1f, done.data.eqPresets.single().preampDb, 0f)
        assertEquals(0, done.summary.presetsAdded)
    }

    @Test fun backupWithoutStatsStillReadsAndPhoneFilesAreUnaffected() {
        val empty = emptyList<Nothing>()
        val plain = BackupCodec.encode(BackupData(0, AppSettings(), empty, empty, empty, empty, emptyMap()))
        assertTrue(BackupCodec.decode(plain).playStats.isEmpty())
        assertTrue(!plain.contains("playStats"))
    }

    @Test fun noticeMentionsExtrasOnlyWhenPresent() {
        val base = ImportSummary(1, 0, 2, 3, 4, 5)
        assertTrue(!importNotice(base).contains("equalizer"))
        val full = importNotice(base.copy(presetsAdded = 2, lyricsAdded = 3, statsMatched = 4))
        assertTrue(full.contains("2 equalizer presets") && full.contains("3 songs") && full.contains("4 songs"))
    }
}
