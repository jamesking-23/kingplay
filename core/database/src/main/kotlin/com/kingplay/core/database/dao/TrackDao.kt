package com.kingplay.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Upsert
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.kingplay.core.database.entity.TrackEntity
import com.kingplay.core.database.entity.TrackUserDataEntity
import com.kingplay.core.model.SongFilter
import com.kingplay.core.model.SongSort
import com.kingplay.core.model.SongSortField
import com.kingplay.core.model.UnknownNames
import java.util.Locale
import kotlinx.coroutines.flow.Flow

// Raw strings below are compile-time constants so they can be concatenated inside @Query.
internal const val TRACK_SELECT = """
    SELECT t.*, COALESCE(u.playCount, 0) AS playCount, u.lastPlayedMs AS lastPlayedMs,
           COALESCE(u.isFavorite, 0) AS isFavorite
    FROM tracks t LEFT JOIN track_user_data u ON u.trackId = t.id
"""

internal const val ALBUM_SELECT = """
    SELECT albumId AS id, MAX(albumTitle) AS title,
           CASE WHEN MAX(albumArtist) IS NOT NULL THEN MAX(albumArtist)
                WHEN COUNT(DISTINCT artistId) > 1 THEN '""" + UnknownNames.VARIOUS_ARTISTS + """'
                ELSE MAX(artistName) END AS artistName,
           MIN(uri) AS artworkUri, MAX(year) AS year,
           COUNT(*) AS trackCount, SUM(durationMs) AS totalDurationMs
    FROM tracks
"""

internal const val SONG_FILTER = """
    WHERE (:format IS NULL OR t.format = :format)
      AND t.bitrateKbps >= :minBitrate
      AND t.durationMs >= :minDuration AND t.durationMs <= :maxDuration
"""

internal const val ORDER_TITLE_ASC = " ORDER BY t.titleSort ASC, t.id ASC"
internal const val ORDER_TITLE_DESC = " ORDER BY t.titleSort DESC, t.id ASC"
private const val ALBUM_TAIL = "t.albumTitle COLLATE NOCASE ASC, t.discNumber ASC, t.trackNumber ASC, t.id ASC"
internal const val ORDER_ARTIST_ASC = " ORDER BY t.artistName COLLATE NOCASE ASC, " + ALBUM_TAIL
internal const val ORDER_ARTIST_DESC = " ORDER BY t.artistName COLLATE NOCASE DESC, " + ALBUM_TAIL
internal const val ORDER_ALBUM_ASC = " ORDER BY " + ALBUM_TAIL
internal const val ORDER_ALBUM_DESC =
    " ORDER BY t.albumTitle COLLATE NOCASE DESC, t.discNumber ASC, t.trackNumber ASC, t.id ASC"
internal const val ORDER_ADDED_ASC = " ORDER BY t.dateAddedMs ASC, t.id ASC"
internal const val ORDER_ADDED_DESC = " ORDER BY t.dateAddedMs DESC, t.id ASC"
internal const val ORDER_DURATION_ASC = " ORDER BY t.durationMs ASC, t.id ASC"
internal const val ORDER_DURATION_DESC = " ORDER BY t.durationMs DESC, t.id ASC"
internal const val ORDER_PLAYS_ASC = " ORDER BY playCount ASC, t.titleSort ASC, t.id ASC"
internal const val ORDER_PLAYS_DESC = " ORDER BY playCount DESC, t.titleSort ASC, t.id ASC"

private fun orderFor(sort: SongSort): String = when (sort.field) {
    SongSortField.TITLE -> if (sort.ascending) ORDER_TITLE_ASC else ORDER_TITLE_DESC
    SongSortField.ARTIST -> if (sort.ascending) ORDER_ARTIST_ASC else ORDER_ARTIST_DESC
    SongSortField.ALBUM -> if (sort.ascending) ORDER_ALBUM_ASC else ORDER_ALBUM_DESC
    SongSortField.DATE_ADDED -> if (sort.ascending) ORDER_ADDED_ASC else ORDER_ADDED_DESC
    SongSortField.DURATION -> if (sort.ascending) ORDER_DURATION_ASC else ORDER_DURATION_DESC
    SongSortField.PLAY_COUNT -> if (sort.ascending) ORDER_PLAYS_ASC else ORDER_PLAYS_DESC
}

