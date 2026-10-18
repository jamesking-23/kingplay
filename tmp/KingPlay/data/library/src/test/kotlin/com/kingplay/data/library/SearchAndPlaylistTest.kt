package com.kingplay.data.library

import com.kingplay.data.library.playlist.M3uEntry
import com.kingplay.data.library.playlist.M3uParser
import com.kingplay.data.library.playlist.M3uWriter
import com.kingplay.data.library.playlist.PathRow
import com.kingplay.data.library.playlist.TrackPathIndex
import com.kingplay.data.library.repo.moveItem
import com.kingplay.data.library.repo.normalizedResumePosition
import com.kingplay.data.library.repo.removeItem
import com.kingplay.data.library.scan.albumId
import com.kingplay.data.library.search.FtsQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtsQueryTest {
    @Test
    fun wordsBecomeQuotedPrefixTerms() {
        assertEquals("\"burna*\" \"boy*\"", FtsQuery.build("Burna Boy"))
    }

    @Test
    fun operatorsAndPunctuationAreNeutralised() {
        assertEquals("\"not*\" \"near*\" \"or*\"", FtsQuery.build("NOT NEAR \"OR\""))
        assertEquals("\"don*\" \"t*\" \"stop*\"", FtsQuery.build("don't stop!!"))
        assertEquals("\"a*\" \"b*\"", FtsQuery.build("a:b*"))
    }

    @Test
    fun nothingSearchableReturnsNull() {
        assertNull(FtsQuery.build(""))
        assertNull(FtsQuery.build("  -- ** "))
    }

    @Test
    fun unicodeLettersSurvive() {
        assertEquals("\"ñandú*\"", FtsQuery.build("Ñandú"))
        assertEquals("\"日本語*\"", FtsQuery.build("日本語"))
    }

    @Test
    fun termCountIsCapped() {
        val query = FtsQuery.build((1..20).joinToString(" ") { "w$it" })!!
        assertEquals(8, query.split(' ').size)
    }
}

class M3uTest {
    @Test
    fun parserDropsCommentsAndNormalisesPaths() {
        val text = "\uFEFF#EXTM3U\r\n#EXTINF:1,a - b\r\n/music/a.mp3\r\n\r\n" +
            "C:\\Music\\b.flac\r\nfile:///sdcard/My%20Song.mp3\r\n"
        assertEquals(listOf("/music/a.mp3", "C:/Music/b.flac", "/sdcard/My Song.mp3"), M3uParser.parse(text))
    }

    @Test
    fun writerProducesExtendedM3u() {
        val out = M3uWriter.write(listOf(M3uEntry("/m/a.mp3", 215, "Artist - Title")))
        assertEquals("#EXTM3U\n#EXTINF:215,Artist - Title\n/m/a.mp3\n", out)
    }

    @Test
    fun writtenPlaylistParsesBackToSamePaths() {
        val entries = listOf(M3uEntry("/m/a.mp3", 1, "x"), M3uEntry("/m/b c.mp3", 2, "y"))
        assertEquals(entries.map { it.path }, M3uParser.parse(M3uWriter.write(entries)))
    }

    private val index = TrackPathIndex(
        listOf(
            PathRow(1, "/storage/emulated/0/Music/Fela", "Zombie.mp3"),
            PathRow(2, "/storage/emulated/0/Music/Fela", "Water No Get Enemy.mp3"),
            PathRow(3, "/storage/emulated/0/Music/Best Of", "Zombie.mp3"),
            PathRow(4, "/storage/1234-5678/Music", "Unique.flac"),
        ),
    )

    @Test
    fun exactAndCaseInsensitivePathsMatch() {
        assertEquals(1L, index.match("/storage/emulated/0/Music/Fela/Zombie.mp3"))
        assertEquals(1L, index.match("/STORAGE/EMULATED/0/MUSIC/FELA/ZOMBIE.MP3"))
    }

    @Test
    fun otherDevicePathsMatchByFolderAndName() {
        assertEquals(1L, index.match("D:\\Music\\Fela\\Zombie.mp3"))
        assertEquals(3L, index.match("../Best Of/Zombie.mp3"))
    }

    @Test
    fun ambiguousNameOnlyMatchIsRejected() {
        assertNull(index.match("/somewhere/else/Zombie.mp3"))
    }

    @Test
    fun uniqueNameOnlyMatchIsAccepted() {
        assertEquals(4L, index.match("/old/phone/unique.FLAC"))
        assertNull(index.match("/old/phone/missing.mp3"))
        assertNull(index.match(""))
    }
}

class ListOpsTest {
    @Test
    fun moveItemReordersAndIgnoresBadIndices() {
        val list = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "c", "a", "d"), moveItem(list, 0, 2))
        assertEquals(listOf("a", "d", "b", "c"), moveItem(list, 3, 1))
        assertEquals(list, moveItem(list, 1, 1))
        assertEquals(list, moveItem(list, 9, 0))
        assertEquals(list, moveItem(list, 0, -1))
    }

    @Test
    fun removeItemClosesTheGap() {
        val list = listOf("a", "b", "c")
        assertEquals(listOf("a", "c"), removeItem(list, 1))
        assertEquals(list, removeItem(list, 5))
    }

    @Test
    fun resumePositionIsClearedNearTheEnd() {
        val f = ::normalizedResumePosition
        assertEquals(60_000L, f(60_000L, 600_000L))
        assertEquals(0L, f(596_000L, 600_000L))
        assertEquals(0L, f(-5L, 600_000L))
        assertEquals(30_000L, f(30_000L, 0L))
    }
}

class AlbumIdentityTest {
    @Test
    fun compilationFolderStaysOneAlbumButSameTitleElsewhereDoesNot() {
        val a = albumId("Hits", null, "/m/Compilation")
        val b = albumId("hits", null, "/m/Compilation")
        val c = albumId("Hits", null, "/m/Other")
        val d = albumId("Hits", "Fela", "/m/Compilation")
        val e = albumId("Hits", "FELA", "/m/Elsewhere")
        assertEquals(a, b)
        assertNotEquals(a, c)
        assertNotEquals(a, d)
        assertEquals(d, e)
    }
}
