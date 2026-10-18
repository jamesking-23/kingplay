package com.kingplay.core.model.repository

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.Playlist
import com.kingplay.core.model.PlaylistEntry
import com.kingplay.core.model.PlaylistImportResult
import com.kingplay.core.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun playlists(): Flow<List<Playlist>>

    fun playlist(id: Long): Flow<Playlist?>

    /** All entries in order, including ones whose file is currently missing from the library. */
    fun entries(playlistId: Long): Flow<List<PlaylistEntry>>

    /** Audio entries resolved to tracks, in playlist order. Missing files are skipped. */
    fun tracks(playlistId: Long): Flow<List<Track>>

    suspend fun create(name: String): Long

    suspend fun rename(id: Long, name: String)

    suspend fun delete(id: Long)

    suspend fun add(playlistId: Long, refs: List<MediaRef>)

    /** Removes the entry at [position] and closes the gap. */
    suspend fun removeAt(playlistId: Long, position: Int)

    suspend fun move(playlistId: Long, from: Int, to: Int)

    /** Creates a playlist from the current queue and returns its id. */
    suspend fun createFrom(name: String, refs: List<MediaRef>): Long

    suspend fun exportM3u(playlistId: Long): String

    /** Matches the paths in an M3U file against the library and creates a playlist from the matches. */
    suspend fun importM3u(name: String, content: String): PlaylistImportResult
}
