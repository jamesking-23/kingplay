package com.kingplay.data.library.repo

import androidx.room.withTransaction
import com.kingplay.core.database.KingPlayDatabase
import com.kingplay.core.database.dao.PlaylistDao
import com.kingplay.core.database.dao.TrackDao
import com.kingplay.core.database.entity.PlaylistEntity
import com.kingplay.core.database.entity.PlaylistEntryEntity
import com.kingplay.core.database.entity.RefType
import com.kingplay.core.database.refType
import com.kingplay.core.database.toModel
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.Playlist
import com.kingplay.core.model.PlaylistEntry
import com.kingplay.core.model.PlaylistImportResult
import com.kingplay.core.model.Track
import com.kingplay.core.model.playlist.M3uEntry
import com.kingplay.core.model.playlist.M3uParser
import com.kingplay.core.model.playlist.M3uWriter
import com.kingplay.core.model.playlist.PathRow
import com.kingplay.core.model.playlist.TrackPathIndex
import com.kingplay.core.model.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomPlaylistRepository @Inject constructor(
    private val db: KingPlayDatabase,
    private val playlistDao: PlaylistDao,
    private val trackDao: TrackDao,
) : PlaylistRepository {

    override fun playlists(): Flow<List<Playlist>> =
        playlistDao.observePlaylists().map { rows -> rows.map { it.toModel() } }

    override fun playlist(id: Long): Flow<Playlist?> = playlistDao.observePlaylist(id).map { it?.toModel() }

    override fun entries(playlistId: Long): Flow<List<PlaylistEntry>> =
        playlistDao.observeEntries(playlistId).map { rows -> rows.map { it.toModel() } }

    override fun tracks(playlistId: Long): Flow<List<Track>> =
        playlistDao.observeTracks(playlistId).map { rows -> rows.map { it.toModel() } }

    override suspend fun create(name: String): Long {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Playlist name must not be blank" }
        val now = System.currentTimeMillis()
        return playlistDao.insertPlaylist(PlaylistEntity(name = clean, createdAtMs = now, updatedAtMs = now))
    }

    override suspend fun rename(id: Long, name: String) {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Playlist name must not be blank" }
        playlistDao.rename(id, clean, System.currentTimeMillis())
    }

    override suspend fun delete(id: Long) = playlistDao.delete(id)

    override suspend fun add(playlistId: Long, refs: List<MediaRef>) {
        if (refs.isEmpty()) return
        db.withTransaction {
            val start = playlistDao.getEntries(playlistId).size
            playlistDao.insertEntries(
                refs.mapIndexed { index, ref ->
                    PlaylistEntryEntity(
                        playlistId = playlistId,
                        position = start + index,
                        refType = ref.refType(),
                        refId = ref.id,
                    )
                },
            )
            playlistDao.touch(playlistId, System.currentTimeMillis())
        }
    }

    override suspend fun removeAt(playlistId: Long, position: Int) {
        db.withTransaction {
            val current = playlistDao.getEntries(playlistId)
            if (position in current.indices) rewrite(playlistId, removeItem(current, position))
        }
    }

    override suspend fun move(playlistId: Long, from: Int, to: Int) {
        db.withTransaction {
            val current = playlistDao.getEntries(playlistId)
            val moved = moveItem(current, from, to)
            if (moved !== current) rewrite(playlistId, moved)
        }
    }

    override suspend fun createFrom(name: String, refs: List<MediaRef>): Long = db.withTransaction {
        create(name).also { add(it, refs) }
    }

    override suspend fun exportM3u(playlistId: Long): String {
        val audioIds = playlistDao.getEntries(playlistId).filter { it.refType == RefType.AUDIO }.map { it.refId }
        val byId = audioIds.distinct().chunked(ID_CHUNK).flatMap { trackDao.getByIds(it) }.associateBy { it.track.id }
        val entries = audioIds.mapNotNull { byId[it]?.track }.map { t ->
            M3uEntry(
                path = t.folderPath.trimEnd('/') + "/" + t.fileName,
                durationSec = (t.durationMs / MS_PER_SECOND).toInt(),
                title = "${t.artistName} - ${t.title}",
            )
        }
        return M3uWriter.write(entries)
    }

    override suspend fun importM3u(name: String, content: String): PlaylistImportResult {
        val paths = M3uParser.parse(content)
        val index = TrackPathIndex(trackDao.allPaths().map { PathRow(it.id, it.folderPath, it.fileName) })
        val matchedIds = paths.mapNotNull(index::match)
        val playlistName = name.trim().ifEmpty { DEFAULT_IMPORT_NAME }
        val id = createFrom(playlistName, matchedIds.map { MediaRef.Audio(it) })
        return PlaylistImportResult(playlistId = id, matched = matchedIds.size, total = paths.size)
    }

    /** Replaces all entries with [items] renumbered from zero, inside the caller's transaction. */
    private suspend fun rewrite(playlistId: Long, items: List<PlaylistEntryEntity>) {
        playlistDao.deleteEntries(playlistId)
        playlistDao.insertEntries(items.mapIndexed { index, e -> e.copy(entryId = 0, position = index) })
        playlistDao.touch(playlistId, System.currentTimeMillis())
    }

    private companion object {
        const val ID_CHUNK = 500
        const val MS_PER_SECOND = 1000L
        const val DEFAULT_IMPORT_NAME = "Imported playlist"
    }
}
