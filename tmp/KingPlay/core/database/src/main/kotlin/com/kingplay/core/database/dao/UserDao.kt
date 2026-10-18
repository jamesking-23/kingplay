package com.kingplay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kingplay.core.database.entity.DeviceProfileEntity
import com.kingplay.core.database.entity.EqPresetEntity
import com.kingplay.core.database.entity.ExcludedFolderEntity
import com.kingplay.core.database.entity.QueueItemEntity
import com.kingplay.core.database.entity.QueueStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    suspend fun getItems(): List<QueueItemEntity>

    @Query("SELECT * FROM queue_state WHERE id = 1")
    suspend fun getState(): QueueStateEntity?

    @Query("DELETE FROM queue_items")
    suspend fun clearItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<QueueItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveState(state: QueueStateEntity)

    @Query("UPDATE queue_state SET currentIndex = :currentIndex, positionMs = :positionMs WHERE id = 1")
    suspend fun updatePosition(currentIndex: Int, positionMs: Long)
}

@Dao
interface EqDao {
    @Query("SELECT * FROM eq_presets ORDER BY name COLLATE NOCASE ASC, id ASC")
    fun observePresets(): Flow<List<EqPresetEntity>>

    @Query("SELECT * FROM eq_presets WHERE id = :id")
    suspend fun getPreset(id: Long): EqPresetEntity?

    /** Returns the row id for both inserts and replacements. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreset(preset: EqPresetEntity): Long

    @Query("DELETE FROM eq_presets WHERE id = :id")
    suspend fun deletePreset(id: Long)

    @Query("UPDATE device_profiles SET eqPresetId = NULL WHERE eqPresetId = :presetId")
    suspend fun clearPresetFromProfiles(presetId: Long)

    @Query("SELECT * FROM device_profiles ORDER BY displayName COLLATE NOCASE ASC")
    fun observeProfiles(): Flow<List<DeviceProfileEntity>>

    @Query("SELECT * FROM device_profiles WHERE deviceKey = :deviceKey")
    suspend fun getProfile(deviceKey: String): DeviceProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: DeviceProfileEntity)

    @Query("DELETE FROM device_profiles WHERE deviceKey = :deviceKey")
    suspend fun deleteProfile(deviceKey: String)
}

@Dao
interface ExcludedFolderDao {
    @Query("SELECT path FROM excluded_folders ORDER BY path ASC")
    fun observePaths(): Flow<List<String>>

    @Query("SELECT path FROM excluded_folders")
    suspend fun getPaths(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(folder: ExcludedFolderEntity)

    @Query("DELETE FROM excluded_folders WHERE path = :path")
    suspend fun delete(path: String)
}
