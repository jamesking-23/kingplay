package com.kingplay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kingplay.core.database.entity.PlayEventEntity
import kotlinx.coroutines.flow.Flow

data class MonthTotalsRow(val plays: Int, val totalMs: Long, val distinctTracks: Int)

data class TopTrackRow(
    val trackId: Long,
    val title: String,
    val artistName: String,
    val albumId: Long,
    val uri: String,
    val plays: Int,
)

data class TopNameRow(val name: String, val plays: Int)

@Dao
interface PlayEventDao {
    @Insert
    suspend fun insert(event: PlayEventEntity)

    /** Months (as "yyyy-MM", device time zone) that have at least one play, newest first. */
    @Query(
        "SELECT DISTINCT strftime('%Y-%m', playedAtMs / 1000, 'unixepoch', 'localtime') AS month " +
            "FROM play_events ORDER BY month DESC",
    )
    fun observeMonths(): Flow<List<String>>

    @Query(
        "SELECT COUNT(*) AS plays, COALESCE(SUM(t.durationMs), 0) AS totalMs, " +
            "COUNT(DISTINCT e.trackId) AS distinctTracks " +
            "FROM play_events e JOIN tracks t ON t.id = e.trackId " +
            "WHERE e.playedAtMs >= :fromMs AND e.playedAtMs < :toMs",
    )
    suspend fun totals(fromMs: Long, toMs: Long): MonthTotalsRow

    @Query(
        "SELECT t.id AS trackId, t.title AS title, t.artistName AS artistName, t.albumId AS albumId, " +
            "t.uri AS uri, COUNT(*) AS plays " +
            "FROM play_events e JOIN tracks t ON t.id = e.trackId " +
            "WHERE e.playedAtMs >= :fromMs AND e.playedAtMs < :toMs " +
            "GROUP BY t.id ORDER BY plays DESC, t.title ASC LIMIT :limit",
    )
    suspend fun topTracks(fromMs: Long, toMs: Long, limit: Int): List<TopTrackRow>

    @Query(
        "SELECT t.artistName AS name, COUNT(*) AS plays " +
            "FROM play_events e JOIN tracks t ON t.id = e.trackId " +
            "WHERE e.playedAtMs >= :fromMs AND e.playedAtMs < :toMs " +
            "GROUP BY t.artistName ORDER BY plays DESC, name ASC LIMIT :limit",
    )
    suspend fun topArtists(fromMs: Long, toMs: Long, limit: Int): List<TopNameRow>

    @Query(
        "SELECT t.genre AS name, COUNT(*) AS plays " +
            "FROM play_events e JOIN tracks t ON t.id = e.trackId " +
            "WHERE e.playedAtMs >= :fromMs AND e.playedAtMs < :toMs AND t.genre IS NOT NULL AND t.genre != '' " +
            "GROUP BY t.genre ORDER BY plays DESC, name ASC LIMIT :limit",
    )
    suspend fun topGenres(fromMs: Long, toMs: Long, limit: Int): List<TopNameRow>

    /** Plays per hour of day (0-23, device time zone), for "you listen most at ..." */
    @Query(
        "SELECT CAST(strftime('%H', playedAtMs / 1000, 'unixepoch', 'localtime') AS INTEGER) AS hour, " +
            "COUNT(*) AS plays FROM play_events " +
            "WHERE playedAtMs >= :fromMs AND playedAtMs < :toMs GROUP BY hour ORDER BY plays DESC LIMIT 1",
    )
    suspend fun busiestHour(fromMs: Long, toMs: Long): HourRow?
}

data class HourRow(val hour: Int, val plays: Int)
