package com.kingplay.core.model.repository

import com.kingplay.core.model.backup.RestoreResult
import kotlinx.coroutines.flow.Flow

/** User-saved lyrics (imported .lrc files), keyed by [com.kingplay.core.model.lyrics.lyricsKey]. */
interface LyricsStore {
    fun observe(key: String): Flow<String?>

    suspend fun save(key: String, lrc: String)

    suspend fun remove(key: String)

    suspend fun all(): Map<String, String>
}

/** Writes everything the user made (not the music itself) to one text file, and reads it back. */
interface BackupRepository {
    suspend fun create(): String

    suspend fun restore(text: String): RestoreResult
}
