package com.kingplay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kingplay.core.database.entity.ImportedLyricsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricsDao {
    @Query("SELECT lrc FROM imported_lyrics WHERE lyricsKey = :key")
    fun observe(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: ImportedLyricsEntity)

    @Query("DELETE FROM imported_lyrics WHERE lyricsKey = :key")
    suspend fun delete(key: String)

    @Query("SELECT * FROM imported_lyrics")
    suspend fun all(): List<ImportedLyricsEntity>
}
