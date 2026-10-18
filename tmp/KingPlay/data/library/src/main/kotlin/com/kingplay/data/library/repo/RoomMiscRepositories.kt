package com.kingplay.data.library.repo

import androidx.room.withTransaction
import com.kingplay.core.database.KingPlayDatabase
import com.kingplay.core.database.dao.EqDao
import com.kingplay.core.database.dao.ExcludedFolderDao
import com.kingplay.core.database.dao.QueueDao
import com.kingplay.core.database.dao.TrackDao
import com.kingplay.core.database.dao.VideoDao
import com.kingplay.core.database.entity.ExcludedFolderEntity
import com.kingplay.core.database.queueStateOf
import com.kingplay.core.database.toEntities
import com.kingplay.core.database.toEntity
import com.kingplay.core.database.toModel
import com.kingplay.core.model.BuiltInEqPresets
import com.kingplay.core.model.DeviceProfile
import com.kingplay.core.model.EqPreset
import com.kingplay.core.model.QueueState
import com.kingplay.core.model.SearchResults
import com.kingplay.core.model.repository.EqRepository
import com.kingplay.core.model.repository.LibraryFolderSettings
import com.kingplay.core.model.repository.LibraryScanner
import com.kingplay.core.model.repository.QueueRepository
import com.kingplay.core.model.repository.SearchRepository
import com.kingplay.data.library.search.FtsQuery
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class RoomQueueRepository @Inject constructor(
    private val db: KingPlayDatabase,
    private val queueDao: QueueDao,
) : QueueRepository {

    override suspend fun load(): QueueState = queueStateOf(queueDao.getState(), queueDao.getItems())

    override suspend fun save(state: QueueState) {
        val (stateRow, itemRows) = state.toEntities()
        db.withTransaction {
            queueDao.clearItems()
            queueDao.insertItems(itemRows)
            queueDao.saveState(stateRow)
        }
    }

    override suspend fun savePosition(currentIndex: Int, positionMs: Long) =
        queueDao.updatePosition(currentIndex, positionMs)
}

@Singleton
class RoomSearchRepository @Inject constructor(
    private val trackDao: TrackDao,
    private val videoDao: VideoDao,
) : SearchRepository {

    override fun search(query: String, limitPerSection: Int): Flow<SearchResults> {
        val fts = FtsQuery.build(query) ?: return flowOf(SearchResults.Empty)
        return combine(
            trackDao.search(fts, limitPerSection),
            trackDao.searchAlbums(fts, limitPerSection),
            trackDao.searchArtists(fts, limitPerSection),
            videoDao.search(fts, limitPerSection),
        ) { tracks, albums, artists, videos ->
            SearchResults(
                tracks = tracks.map { it.toModel() },
                albums = albums.map { it.toModel() },
                artists = artists.map { it.toModel() },
                videos = videos.map { it.toModel() },
            )
        }
    }
}

@Singleton
class RoomEqRepository @Inject constructor(
    private val db: KingPlayDatabase,
    private val eqDao: EqDao,
) : EqRepository {

    override fun presets(): Flow<List<EqPreset>> =
        eqDao.observePresets().map { custom -> BuiltInEqPresets.all + custom.map { it.toModel() } }

    override suspend fun preset(id: Long): EqPreset? =
        if (id > 0) eqDao.getPreset(id)?.toModel() else BuiltInEqPresets.byId(id)

    override suspend fun savePreset(preset: EqPreset): Long {
        require(!preset.isBuiltIn) { "Built-in presets cannot be modified" }
        return eqDao.savePreset(preset.toEntity())
    }

    override suspend fun deletePreset(id: Long) {
        if (id <= 0) return
        db.withTransaction {
            eqDao.clearPresetFromProfiles(id)
            eqDao.deletePreset(id)
        }
    }

    override fun deviceProfiles(): Flow<List<DeviceProfile>> =
        eqDao.observeProfiles().map { rows -> rows.map { it.toModel() } }

    override suspend fun deviceProfile(deviceKey: String): DeviceProfile? = eqDao.getProfile(deviceKey)?.toModel()

    override suspend fun saveDeviceProfile(profile: DeviceProfile) = eqDao.saveProfile(profile.toEntity())

    override suspend fun deleteDeviceProfile(deviceKey: String) = eqDao.deleteProfile(deviceKey)
}

@Singleton
class RoomLibraryFolderSettings @Inject constructor(
    private val dao: ExcludedFolderDao,
    private val scanner: LibraryScanner,
) : LibraryFolderSettings {

    override val excludedFolders: Flow<List<String>> = dao.observePaths()

    override suspend fun exclude(path: String) {
        val clean = path.trim().trimEnd('/')
        if (clean.isEmpty()) return
        dao.insert(ExcludedFolderEntity(clean))
        scanner.requestScan()
    }

    override suspend fun include(path: String) {
        dao.delete(path.trim().trimEnd('/'))
        scanner.requestScan()
    }
}
