package com.kingplay.core.model.repository

import com.kingplay.core.model.QueueState

/** Persists the play queue so it survives app restarts. */
interface QueueRepository {
    suspend fun load(): QueueState

    /** Full write. Call when the queue contents, order, shuffle or repeat mode change. */
    suspend fun save(state: QueueState)

    /** Cheap write for the high-frequency case where only the playback position moved. */
    suspend fun savePosition(currentIndex: Int, positionMs: Long)
}