/**
 * Ids of all songs matching [filter], ordered exactly like the paged queries. It uses the same ORDER BY text and the
 * same filter conditions, so a queue built from it matches what the list shows.
 */
fun songIdsQuery(sort: SongSort, filter: SongFilter): SupportSQLiteQuery {
    val format = filter.format?.trim()?.uppercase(Locale.ROOT)?.ifEmpty { null }
    val sql = "SELECT t.id, COALESCE(u.playCount, 0) AS playCount " +
        "FROM tracks t LEFT JOIN track_user_data u ON u.trackId = t.id " +
        "WHERE (? IS NULL OR t.format = ?) AND t.bitrateKbps >= ? " +
        "AND t.durationMs >= ? AND t.durationMs <= ?" + orderFor(sort)
    val args: Array<Any?> = arrayOf(format, format, filter.minBitrateKbps, filter.minDurationMs, filter.maxDurationMs)
    return SimpleSQLiteQuery(sql, args)
}

@Dao
@Suppress("TooManyFunctions")
interface TrackDao {

    // ---- Sync (used by the scanner) ----

    @Query("SELECT id, dateModifiedMs, sizeBytes, folderPath FROM tracks")
    suspend fun syncSnapshot(): List<SyncRow>

    @Upsert
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT id, uri, sizeBytes, durationMs, bitrateKbps FROM tracks WHERE needsDetails = 1 LIMIT :limit")
    suspend fun pendingDetails(limit: Int): List<PendingDetailsRow>

    /** Built by [songIdsQuery] so the order matches the paged queries below exactly. */
    @RawQuery
    suspend fun songIdsRaw(query: SupportSQLiteQuery): List<Long>

    @Query("SELECT COUNT(*) FROM tracks WHERE needsDetails = 1")
    suspend fun pendingDetailsCount(): Int

    @Query(
        "UPDATE tracks SET bitrateKbps = :bitrateKbps, sampleRateHz = :sampleRateHz, needsDetails = 0 WHERE id = :id",
    )
    suspend fun updateDetails(id: Long, bitrateKbps: Int, sampleRateHz: Int)

    @Query("SELECT id, folderPath, fileName FROM tracks")
    suspend fun allPaths(): List<TrackPathRow>

    // ---- Songs list (paged). One query per sort so SQLite can use its indexes. ----

    @Query(TRACK_SELECT + SONG_FILTER + ORDER_TITLE_ASC)
    fun songsByTitleAsc(
        format: String?,
        minBitrate: Int,
        minDuration: Long,
        maxDuration: Long,
    ): PagingSource<Int, TrackRow>

    @Query(TRACK_SELECT + SONG_FILTER + ORDER_TITLE_DESC)
    fun songsByTitleDesc(
        format: String?,
        minBitrate: Int,
        minDuration: Long,
        maxDuration: Long,
    ): PagingSource<Int, TrackRow>

    @Query(TRACK_SELECT + SONG_FILTER + ORDER_ARTIST_ASC)
    fun songsByArtistAsc(
        format: String?,
        minBitrate: Int,
        minDuration: Long,
        maxDuration: Long,
    ): PagingSource<Int, TrackRow>

    @Query(TRACK_SELECT + SONG_FILTER + ORDER_ARTIST_DESC)
    fun songsByArtistDesc(
        format: String?,
        minBitrate: Int,
        minDuration: Long,
        maxDuration: Long,
    ): PagingSource<Int, TrackRow>

    @Query(TRACK_SELECT + SONG_FILTER + ORDER_ALBUM_ASC)
    fun songsByAlbumAsc(
        format: String?,
        minBitrate: Int,
        minDuration: Long,
        maxDuration: Long,
    ): PagingSource<Int, TrackRow>

    @Query(TRACK_SELECT + SONG_FILTER + ORDER_ALBUM_DESC)
    fun songsByAlbumDesc(
        format: String?,
        minBitrate: Int,
        minDuration: Long,
        maxDuration: Long,
    ): PagingSource<Int, TrackRow>

    @Query(TRACK_SELECT + SONG_FILTER + ORDER_ADDED_ASC)
    fun songsByDateAddedAsc(
        format: String?,
        minBitrate: Int,
        minDuration: Long,
        maxDuration: Long,
    ): PagingSource<Int, TrackRow>

