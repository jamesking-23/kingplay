package com.kingplay.data.library.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import androidx.room.withTransaction
import com.kingplay.core.database.KingPlayDatabase
import com.kingplay.core.database.dao.TrackDao
import com.kingplay.core.database.dao.TrackRow
import com.kingplay.core.database.dao.songIdsQuery
import com.kingplay.core.database.dao.VideoDao
import com.kingplay.core.database.entity.TrackUserDataEntity
import com.kingplay.core.database.toModel
import com.kingplay.core.model.Album
import com.kingplay.core.model.Artist
import com.kingplay.core.model.Genre
import com.kingplay.core.model.MediaFolder
import com.kingplay.core.model.SongFilter
import com.kingplay.core.model.SongSort
import com.kingplay.core.model.SongSortField
import com.kingplay.core.model.Track
import com.kingplay.core.model.repository.LibraryRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
@Suppress("TooManyFunctions")
class RoomLibraryRepository @Inject constructor(
    private val db: KingPlayDatabase,
    private val trackDao: TrackDao,
    private val videoDao: VideoDao,
) : LibraryRepository {

    override fun songs(sort: SongSort, filter: SongFilter): Flow<PagingData<Track>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, initialLoadSize = PAGE_SIZE * 2, enablePlaceholders = true),
        pagingSourceFactory = { songSource(sort, filter) },
    ).flow.map { page -> page.map { row -> row.toModel() } }

    override suspend fun songIds(sort: SongSort, filter: SongFilter): List<Long> =
        trackDao.songIdsRaw(songIdsQuery(sort, filter))

    override fun songCount(): Flow<Int> = trackDao.observeCount()

    override fun albums(): Flow<List<Album>> = trackDao.observeAlbums().map { rows -> rows.map { it.toModel() } }

    override fun album(albumId: Long): Flow<Album?> = trackDao.observeAlbum(albumId).map { it?.toModel() }

    override fun albumTracks(albumId: Long): Flow<List<Track>> = trackDao.observeAlbumTracks(albumId).toTracks()

    override fun artists(): Flow<List<Artist>> = trackDao.observeArtists().map { rows -> rows.map { it.toModel() } }

    override fun albumArtists(): Flow<List<Artist>> =
        trackDao.observeAlbumArtists().map { rows -> rows.map { it.toModel() } }

    /** An id can belong to a track artist, an album artist, or both; the track artist row wins. */
    override fun artist(artistId: Long): Flow<Artist?> = combine(
        trackDao.observeArtist(artistId),
        trackDao.observeAlbumArtist(artistId),
    ) { asArtist, asAlbumArtist -> (asArtist ?: asAlbumArtist)?.toModel() }

    override fun artistAlbums(artistId: Long): Flow<List<Album>> =
        trackDao.observeArtistAlbums(artistId).map { rows -> rows.map { it.toModel() } }

    override fun artistTracks(artistId: Long): Flow<List<Track>> = trackDao.observeArtistTracks(artistId).toTracks()

    override fun genres(): Flow<List<Genre>> = trackDao.observeGenres().map { rows -> rows.map { it.toModel() } }

    override fun genreTracks(genre: String): Flow<List<Track>> = trackDao.observeGenreTracks(genre).toTracks()

    override fun folders(): Flow<List<MediaFolder>> = combine(
        trackDao.observeFolderCounts(),
        videoDao.observeFolderCounts(),
    ) { audio, video ->
        val audioCounts = audio.associate { it.path to it.count }
        val videoCounts = video.associate { it.path to it.count }
        (audioCounts.keys + videoCounts.keys)
            .map { path -> MediaFolder(path, audioCounts[path] ?: 0, videoCounts[path] ?: 0) }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    override fun folderTracks(path: String): Flow<List<Track>> = trackDao.observeFolderTracks(path).toTracks()

    override fun recentlyAdded(limit: Int): Flow<List<Track>> = trackDao.observeRecentlyAdded(limit).toTracks()

    override fun recentlyPlayed(limit: Int): Flow<List<Track>> = trackDao.observeRecentlyPlayed(limit).toTracks()

    override fun mostPlayed(limit: Int): Flow<List<Track>> = trackDao.observeMostPlayed(limit).toTracks()

    override fun favorites(): Flow<List<Track>> = trackDao.observeFavorites().toTracks()

    override fun track(id: Long): Flow<Track?> = trackDao.observeTrack(id).map { it?.toModel() }

    override suspend fun getTracks(ids: List<Long>): List<Track> {
        if (ids.isEmpty()) return emptyList()
        val byId = ids.distinct().chunked(ID_CHUNK).flatMap { trackDao.getByIds(it) }.associateBy { it.track.id }
        return ids.mapNotNull { byId[it]?.toModel() }
    }

    override suspend fun setFavorite(trackId: Long, favorite: Boolean) {
        db.withTransaction {
            trackDao.insertUserDataIfAbsent(TrackUserDataEntity(trackId, 0, null, false))
            trackDao.updateFavorite(trackId, favorite)
        }
    }

    override suspend fun recordPlay(trackId: Long, playedAtMs: Long) {
        db.withTransaction {
            trackDao.insertUserDataIfAbsent(TrackUserDataEntity(trackId, 0, null, false))
            trackDao.incrementPlayCount(trackId, playedAtMs)
        }
    }

    private fun Flow<List<TrackRow>>.toTracks(): Flow<List<Track>> = map { rows -> rows.map { it.toModel() } }

    private fun songSource(sort: SongSort, filter: SongFilter): PagingSource<Int, TrackRow> {
        val format = filter.format?.trim()?.uppercase(Locale.ROOT)?.ifEmpty { null }
        val bitrate = filter.minBitrateKbps
        val minDuration = filter.minDurationMs
        val maxDuration = filter.maxDurationMs
        val asc = sort.ascending
        val dao = trackDao
        return when (sort.field) {
            SongSortField.TITLE ->
                if (asc) dao.songsByTitleAsc(format, bitrate, minDuration, maxDuration)
                else dao.songsByTitleDesc(format, bitrate, minDuration, maxDuration)
            SongSortField.ARTIST ->
                if (asc) dao.songsByArtistAsc(format, bitrate, minDuration, maxDuration)
                else dao.songsByArtistDesc(format, bitrate, minDuration, maxDuration)
            SongSortField.ALBUM ->
                if (asc) dao.songsByAlbumAsc(format, bitrate, minDuration, maxDuration)
                else dao.songsByAlbumDesc(format, bitrate, minDuration, maxDuration)
            SongSortField.DATE_ADDED ->
                if (asc) dao.songsByDateAddedAsc(format, bitrate, minDuration, maxDuration)
                else dao.songsByDateAddedDesc(format, bitrate, minDuration, maxDuration)
            SongSortField.DURATION ->
                if (asc) dao.songsByDurationAsc(format, bitrate, minDuration, maxDuration)
                else dao.songsByDurationDesc(format, bitrate, minDuration, maxDuration)
            SongSortField.PLAY_COUNT ->
                if (asc) dao.songsByPlayCountAsc(format, bitrate, minDuration, maxDuration)
                else dao.songsByPlayCountDesc(format, bitrate, minDuration, maxDuration)
        }
    }

    private companion object {
        const val PAGE_SIZE = 50
        const val ID_CHUNK = 500
    }
}
