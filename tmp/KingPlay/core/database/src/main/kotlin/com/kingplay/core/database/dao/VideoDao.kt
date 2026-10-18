package com.kingplay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.kingplay.core.database.entity.VideoEntity
import com.kingplay.core.database.entity.VideoUserDataEntity
import kotlinx.coroutines.flow.Flow

internal const val VIDEO_SELECT = """
    SELECT v.*, COALESCE(u.resumePositionMs, 0) AS resumePositionMs, u.lastPlayedMs AS lastPlayedMs
    FROM videos v LEFT JOIN video_user_data u ON u.videoId = v.id
"""

@Dao
interface VideoDao {

    @Query("SELECT id, dateModifiedMs, sizeBytes, folderPath FROM videos")
    suspend fun syncSnapshot(): List<SyncRow>

    @Upsert
    suspend fun upsertAll(videos: List<VideoEntity>)

    @Query("DELETE FROM videos WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query(VIDEO_SELECT + " ORDER BY v.titleSort ASC, v.id ASC")
    fun observeAll(): Flow<List<VideoRow>>

    @Query("SELECT COUNT(*) FROM videos")
    fun observeCount(): Flow<Int>

    @Query(VIDEO_SELECT + " WHERE v.id = :id")
    fun observeVideo(id: Long): Flow<VideoRow?>

    @Query(VIDEO_SELECT + " WHERE v.folderPath = :path ORDER BY v.titleSort ASC, v.id ASC")
    fun observeFolderVideos(path: String): Flow<List<VideoRow>>

    @Query(VIDEO_SELECT + " WHERE u.lastPlayedMs IS NOT NULL ORDER BY u.lastPlayedMs DESC, v.id ASC LIMIT :limit")
    fun observeRecentlyPlayed(limit: Int): Flow<List<VideoRow>>

    /** Started but not finished: some progress, and more than 5 seconds left. */
    @Query(
        VIDEO_SELECT +
            " WHERE u.resumePositionMs > 0 AND u.resumePositionMs < v.durationMs - 5000" +
            " ORDER BY u.lastPlayedMs DESC, v.id ASC LIMIT :limit",
    )
    fun observeContinueWatching(limit: Int): Flow<List<VideoRow>>

    @Query(VIDEO_SELECT + " WHERE v.id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<VideoRow>

    @Query(
        VIDEO_SELECT +
            " WHERE v.id IN (SELECT rowid FROM videos_fts WHERE videos_fts MATCH :ftsQuery)" +
            " ORDER BY v.titleSort ASC, v.id ASC LIMIT :limit",
    )
    fun search(ftsQuery: String, limit: Int): Flow<List<VideoRow>>

    @Query("SELECT folderPath AS path, COUNT(*) AS count FROM videos GROUP BY folderPath")
    fun observeFolderCounts(): Flow<List<FolderCountRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserData(data: VideoUserDataEntity)
}
