package com.kingplay.desktop

import com.kingplay.core.model.Track
import com.kingplay.desktop.library.SongOrder
import com.kingplay.desktop.library.sortedBy
import org.junit.Assert.assertEquals
import org.junit.Test

class SongOrderTest {
    private fun t(id: Long, title: String, added: Long) = Track(
        id = id, uri = "C:/m/$id.mp3", title = title, artistId = 1, artistName = "A", albumId = 1, albumTitle = "B",
        albumArtist = null, genre = null, durationMs = 1000, bitrateKbps = 0, sampleRateHz = 0, format = "mp3",
        trackNumber = 0, discNumber = 0, year = null, folderPath = "C:/m", dateAddedMs = added, dateModifiedMs = 0,
    )

    @Test fun newestComesFirstThenByTitle() {
        val songs = listOf(t(1, "b", 100), t(2, "a", 300), t(3, "c", 300), t(4, "d", 200))
        assertEquals(listOf(2L, 3L, 4L, 1L), songs.sortedBy(SongOrder.NEWEST).map { it.id })
    }

    @Test fun titleOrderIsAlphabetical() {
        val songs = listOf(t(1, "b", 1), t(2, "A", 2))
        assertEquals(listOf(2L, 1L), songs.sortedBy(SongOrder.TITLE).map { it.id })
    }
}
