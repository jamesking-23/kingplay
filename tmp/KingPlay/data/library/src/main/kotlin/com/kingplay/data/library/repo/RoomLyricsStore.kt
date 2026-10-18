package com.kingplay.data.library.repo

import com.kingplay.core.database.dao.LyricsDao
import com.kingplay.core.database.entity.ImportedLyricsEntity
import com.kingplay.core.model.repository.LyricsStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class RoomLyricsStore @Inject constructor(private val dao: LyricsDao) : LyricsStore {
    override fun observe(key: String): Flow<String?> = dao.observe(key)

    override suspend fun save(key: String, lrc: String) =
        dao.save(ImportedLyricsEntity(lyricsKey = key, lrc = lrc, updatedAtMs = System.currentTimeMillis()))

    override suspend fun remove(key: String) = dao.delete(key)

    override suspend fun all(): Map<String, String> = dao.all().associate { it.lyricsKey to it.lrc }
}
