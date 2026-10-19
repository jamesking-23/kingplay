package com.kingplay.core.model

/** "yyyy-MM", for example "2026-10". */
typealias MonthKey = String

data class ReportTrack(
    val trackId: Long,
    val title: String,
    val artistName: String,
    val albumId: Long,
    val uri: String,
    val plays: Int,
)

data class ReportEntry(val name: String, val plays: Int)

/** A month of listening, in the spirit of a year-in-review: totals, favourites and when you listen. */
data class ListeningReport(
    val month: MonthKey,
    val plays: Int,
    val minutes: Long,
    val distinctSongs: Int,
    val topTracks: List<ReportTrack>,
    val topArtists: List<ReportEntry>,
    val topGenres: List<ReportEntry>,
    /** Hour of day (0-23) with the most plays, or null with no plays. */
    val busiestHour: Int?,
) {
    val isEmpty: Boolean get() = plays == 0
}
