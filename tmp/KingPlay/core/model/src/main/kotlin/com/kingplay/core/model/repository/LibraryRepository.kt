package com.kingplay.core.model.repository

import androidx.paging.PagingData
import com.kingplay.core.model.Album
import com.kingplay.core.model.Artist
import com.kingplay.core.model.Genre
import com.kingplay.core.model.MediaFolder
import com.kingplay.core.model.SongFilter
import com.kingplay.core.model.SongSort
import com.kingplay.core.model.Track
import kotlinx.coroutines.flow.Flow

/** Read access to the audio library plus the small amount of user data attached to tracks. Room is the source. */
interface LibraryRepository {
    /** Paged so lists of 50,000+ tracks stay smooth. */
    fun songs(sort: SongSort = SongSort(), filter: SongFilter = SongFilter.None): Flow<PagingData<Track>>

    fun songCount(): Flow<Int>

    fun albums(): Flow<List<Album>>

    fun album(albumId: Long): Flow<Album?>

    fun albumTracks(albumId: Long): Flow<List<Track>>

    fun artists(): Flow<List<Artist>>

    fun albumArtists(): Flow<List<Artist>>

    fun artist(artistId: Long): Flow<Artist?>

    fun artistAlbums(artistId: Long): Flow<List<Album>>

    fun artistTracks(artistId: Long): Flow<List<Track>>

    fun genres(): Flow<List<Genre>>

    fun genreTracks(genre: String): Flow<List<Track>>

    /** Folders that contain audio or video, merged into one list. */
    fun folders(): Flow<List<MediaFolder>>

    fun folderTracks(path: String): Flow<List<Track>>

    fun recentlyAdded(limit: Int = DEFAULT_LIMIT): Flow<List<Track>>

    fun recentlyPlayed(limit: Int = DEFAULT_LIMIT): Flow<List<Track>>

    fun mostPlayed(limit: Int = DEFAULT_LIMIT): Flow<List<Track>>

    fun favorites(): Flow<List<Track>>

    fun track(id: Long): Flow<Track?>

    /** Resolves ids to tracks, keeping the order of [ids]. Ids that no longer exist are skipped. */
    suspend fun getTracks(ids: List<Long>): List<Track>

    suspend fun setFavorite(trackId: Long, favorite: Boolean)

    /** Called by the player when a track has been played long enough to count. */
    suspend fun recordPlay(trackId: Long, playedAtMs: Long)

    companion object {
        const val DEFAULT_LIMIT = 50
    }
}