    @Query(TRACK_SELECT + SONG_FILTER + ORDER_ADDED_DESC)
    fun songsByDateAddedDesc(
        format: String?,
        minBitrate: Int,
        minDuration: Long,
        maxDuration: Long,
    ): PagingSource<Int, TrackRow>

    @Query(TRACK_SELECT + SONG_FILTER + ORDER_DURATION_ASC)
    fun songsByDurationAsc(
        format: String?,
        minBitrate: Int,
        minDuration: Long,
        maxDuration: Long,
    ): PagingSource<Int, TrackRow>

    @Query(TRACK_SELECT + SONG_FILTER + ORDER_DURATION_DESC)
    fun songsByDurationDesc(
        format: String?,
        minBitrate: Int,
        minDuration: Long,
        maxDuration: Long,
    ): PagingSource<Int, TrackRow>

    @Query(TRACK_SELECT + SONG_FILTER + ORDER_PLAYS_ASC)
    fun songsByPlayCountAsc(
        format: String?,
        minBitrate: Int,
        minDuration: Long,
        maxDuration: Long,
    ): PagingSource<Int, TrackRow>

    @Query(TRACK_SELECT + SONG_FILTER + ORDER_PLAYS_DESC)
    fun songsByPlayCountDesc(
        format: String?,
        minBitrate: Int,
        minDuration: Long,
        maxDuration: Long,
    ): PagingSource<Int, TrackRow>

    @Query("SELECT COUNT(*) FROM tracks")
    fun observeCount(): Flow<Int>

    // ---- Lists ----

    @Query(TRACK_SELECT + " WHERE t.albumId = :albumId ORDER BY t.discNumber ASC, t.trackNumber ASC, t.titleSort ASC")
    fun observeAlbumTracks(albumId: Long): Flow<List<TrackRow>>

    @Query(
        TRACK_SELECT +
            " WHERE t.artistId = :artistId OR t.albumArtistId = :artistId" +
            " ORDER BY t.albumTitle COLLATE NOCASE ASC, t.discNumber ASC, t.trackNumber ASC, t.titleSort ASC",
    )
    fun observeArtistTracks(artistId: Long): Flow<List<TrackRow>>

    @Query(TRACK_SELECT + " WHERE t.genre = :genre COLLATE NOCASE ORDER BY t.titleSort ASC, t.id ASC")
    fun observeGenreTracks(genre: String): Flow<List<TrackRow>>

    @Query(TRACK_SELECT + " WHERE t.folderPath = :path ORDER BY t.titleSort ASC, t.id ASC")
    fun observeFolderTracks(path: String): Flow<List<TrackRow>>

