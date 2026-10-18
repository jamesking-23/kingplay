package com.kingplay.data.library.repo

import com.kingplay.core.database.dao.VideoDao
import com.kingplay.core.database.dao.VideoRow
import com.kingplay.core.database.entity.VideoUserDataEntity
import com.kingplay.core.database.toModel
import com.kingplay.core.model.VideoItem
import com.kingplay.core.model.VideoSort
import com.kingplay.core.model.VideoSortField
import com.kingplay.core.model.repository.VideoRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomVideoRepository @Inject constructor(
    private val videoDao: VideoDao,
) : VideoRepository {

    override fun videos(sort: VideoSort): Flow<List<VideoItem>> = videoDao.observeAll().map { rows ->
        val items = rows.map { it.toModel() }
        val comparator: Comparator<VideoItem> = when (sort.field) {
            VideoSortField.TITLE -> compareBy { it.title.lowercase(Locale.ROOT) }
            VideoSortField.DATE_ADDED -> compareBy { it.dateAddedMs }
            VideoSortField.DURATION -> compareBy { it.durationMs }
            VideoSortField.SIZE -> compareBy { it.sizeBytes }
        }
        items.sortedWith(if (sort.ascending) comparator else comparator.reversed())
    }

    override fun videoCount(): Flow<Int> = videoDao.observeCount()

    override fun video(id: Long): Flow<VideoItem?> = videoDao.observeVideo(id).map { it?.toModel() }

    override fun folderVideos(path: String): Flow<List<VideoItem>> = videoDao.observeFolderVideos(path).toItems()

    override fun recentlyPlayed(limit: Int): Flow<List<VideoItem>> = videoDao.observeRecentlyPlayed(limit).toItems()

    override fun continueWatching(limit: Int): Flow<List<VideoItem>> =
        videoDao.observeContinueWatching(limit).toItems()

    override suspend fun getVideos(ids: List<Long>): List<VideoItem> {
        if (ids.isEmpty()) return emptyList()
        val byId = ids.distinct().chunked(ID_CHUNK).flatMap { videoDao.getByIds(it) }.associateBy { it.video.id }
        return ids.mapNotNull { byId[it]?.toModel() }
    }

    override suspend fun saveResumePosition(videoId: Long, positionMs: Long, playedAtMs: Long) {
        val duration = videoDao.getByIds(listOf(videoId)).firstOrNull()?.video?.durationMs ?: 0L
        videoDao.saveUserData(
            VideoUserDataEntity(videoId, normalizedResumePosition(positionMs, duration), playedAtMs),
        )
    }

    private fun Flow<List<VideoRow>>.toItems(): Flow<List<VideoItem>> = map { rows -> rows.map { it.toModel() } }

    private companion object {
        const val ID_CHUNK = 500
    }
}
