package com.kingplay.data.library

import com.kingplay.core.model.UnknownNames
import com.kingplay.data.library.scan.StableIds
import com.kingplay.data.library.scan.audioFormat
import com.kingplay.data.library.scan.decodeLegacyTrackNumber
import com.kingplay.data.library.scan.estimateBitrateKbps
import com.kingplay.data.library.scan.isExcluded
import com.kingplay.data.library.scan.parseLeadingInt
import com.kingplay.data.library.scan.resolveAlbum
import com.kingplay.data.library.scan.resolveArtist
import com.kingplay.data.library.scan.resolveFolder
import com.kingplay.data.library.scan.titleOrFileName
import com.kingplay.data.library.scan.titleSortKey
import com.kingplay.data.library.scan.videoContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalizeTest {
    @Test
    fun stableIds_ignoreCaseAndWhitespace() {
        assertEquals(StableIds.artist("Burna Boy"), StableIds.artist("  burna   BOY "))
        assertEquals(StableIds.album("Love, Damini", "Burna Boy"), StableIds.album("love, damini", "BURNA BOY"))
    }

    @Test
    fun stableIds_differByNameAndKind() {
        assertNotEquals(StableIds.artist("Asake"), StableIds.artist("Rema"))
        assertNotEquals(StableIds.artist("Asake"), StableIds.album("Asake", ""))
        assertNotEquals(StableIds.album("Hits", "A"), StableIds.album("Hits", "B"))
    }

    @Test
    fun stableIds_arePositiveAndDeterministic() {
        val samples = listOf("", "a", "Ñandú", "日本語", "x".repeat(500))
        for (s in samples) {
            val id = StableIds.artist(s)
            assertTrue("id for '$s' must be positive", id > 0)
            assertEquals(id, StableIds.artist(s))
        }
    }

    @Test
    fun stableIds_knownValueIsStableAcrossReleases() {
        // Guards against accidental changes to the hash, which would orphan every album id.
        assertEquals(StableIds.artist("test"), StableIds.artist("TEST"))
        assertEquals(EXPECTED_TEST_ARTIST_ID, StableIds.artist("test"))
    }

    @Test
    fun titleSortKey_dropsLeadingArticleButNotWholeTitle() {
        assertEquals("wall", titleSortKey("The Wall"))
        assertEquals("a", titleSortKey("A"))
        assertEquals("the", titleSortKey("The"))
        assertEquals("another one", titleSortKey("An Another One"))
        assertEquals("anthem", titleSortKey("Anthem"))
    }

    @Test
    fun unknownTagsBecomePlaceholders() {
        assertEquals(UnknownNames.ARTIST, resolveArtist(null))
        assertEquals(UnknownNames.ARTIST, resolveArtist("<unknown>"))
        assertEquals(UnknownNames.ARTIST, resolveArtist("   "))
        assertEquals(UnknownNames.ALBUM, resolveAlbum("<UNKNOWN>"))
        assertEquals("Fela", resolveArtist("  Fela "))
    }

    @Test
    fun titleFallsBackToFileNameWithoutExtension() {
        assertEquals("track one", titleOrFileName(null, "track one.flac"))
        assertEquals("noext", titleOrFileName("<unknown>", "noext"))
        assertEquals("Real", titleOrFileName("Real", "x.mp3"))
    }

    @Test
    fun formatComesFromExtensionThenMime() {
        assertEquals("FLAC", audioFormat("a.flac", "audio/flac"))
        assertEquals("MP3", audioFormat("a.b.mp3", null))
        assertEquals("OGG", audioFormat("noext", "audio/ogg"))
        assertEquals("UNKNOWN", audioFormat("noext", null))
        assertEquals("mkv", videoContainer("movie.MKV", "video/x-matroska"))
    }

    @Test
    fun legacyTrackNumbersSplitDiscAndTrack() {
        assertEquals(1 to 7, decodeLegacyTrackNumber(7))
        assertEquals(2 to 3, decodeLegacyTrackNumber(2003))
        assertEquals(1 to 0, decodeLegacyTrackNumber(-4))
    }

    @Test
    fun leadingIntParsesTrackOfTotal() {
        assertEquals(3, parseLeadingInt("3/12"))
        assertEquals(12, parseLeadingInt(" 12 "))
        assertNull(parseLeadingInt("x"))
        assertNull(parseLeadingInt(null))
    }

    @Test
    fun bitrateEstimateIsKilobitsPerSecond() {
        // 4,000,000 bytes over 200 s = 160 kbit/s
        assertEquals(160, estimateBitrateKbps(4_000_000, 200_000))
        assertEquals(0, estimateBitrateKbps(0, 1000))
        assertEquals(0, estimateBitrateKbps(1000, 0))
    }

    @Test
    fun excludedFoldersMatchOnPathBoundaries() {
        val excluded = listOf("/storage/emulated/0/WhatsApp/")
        assertTrue(isExcluded("/storage/emulated/0/WhatsApp", excluded))
        assertTrue(isExcluded("/storage/emulated/0/WhatsApp/Media/Audio", excluded))
        assertFalse(isExcluded("/storage/emulated/0/WhatsApp2", excluded))
        assertFalse(isExcluded("/storage/emulated/0/Music", excluded))
        assertFalse(isExcluded("/anything", emptyList()))
        assertFalse(isExcluded("/anything", listOf("")))
    }

    @Test
    fun folderComesFromAbsoluteThenRelativePath() {
        assertEquals("/storage/emulated/0/Music/Album", resolveFolder("/storage/emulated/0/Music/Album/a.mp3", null))
        assertEquals("/Music/Album", resolveFolder(null, "Music/Album/"))
        assertEquals("/", resolveFolder("/a.mp3", null))
        assertEquals("", resolveFolder(null, null))
    }

    private companion object {
        // FNV-1a 64 over "artist", 0x1f, "test", 0x1f, masked positive. Cross-checked with an independent script.
        const val EXPECTED_TEST_ARTIST_ID = 5946011130159695816L
    }
}
