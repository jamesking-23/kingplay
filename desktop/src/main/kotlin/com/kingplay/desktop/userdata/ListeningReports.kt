package com.kingplay.desktop.userdata

import com.kingplay.core.model.ListeningReport
import com.kingplay.core.model.ReportEntry
import com.kingplay.core.model.ReportTrack
import com.kingplay.desktop.library.LibraryIndex
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

private const val MS_PER_MINUTE = 60_000L
private const val TOP = 5

private fun monthOf(atMs: Long, zone: ZoneId): YearMonth = YearMonth.from(Instant.ofEpochMilli(atMs).atZone(zone))

/** Months ("yyyy-MM") that have at least one play, newest first. */
fun monthsWithPlays(history: List<PlayEvent>, zone: ZoneId = ZoneId.systemDefault()): List<String> =
    history.map { monthOf(it.atMs, zone).toString() }.distinct().sortedDescending()

/** One month of listening. Plays of songs that left the library are ignored. */
fun buildReport(
    history: List<PlayEvent>,
    index: LibraryIndex,
    month: YearMonth,
    zone: ZoneId = ZoneId.systemDefault(),
): ListeningReport {
    val plays = history.filter { monthOf(it.atMs, zone) == month }.mapNotNull { e -> index[e.trackId]?.let { e to it } }
    val byTrack = plays.groupBy({ it.second.id }, { it }).values
        .sortedWith(compareByDescending<List<Pair<PlayEvent, com.kingplay.core.model.Track>>> { it.size }
            .thenBy { it.first().second.title.lowercase() })
    fun names(selector: (com.kingplay.core.model.Track) -> String?): List<ReportEntry> = plays
        .mapNotNull { selector(it.second)?.takeIf { n -> n.isNotBlank() } }
        .groupingBy { it }.eachCount().entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
        .take(TOP).map { ReportEntry(it.key, it.value) }
    val busiest = plays.groupingBy { Instant.ofEpochMilli(it.first.atMs).atZone(zone).hour }.eachCount()
        .entries.sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key }).firstOrNull()?.key
    return ListeningReport(
        month = month.toString(),
        plays = plays.size,
        minutes = plays.sumOf { it.second.durationMs } / MS_PER_MINUTE,
        distinctSongs = byTrack.size,
        topTracks = byTrack.take(TOP).map { group ->
            val t = group.first().second
            ReportTrack(t.id, t.title, t.artistName, t.albumId, t.uri, group.size)
        },
        topArtists = names { it.artistName },
        topGenres = names { it.genre },
        busiestHour = busiest,
    )
}