    @Query(TRACK_SELECT + " ORDER BY t.dateAddedMs DESC, t.id ASC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int): Flow<List<TrackRow>>

    @Query(
        TRACK_SELECT + " WHERE u.lastPlayedMs IS NOT NULL ORDER BY u.lastPlayedMs DESC, t.id ASC LIMIT :limit",
    )
    fun observeRecentlyPlayed(limit: Int): Flow<List<TrackRow>>

    @Query(
        TRACK_SELECT +
            " WHERE u.playCount > 0 ORDER BY u.playCount DESC, u.lastPlayedMs DESC, t.id ASC LIMIT :limit",
    )
    fun observeMostPlayed(limit: Int): Flow<List<TrackRow>>

    @Query(TRACK_SELECT + " WHERE u.isFavorite = 1 ORDER BY t.titleSort ASC, t.id ASC")
    fun observeFavorites(): Flow<List<TrackRow>>

    @Query(TRACK_SELECT + " WHERE t.id = :id")
    fun observeTrack(id: Long): Flow<TrackRow?>

    /** Callers keep [ids] under 500 entries (SQLite's variable limit on older Android versions is 999). */
    @Query(TRACK_SELECT + " WHERE t.id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<TrackRow>

    @Query(
        TRACK_SELECT +
            " WHERE t.id IN (SELECT rowid FROM tracks_fts WHERE tracks_fts MATCH :ftsQuery)" +
            " ORDER BY t.titleSort ASC, t.id ASC LIMIT :limit",
    )
    fun search(ftsQuery: String, limit: Int): Flow<List<TrackRow>>

    // ---- Albums, artists, genres, folders (derived from tracks) ----

    @Query(ALBUM_SELECT + " GROUP BY albumId ORDER BY MAX(albumTitle) COLLATE NOCASE ASC")
    fun observeAlbums(): Flow<List<AlbumRow>>

    @Query(ALBUM_SELECT + " WHERE albumId = :albumId GROUP BY albumId")
    fun observeAlbum(albumId: Long): Flow<AlbumRow?>

    @Query(
        ALBUM_SELECT +
            " WHERE artistId = :artistId OR albumArtistId = :artistId" +
            " GROUP BY albumId ORDER BY MAX(year) DESC, MAX(albumTitle) COLLATE NOCASE ASC",
    )
    fun observeArtistAlbums(artistId: Long): Flow<List<AlbumRow>>

    @Query(
        ALBUM_SELECT +
            " WHERE id IN (SELECT rowid FROM tracks_fts WHERE albumTitle MATCH :ftsQuery)" +
            " GROUP BY albumId ORDER BY MAX(albumTitle) COLLATE NOCASE ASC LIMIT :limit",
    )
    fun searchAlbums(ftsQuery: String, limit: Int): Flow<List<AlbumRow>>

    @Query(
        """
        SELECT artistId AS id, MAX(artistName) AS name, COUNT(DISTINCT albumId) AS albumCount,
               COUNT(*) AS trackCount, 0 AS isAlbumArtist
        FROM tracks GROUP BY artistId ORDER BY MAX(artistName) COLLATE NOCASE ASC
        """,
    )
    fun observeArtists(): Flow<List<ArtistRow>>

    @Query(
        """
        SELECT albumArtistId AS id, MAX(albumArtist) AS name, COUNT(DISTINCT albumId) AS albumCount,
               COUNT(*) AS trackCount, 1 AS isAlbumArtist
        FROM tracks WHERE albumArtistId IS NOT NULL
        GROUP BY albumArtistId ORDER BY MAX(albumArtist) COLLATE NOCASE ASC
        """,
    )
    fun observeAlbumArtists(): Flow<List<ArtistRow>>

    @Query(
        """
        SELECT artistId AS id, MAX(artistName) AS name, COUNT(DISTINCT albumId) AS albumCount,
               COUNT(*) AS trackCount, 0 AS isAlbumArtist
        FROM tracks WHERE artistId = :artistId GROUP BY artistId
        """,
    )
    fun observeArtist(artistId: Long): Flow<ArtistRow?>

    @Query(
        """
        SELECT albumArtistId AS id, MAX(albumArtist) AS name, COUNT(DISTINCT albumId) AS albumCount,
               COUNT(*) AS trackCount, 1 AS isAlbumArtist
        FROM tracks WHERE albumArtistId = :artistId GROUP BY albumArtistId
        """,
    )
    fun observeAlbumArtist(artistId: Long): Flow<ArtistRow?>

    @Query(
        """
        SELECT artistId AS id, MAX(artistName) AS name, COUNT(DISTINCT albumId) AS albumCount,
               COUNT(*) AS trackCount, 0 AS isAlbumArtist
        FROM tracks
        WHERE id IN (SELECT rowid FROM tracks_fts WHERE artistName MATCH :ftsQuery)
        GROUP BY artistId ORDER BY MAX(artistName) COLLATE NOCASE ASC LIMIT :limit
        """,
    )
    fun searchArtists(ftsQuery: String, limit: Int): Flow<List<ArtistRow>>

    @Query(
        """
        SELECT genre AS name, COUNT(*) AS trackCount FROM tracks
        WHERE genre IS NOT NULL AND genre != ''
        GROUP BY genre COLLATE NOCASE ORDER BY genre COLLATE NOCASE ASC
        """,
    )
    fun observeGenres(): Flow<List<GenreRow>>

    @Query("SELECT folderPath AS path, COUNT(*) AS count FROM tracks GROUP BY folderPath")
    fun observeFolderCounts(): Flow<List<FolderCountRow>>

    // ---- User data ----

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserDataIfAbsent(data: TrackUserDataEntity)

    @Query("UPDATE track_user_data SET isFavorite = :favorite WHERE trackId = :trackId")
    suspend fun updateFavorite(trackId: Long, favorite: Boolean)

    @Query("UPDATE track_user_data SET playCount = playCount + 1, lastPlayedMs = :playedAtMs WHERE trackId = :trackId")
    suspend fun incrementPlayCount(trackId: Long, playedAtMs: Long)
}
