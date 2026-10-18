package com.kingplay.feature.nowplaying

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.RepeatMode
import com.kingplay.core.model.Track
import com.kingplay.core.model.VideoItem
import com.kingplay.feature.nowplaying.logic.buildQueueItems
import com.kingplay.feature.nowplaying.logic.nextRepeatMode
import com.kingplay.feature.nowplaying.logic.seekTargetMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingLogicTest {
    private fun track(id: Long, albumId: Long = 7) = Track(
        id = id, uri = "content://media/$id", title = "T$id", artistId = 1, artistName = "Artist",
        albumId = albumId, albumTitle = "Album", albumArtist = null, genre = null, durationMs = 1000,
        bitrateKbps = 0, sampleRateHz = 0, format = "MP3", trackNumber = 1, discNumber = 1, year = null,
        folderPath = "/m", dateAddedMs = 0, dateModifiedMs = 0,
    )

    private fun video(id: Long) = VideoItem(
        id = id, uri = "content://video/$id", title = "V$id", durationMs = 1000, width = 1, height = 1,
        videoCodec = null, container = "mp4", folderPath = "/v", sizeBytes = 1, dateAddedMs = 0,
    )

    @Test
    fun queueItemsKeepOrderAndMarkMissingFiles() {
        val items = buildQueueItems(
            queue = listOf(MediaRef.Audio(2), MediaRef.Audio(99), MediaRef.Video(5), MediaRef.Video(6)),
            tracks = mapOf(2L to track(2)),
            videos = mapOf(5L to video(5)),
        )
        assertEquals(4, items.size)
        assertEquals("T2", items[0].title)
        assertEquals("album:7", items[0].artworkKey)
        assertNull(items[1].title)
        assertEquals("V5", items[2].title)
        assertTrue(items[2].isVideo)
        assertNull(items[3].title)
    }

    @Test
    fun repeatCycles() {
        assertEquals(RepeatMode.ALL, nextRepeatMode(RepeatMode.OFF))
        assertEquals(RepeatMode.ONE, nextRepeatMode(RepeatMode.ALL))
        assertEquals(RepeatMode.OFF, nextRepeatMode(RepeatMode.ONE))
    }

    @Test
    fun seekTarget() {
        assertEquals(0, seekTargetMs(0.5f, 0))
        assertEquals(50_000, seekTargetMs(0.5f, 100_000))
        assertEquals(100_000, seekTargetMs(2f, 100_000))
        assertEquals(0, seekTargetMs(-1f, 100_000))
    }
}
