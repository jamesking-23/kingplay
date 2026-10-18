package com.kingplay.desktop

import com.kingplay.core.model.BuiltInEqPresets
import com.kingplay.core.model.EqPreset
import com.kingplay.core.model.Track
import com.kingplay.core.model.backup.BackupCodec
import com.kingplay.core.model.backup.BackupData
import com.kingplay.core.model.lyrics.lyricsKey
import com.kingplay.core.model.settings.AppSettings
import com.kingplay.desktop.library.LibraryIndex
import com.kingplay.desktop.lyrics.LyricsResolver
import com.kingplay.desktop.lyrics.decodeLyricsBytes
import com.kingplay.desktop.lyrics.findSidecarLrc
import com.kingplay.desktop.lyrics.readSidecarLrc
import com.kingplay.desktop.settings.DesktopSettings
import com.kingplay.desktop.settings.SettingsStore
import com.kingplay.desktop.settings.flat
import com.kingplay.desktop.settings.matchingPreset
import com.kingplay.desktop.settings.withBand
import com.kingplay.desktop.settings.withPreamp
import com.kingplay.desktop.settings.withPreset
import com.kingplay.desktop.userdata.BackupExport
import com.kingplay.desktop.userdata.BackupImport
import com.kingplay.desktop.userdata.ImportOutcome
import com.kingplay.desktop.userdata.UserData
import com.kingplay.desktop.userdata.UserDataStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

private fun dir(): File = Files.createTempDirectory("kp-ly").toFile().also { it.deleteOnExit() }

private fun track(path: String = "C:/m/a.mp3", artist: String = "Ann", title: String = "Song") = Track(
    id = 1, uri = path, title = title, artistId = 1, artistName = artist, albumId = 1, albumTitle = "B",
    albumArtist = null, genre = null, durationMs = 1000, bitrateKbps = 0, sampleRateHz = 0, format = "mp3",
    trackNumber = 0, discNumber = 0, year = null, folderPath = "C:/m", dateAddedMs = 0, dateModifiedMs = 0,
)

private const val SYNCED = "[00:01.00]one\n[00:02.00]two"

class LyricsSourcesTest {
    @Test fun decodesBomsAndPlainUtf8() {
        assertEquals("héllo", decodeLyricsBytes("héllo".toByteArray(Charsets.UTF_8)))
        val utf8Bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val leBom = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        val beBom = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
        assertEquals("hi", decodeLyricsBytes(utf8Bom + "hi".toByteArray()))
        assertEquals("hi", decodeLyricsBytes(leBom + "hi".toByteArray(Charsets.UTF_16LE)))
        assertEquals("hi", decodeLyricsBytes(beBom + "hi".toByteArray(Charsets.UTF_16BE)))
    }

    @Test fun findsSidecarIgnoringLetterCase() {
        val d = dir()
        File(d, "Song.MP3").writeText("x")
        File(d, "song.LRC").writeText(SYNCED)
        assertEquals("song.LRC", findSidecarLrc(File(d, "Song.MP3"))?.name)
        assertEquals(SYNCED, readSidecarLrc(File(d, "Song.MP3")))
    }

    @Test fun missingOrEmptySidecarIsNull() {
        val d = dir()
        File(d, "a.mp3").writeText("x")
        assertNull(findSidecarLrc(File(d, "a.mp3")))
        File(d, "a.lrc").writeText("")
        assertNull(findSidecarLrc(File(d, "a.mp3")))
        assertNull(readSidecarLrc(File(d, "nofolder/none.mp3")))
    }

    @Test fun importedBeatsSidecarBeatsEmbedded() {
        val t = track()
        val resolver = LyricsResolver(readSidecar = { "[00:01.00]sidecar" }, readEmbedded = { "embedded text" })
        val key = lyricsKey("Ann", "Song")
        assertEquals("mine", resolver.resolve(t, mapOf(key to "mine"))!!.lines.single().text)
        assertEquals("sidecar", resolver.resolve(t, emptyMap())!!.lines.single().text)
        val embeddedOnly = LyricsResolver(readSidecar = { null }, readEmbedded = { "embedded text" })
        assertEquals("embedded text", embeddedOnly.resolve(t, emptyMap())!!.lines.single().text)
        assertFalse(embeddedOnly.resolve(t, emptyMap())!!.synced)
    }

    @Test fun unusableSourcesFallThroughAndNothingMeansNull() {
        val t = track()
        val resolver = LyricsResolver(readSidecar = { "   " }, readEmbedded = { null })
        assertNull(resolver.resolve(t, mapOf(LyricsResolver.keyOf(t) to "  ")))
        val second = LyricsResolver(readSidecar = { "   " }, readEmbedded = { SYNCED })
        assertTrue(second.resolve(t, mapOf(LyricsResolver.keyOf(t) to "  "))!!.synced)
    }

