package com.kingplay.core.model.repository

import kotlinx.coroutines.flow.Flow

/** Lifetime numbers shown on the profile. Listening time and video count start from the version that added them. */
data class PlaybackRecordsData(
    val songPlays: Long = 0,
    val videoPlays: Long = 0,
    val audioListenMs: Long = 0,
    val videoListenMs: Long = 0,
    /** When KingPlay was first installed on this device, in epoch milliseconds (0 when unknown). */
    val installedAtMs: Long = 0,
) {
    val totalListenMs: Long get() = audioListenMs + videoListenMs
}

interface PlaybackRecordsRepository {
    fun records(): Flow<PlaybackRecordsData>

    /** Adds real playing time, split by kind so music and video can be told apart. */
    suspend fun addListening(audioMs: Long, videoMs: Long)

    suspend fun addVideoPlay()
}
