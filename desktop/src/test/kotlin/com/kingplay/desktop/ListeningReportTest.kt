package com.kingplay.desktop

import com.kingplay.core.model.Track
import com.kingplay.desktop.library.LibraryIndex
import com.kingplay.desktop.userdata.PlayEvent
import com.kingplay.desktop.userdata.UserData
import com.kingplay.desktop.userdata.UserDataStore
import com.kingplay.desktop.userdata.buildReport
import com.kingplay.desktop.userdata.monthsWithPlays
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun song(id: Long, title: String, artist: String, genre: String?, durationMs: Long = 120_000) = Track(
    id = id, uri = "C:\\Music\\$title.mp3", title = title, artistId = 1, artistName = artist, albumId = 1,
    albumTitle = "B", albumArtist = null, genre = genre, durationMs = durationMs, bitrateKbps = 0, sampleRateHz = 0,
    format = "mp3", trackNumber = 0, discNumber = 0, year = null, folderPath = "C:\\Music", dateAddedMs = 0,
    dateModifiedMs = 0,
)

class ListeningReportTest {
    private val utc: ZoneId = ZoneOffset.UTC
    private fun at(day: Int, hour: Int, month: Int = 10) =
        java.time.LocalDateTime.of(2026, month, day, hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private val index = LibraryIndex(
        listOf(song(1, "Alpha", "Ann", "Pop"), song(2, "Beta", "Bob", "Rock"), song(3, "Gamma", "Ann", "Pop")),
    )

    @Test fun reportCountsOnlyThatMonthAndRanksByPlays() {
        val history = listOf(
            PlayEvent(1, at(1, 20)), PlayEvent(1, at(2, 20)), PlayEvent(1, at(3, 8)),
            PlayEvent(2, at(4, 20)), PlayEvent(3, at(5, 20)), PlayEvent(2, at(1, 1, month = 9)),
        )
        val r = buildReport(history, index, YearMonth.of(2026, 10), utc)
        assertEquals(5, r.plays)
        assertEquals(10, r.minutes) // five plays of two minutes
        assertEquals(3, r.distinctSongs)
        assertEquals(listOf("Alpha", "Beta", "Gamma"), r.topTracks.map { it.title })
        assertEquals(3, r.topTracks.first().plays)
        assertEquals("Ann", r.topArtists.first().name)
        assertEquals("Pop", r.topGenres.first().name)
        assertEquals(20, r.busiestHour)
    }

    @Test fun emptyMonthAndMissingSongs() {
        val r = buildReport(listOf(PlayEvent(99, at(1, 1))), index, YearMonth.of(2026, 10), utc)
        assertTrue(r.isEmpty)
        assertNull(r.busiestHour)
        assertEquals(emptyList<String>(), monthsWithPlays(emptyList(), utc))
    }

    @Test fun monthsAreNewestFirst() {
        val history = listOf(PlayEvent(1, at(1, 1, month = 8)), PlayEvent(1, at(1, 1, month = 10)), PlayEvent(1, at(2, 1, month = 10)))
        assertEquals(listOf("2026-10", "2026-08"), monthsWithPlays(history, utc))
    }

    @Test fun recordPlayAppendsHistoryAndItSurvivesStorage() {
        val data = UserData().recordPlay(1, 1000).recordPlay(2, 2000).withReportShown("2026-09")
        assertEquals(listOf(PlayEvent(1, 1000), PlayEvent(2, 2000)), data.history)
        val back = UserDataStore.decode(UserDataStore.encode(data))
        assertEquals(data.history, back.history)
        assertEquals("2026-09", back.reportShownFor)
    }

    @Test fun historyIsCapped() {
        var data = UserData()
        repeat(UserData.MAX_HISTORY + 5) { data = data.recordPlay(1, it.toLong()) }
        assertEquals(UserData.MAX_HISTORY, data.history.size)
        assertEquals(5L, data.history.first().atMs)
    }
}