    @Test fun keyIgnoresCaseAndSpacing() {
        assertEquals(LyricsResolver.keyOf(track(artist = "ANN ", title = " song")), LyricsResolver.keyOf(track()))
    }
}

class LyricsDataTest {
    private val song = track(path = "C:\\Music\\a.mp3")
    private val library = LibraryIndex(listOf(song.copy(folderPath = "C:\\Music")))

    @Test fun userDataKeepsLyricsAcrossSaves() {
        val d = dir()
        val store = UserDataStore(File(d, "u.json"))
        val data = UserData().withLyrics("k", SYNCED).withLyrics("j", "x").withoutLyrics("j")
        store.save(data)
        assertEquals(mapOf("k" to SYNCED), store.load().lyrics)
    }

    @Test fun exportAndImportCarryLyricsWithoutOverwriting() {
        val key = LyricsResolver.keyOf(song)
        val text = BackupExport.build(library, UserData().withLyrics(key, SYNCED), now = 1)
        assertEquals(mapOf(key to SYNCED), BackupCodec.decode(text).lyrics)
        val fresh = BackupImport.import(text, library, UserData(), now = 2) as ImportOutcome.Done
        assertEquals(SYNCED, fresh.data.lyrics[key])
        assertEquals(1, fresh.summary.lyricsAdded)
        val mine = UserData().withLyrics(key, "mine")
        val existing = BackupImport.import(text, library, mine, now = 2) as ImportOutcome.Done
        assertEquals("mine", existing.data.lyrics[key])
        assertEquals(0, existing.summary.lyricsAdded)
    }

    @Test fun importReadsAndroidBackupLyrics() {
        val text = BackupCodec.encode(
            BackupData(0, AppSettings(), emptyList(), emptyList(), emptyList(), emptyList(), mapOf("a|b" to SYNCED)),
        )
        val done = BackupImport.import(text, library, UserData(), now = 1) as ImportOutcome.Done
        assertEquals(mapOf("a|b" to SYNCED), done.data.lyrics)
    }
}

class EqLogicTest {
    @Test fun bandAndPreampAreClamped() {
        val s = DesktopSettings().withBand(2, 99f).withBand(3, -99f).withPreamp(50f)
        assertEquals(EqPreset.MAX_GAIN_DB, s.eqGainsDb[2], 0f)
        assertEquals(EqPreset.MIN_GAIN_DB, s.eqGainsDb[3], 0f)
        assertEquals(EqPreset.MAX_GAIN_DB, s.eqPreampDb, 0f)
        assertEquals(s, s.withBand(-1, 5f).withBand(10, 5f))
        assertEquals(10, s.eqGainsDb.size)
    }

    @Test fun presetsApplyAndAreRecognisedUntilASliderMoves() {
        val rock = BuiltInEqPresets.all.first { it.name == "Rock" }
        val s = DesktopSettings().withPreset(rock)
        assertEquals(rock.bandGainsDb, s.eqGainsDb)
        assertEquals(rock, s.matchingPreset())
        assertNull(s.withBand(0, s.eqGainsDb[0] + 1f).matchingPreset())
        assertEquals(EqPreset.Flat, s.flat().matchingPreset())
        assertEquals(EqPreset.Flat, DesktopSettings().matchingPreset())
    }

    @Test fun equalizerSettingsRoundTripAndBadListsGoFlat() {
        val d = dir()
        val store = SettingsStore(File(d, "s.json"))
        val s = DesktopSettings().withPreset(BuiltInEqPresets.all[3]).withPreamp(-2.5f).copy(eqEnabled = true)
        store.save(s)
        assertEquals(s, store.load())
        File(d, "bad.json").writeText("""{"eqEnabled": true, "eqGainsDb": [1, 2, 3], "eqPreampDb": 99}""")
        val bad = SettingsStore(File(d, "bad.json")).load()
        assertTrue(bad.eqEnabled)
        assertEquals(DesktopSettings.FLAT_BANDS, bad.eqGainsDb)
        assertEquals(EqPreset.MAX_GAIN_DB, bad.eqPreampDb, 0f)
        File(d, "worse.json").writeText("""{"eqGainsDb": [1,2,3,4,5,6,7,8,9,"x"]}""")
        assertEquals(DesktopSettings.FLAT_BANDS, SettingsStore(File(d, "worse.json")).load().eqGainsDb)
    }
}
