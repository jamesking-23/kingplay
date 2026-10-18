package com.kingplay.desktop

import com.kingplay.core.model.UnknownNames
import com.kingplay.desktop.library.LibraryIndex
import com.kingplay.desktop.library.LibraryStore
import com.kingplay.desktop.library.RawTags
import com.kingplay.desktop.library.SavedLibrary
import com.kingplay.desktop.library.TagReader
import com.kingplay.desktop.library.TrackScanner
import com.kingplay.desktop.library.isAudioFile
import com.kingplay.desktop.library.parseNumberPart
import com.kingplay.desktop.library.parseYear
import com.kingplay.desktop.library.stableId
import com.kingplay.desktop.ui.folderLabel
import com.kingplay.desktop.ui.formatTime
import com.kingplay.desktop.ui.nextRepeatMode
import com.kingplay.core.model.RepeatMode
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryTest {
    private fun tempDir(): File = Files.createTempDirectory("kp-test").toFile().also { it.deleteOnExit() }

    private val reader = TagReader { file ->
        when (file.nameWithoutExtension) {
            "broken" -> null
            "a1" -> RawTags("Alpha", "Ann", "First", "Ann", "Pop", 1, 1, 2020, 180_000, 320, 44_100)
            "a2" -> RawTags("Beta", "Ann", "First", "Ann", null, 2, 1, 2020, 200_000)
            else -> RawTags()
        }
    }

    @Test fun audioExtensionsAreRecognisedCaseInsensitively() {
        assertTrue(isAudioFile("Song.MP3"))
        assertTrue(isAudioFile("x.flac"))
        assertTrue(!isAudioFile("cover.jpg"))
        assertTrue(!isAudioFile("noextension"))
    }

    @Test fun stableIdIsRepeatableAndPositive() {
        assertEquals(stableId("C:/a.mp3"), stableId("C:/a.mp3"))
        assertNotEquals(stableId("C:/a.mp3"), stableId("C:/b.mp3"))
        assertTrue(stableId("anything") >= 0)
    }

    @Test fun tagNumberParsing() {
        assertEquals(3, parseNumberPart("3/12"))
        assertEquals(7, parseNumberPart(" 07 "))
        assertEquals(0, parseNumberPart("x"))
        assertEquals(0, parseNumberPart(null))
        assertEquals(2019, parseYear("2019-05-03"))
        assertNull(parseYear("0"))
        assertNull(parseYear("abcd"))
    }

    @Test fun scanFindsAudioInSubfoldersAndSkipsOtherFiles() {
        val root = tempDir()
        File(root, "sub").mkdirs()
        File(root, "a1.mp3").writeText("x")
        File(root, "sub/a2.flac").writeText("x")
        File(root, "cover.jpg").writeText("x")
        File(root, "broken.mp3").writeText("x")
        val tracks = TrackScanner(reader, now = { 5L }).scan(listOf(root.path))
        assertEquals(setOf("Alpha", "Beta", "broken"), tracks.map { it.title }.toSet())
        val alpha = tracks.first { it.title == "Alpha" }
        assertEquals("Ann", alpha.artistName)
        assertEquals(2020, alpha.year)
        assertEquals(5L, alpha.dateAddedMs)
        assertEquals("mp3", alpha.format)
        val unknown = tracks.first { it.title == "broken" }
        assertEquals(UnknownNames.ARTIST, unknown.artistName)
        assertEquals(UnknownNames.ALBUM, unknown.albumTitle)
    }

    @Test fun songsOfOneAlbumShareAlbumAndArtistIds() {
        val root = tempDir()
        File(root, "a1.mp3").writeText("x")
        File(root, "a2.mp3").writeText("x")
        val tracks = TrackScanner(reader).scan(listOf(root.path))
        assertEquals(1, tracks.map { it.albumId }.distinct().size)
        assertEquals(1, tracks.map { it.artistId }.distinct().size)
    }

    @Test fun rescanKeepsUnchangedFilesAndTheirPlayCounts() {
        val root = tempDir()
        File(root, "a1.mp3").writeText("x")
        var reads = 0
        val counting = TagReader { reads++; RawTags("Alpha") }
        val scanner = TrackScanner(counting)
        val first = scanner.scan(listOf(root.path))
        val edited = first.map { it.copy(playCount = 9, isFavorite = true) }
        val second = scanner.scan(listOf(root.path), edited.associateBy { it.uri })
        assertEquals(1, reads)
        assertEquals(9, second.single().playCount)
        assertTrue(second.single().isFavorite)
    }

    @Test fun changedFileIsReadAgainButKeepsPlayCount() {
        val root = tempDir()
        val file = File(root, "a1.mp3").apply { writeText("x") }
        val scanner = TrackScanner(reader)
        val first = scanner.scan(listOf(root.path)).map { it.copy(playCount = 4) }
        file.setLastModified(file.lastModified() + 60_000)
        val second = scanner.scan(listOf(root.path), first.associateBy { it.uri })
        assertEquals(4, second.single().playCount)
        assertNotEquals(first.single().dateModifiedMs, second.single().dateModifiedMs)
    }

    @Test fun missingOrDuplicateRootsAreHarmless() {
        val root = tempDir()
        File(root, "a1.mp3").writeText("x")
        val tracks = TrackScanner(reader).scan(listOf(root.path, root.path, File(root, "nope").path))
        assertEquals(1, tracks.size)
    }

    @Test fun storeRoundTripsAndSurvivesDamage() {
        val dir = tempDir()
        val root = File(dir, "music").apply { mkdirs() }
        File(root, "a1.mp3").writeText("x")
        val tracks = TrackScanner(reader).scan(listOf(root.path)).map { it.copy(isFavorite = true, lastPlayedMs = 7) }
        val store = LibraryStore(File(dir, "data/library.json"))
        store.save(SavedLibrary(listOf(root.path), tracks))
        val back = store.load()
        assertEquals(listOf(root.path), back.folders)
        assertEquals(tracks, back.tracks)
        File(dir, "data/library.json").writeText("{ not json")
        assertTrue(store.load().tracks.isEmpty())
        assertTrue(LibraryStore(File(dir, "missing.json")).load().tracks.isEmpty())
    }

    @Test fun indexGroupsAlbumsAndArtistsAndSearches() {
        val root = tempDir()
        File(root, "a1.mp3").writeText("x")
        File(root, "a2.mp3").writeText("x")
        File(root, "other.mp3").writeText("x")
        val index = LibraryIndex(TrackScanner(reader).scan(listOf(root.path)))
        assertEquals(2, index.albums.size)
        val first = index.albums.first { it.title == "First" }
        assertEquals(2, first.trackCount)
        assertEquals(380_000, first.totalDurationMs)
        assertEquals("Ann", first.artistName)
        assertEquals(listOf("Alpha", "Beta"), index.albumTracks(first.id).map { it.title })
        assertEquals(2, index.artists.size)
        assertEquals(1, index.search("ann beta").size)
        assertEquals(3, index.search("  ").size)
        assertEquals(0, index.search("zzz").size)
        assertSame(index.tracks.first(), index[index.tracks.first().id])
        assertNull(index[-1])
    }

    @Test fun formatting() {
        assertEquals("1:05", formatTime(65_000))
        assertEquals("1:02:05", formatTime(3_725_000))
        assertEquals("0:00", formatTime(-4))
        assertEquals(RepeatMode.ALL, nextRepeatMode(RepeatMode.OFF))
        assertEquals(RepeatMode.OFF, nextRepeatMode(RepeatMode.ONE))
        assertEquals("Music", folderLabel("C:\\Users\\me\\Music\\"))
        assertEquals("C:", folderLabel("C:\\"))
    }
}
