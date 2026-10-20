package com.kingplay.core.model

import com.kingplay.core.model.backup.BackupCodec
import com.kingplay.core.model.backup.BackupData
import com.kingplay.core.model.backup.BackupFormatException
import com.kingplay.core.model.backup.BackupPlaylist
import com.kingplay.core.model.backup.BackupPreset
import com.kingplay.core.model.backup.BackupTrack
import com.kingplay.core.model.backup.Json
import com.kingplay.core.model.lyrics.lyricsKey
import com.kingplay.core.model.settings.AppAccent
import com.kingplay.core.model.settings.AppSettings
import com.kingplay.core.model.settings.AppThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class JsonTest {
    @Test
    fun valuesSurviveAWriteAndRead() {
        val value = linkedMapOf(
            "text" to "line one\nline \"two\" \\ tab\t é 😀 \u0001",
            "n" to 42L,
            "d" to -1.5,
            "ok" to true,
            "none" to null,
            "list" to listOf(1L, "a", listOf<Any?>(), emptyMap<String, Any?>()),
        )
        assertEquals(value, Json.parse(Json.write(value)))
    }

    @Test
    fun floatsDoNotPickUpNoise() {
        assertEquals("0.1", Json.write(0.1f))
        assertEquals("3.0", Json.write(3f))
    }

    @Test
    fun readsEscapesAndWhitespace() {
        assertEquals(mapOf("a" to "A/\n"), Json.parse(" { \"a\" : \"\\u0041\\/\\n\" } "))
    }

    @Test
    fun rejectsBrokenData() {
        for (bad in listOf("", "{", "[1,]", "{\"a\" 1}", "\"open", "tru", "[1] x", "{\"a\":01x}")) {
            try {
                Json.parse(bad)
                fail("accepted: $bad")
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    @Test
    fun rejectsAbsurdNesting() {
        try {
            Json.parse("[".repeat(500) + "]".repeat(500))
            fail("accepted")
        } catch (_: IllegalArgumentException) {
        }
    }
}

class BackupTest {
    private val song = BackupTrack("/storage/emulated/0/Music/A/one.mp3", "One", "Artist")
    private val data = BackupData(
        createdAtMs = 1_700_000_000_000L,
        settings = AppSettings(AppThemeMode.AMOLED, AppAccent.EMBER, dynamicColor = false, lowPowerVisuals = true, uiTheme = com.kingplay.core.model.settings.AppUiTheme.KK_UI),
        playlists = listOf(BackupPlaylist("Gym \"mix\"", 5L, listOf(song, song.copy(title = "Two")))),
        favorites = listOf(song),
        presets = listOf(BackupPreset("Mine", 1.5f, List(EqPreset.BAND_COUNT) { it * 0.5f })),
        excludedFolders = listOf("/storage/emulated/0/Recordings"),
        lyrics = mapOf(lyricsKey("Artist", "One") to "[00:01.00]hello\nworld"),
    )

    @Test
    fun aBackupReadsBackExactly() {
        assertEquals(data, BackupCodec.decode(BackupCodec.encode(data)))
    }

    @Test
    fun anEmptyBackupWorks() {
        val empty = BackupData(0, AppSettings(), emptyList(), emptyList(), emptyList(), emptyList(), emptyMap())
        assertEquals(empty, BackupCodec.decode(BackupCodec.encode(empty)))
    }

    @Test
    fun otherFilesAreRefused() {
        val others = listOf("not json", "[1]", "{\"format\":\"other\",\"version\":1}", "{\"format\":\"kingplay-backup\"}")
        for (text in others) {
            try {
                BackupCodec.decode(text)
                fail("accepted: $text")
            } catch (_: BackupFormatException) {
            }
        }
    }

    @Test
    fun aNewerVersionIsRefusedRatherThanGuessed() {
        val newer = BackupCodec.encode(data).replace("\"version\": 1", "\"version\": 99")
        try {
            BackupCodec.decode(newer)
            fail("accepted")
        } catch (e: BackupFormatException) {
            assertTrue(e.message!!.contains("newer"))
        }
    }

    @Test
    fun missingSettingsKeepDefaultsAndBadPresetsAreDropped() {
        val text = """{"format":"kingplay-backup","version":1,"createdAtMs":1,
            "settings":{"themeMode":"SOMETHING_NEW","accent":"OCEAN"},
            "presets":[{"name":"short","preampDb":0,"bandGainsDb":[1,2,3]}]}"""
        val read = BackupCodec.decode(text)
        assertEquals(AppThemeMode.SYSTEM, read.settings.themeMode)
        assertEquals(AppAccent.OCEAN, read.settings.accent)
        assertTrue(read.presets.isEmpty())
    }

    @Test
    fun wrongTypesAreReportedNotCrashed() {
        val text = """{"format":"kingplay-backup","version":1,"createdAtMs":"x"}"""
        try {
            BackupCodec.decode(text)
            fail("accepted")
        } catch (_: BackupFormatException) {
        }
    }

    @Test
    fun lyricsKeysIgnoreCaseAndSpacing() {
        assertEquals(lyricsKey("The  Artist", " One "), lyricsKey("the artist", "ONE"))
        assertTrue(lyricsKey("a", "b") != lyricsKey("b", "a"))
    }

    @Test fun sourceIsKeptWhenPresentAndAbsentOtherwise() {
        assertNull(BackupCodec.decode(BackupCodec.encode(data)).source)
        val desktop = data.copy(source = BackupCodec.SOURCE_DESKTOP)
        val text = BackupCodec.encode(desktop)
        assertEquals(BackupCodec.SOURCE_DESKTOP, BackupCodec.decode(text).source)
        assertEquals(desktop, BackupCodec.decode(text))
    }

    @Test
    fun profileAndHapticSettingsSurviveBackup() {
        val settings = AppSettings(
            nickname = "James",
            haptics = com.kingplay.core.model.settings.HapticSettings(
                play = com.kingplay.core.model.settings.HapticLevel.LIGHT,
                tap = com.kingplay.core.model.settings.HapticLevel.OFF,
            ),
        )
        val data = BackupData(0, settings, emptyList(), emptyList(), emptyList(), emptyList(), emptyMap())
        val back = BackupCodec.decode(BackupCodec.encode(data)).settings
        assertEquals("James", back.nickname)
        assertEquals(com.kingplay.core.model.settings.HapticLevel.LIGHT, back.haptics.play)
        assertEquals(com.kingplay.core.model.settings.HapticLevel.OFF, back.haptics.tap)
    }

    @Test
    fun hapticsSwitchOffSilencesEveryKind() {
        val off = com.kingplay.core.model.settings.HapticSettings(enabled = false)
        com.kingplay.core.model.settings.HapticKind.entries.forEach {
            assertEquals(com.kingplay.core.model.settings.HapticLevel.OFF, off.levelFor(it))
        }
    }
}
