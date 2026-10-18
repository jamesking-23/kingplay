package com.kingplay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kingplay.core.database.entity.PlaylistEntity
import com.kingplay.core.database.entity.PlaylistEntryEntity
import kotlinx.coroutines.flow.Flow

private const val PLAYLIST_SELECT = """
    SELECT p.id AS id, p.name AS name, p.createdAtMs AS createdAtMs, p.updatedAtMs AS updatedAtMs,
           (SELECT COUNT(*) FROM playlist_entries e WHERE e.playlistId = p.id) AS entryCount
    FROM playlists p
"""

@Dao
interface PlaylistDao {

    @Query(PLAYLIST_SELECT + " ORDER BY p.name COLLATE NOCASE ASC, p.id ASC")
    fun observePlaylists(): Flow<List<PlaylistRow>>

    @Query(PLAYLIST_SELECT + " WHERE p.id = :id")
    fun observePlaylist(id: Long): Flow<PlaylistRow?>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, updatedAtMs = :nowMs WHERE id = :id")
    suspend fun rename(id: Long, name: String, nowMs: Long)

    @Query("UPDATE playlists SET updatedAtMs = :nowMs WHERE id = :id")
    suspend fun touch(id: Long, nowMs: Long)

    /** Entries are removed by the foreign key cascade. */
    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM playlist_entries WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeEntries(playlistId: Long): Flow<List<PlaylistEntryEntity>>

    @Query("SELECT * FROM playlist_entries WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getEntries(playlistId: Long): List<PlaylistEntryEntity>

    @Insert
    suspend fun insertEntries(entries: List<PlaylistEntryEntity>)

    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun deleteEntries(playlistId: Long)

    @Query(
        TRACK_SELECT +
            " JOIN playlist_entries e ON e.refId = t.id AND e.refType = 0" +
            " WHERE e.playlistId = :playlistId ORDER BY e.position ASC",
    )
    fun observeTracks(playlistId: Long): Flow<List<TrackRow>>
}
