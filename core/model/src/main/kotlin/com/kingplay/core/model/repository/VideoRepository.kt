package com.kingplay.core.model.repository

import com.kingplay.core.model.VideoItem
import com.kingplay.core.model.VideoSort
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun videos(sort: VideoSort = VideoSort()): Flow<List<VideoItem>>

    fun videoCount(): Flow<Int>

    fun video(id: Long): Flow<VideoItem?>

    fun folderVideos(path: String): Flow<List<VideoItem>>

    fun recentlyPlayed(limit: Int = LibraryRepository.DEFAULT_LIMIT): Flow<List<VideoItem>>

    /** Videos that were started but not finished, most recent first. */
    fun continueWatching(limit: Int = LibraryRepository.DEFAULT_LIMIT): Flow<List<VideoItem>>

    suspend fun getVideos(ids: List<Long>): List<VideoItem>

    /** Stores where playback stopped. A position near the end clears the resume point. */
    suspend fun saveResumePosition(videoId: Long, positionMs: Long, playedAtMs: Long)
}
