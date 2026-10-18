package com.kingplay.core.model.repository

import com.kingplay.core.model.DeviceProfile
import com.kingplay.core.model.EqPreset
import kotlinx.coroutines.flow.Flow

/** EQ presets (built-in plus user-saved) and per-output-device profiles. */
interface EqRepository {
    /** Built-in presets first, then user presets by name. */
    fun presets(): Flow<List<EqPreset>>

    suspend fun preset(id: Long): EqPreset?

    /** Saves a user preset. A preset with id 0 or lower is inserted; otherwise it is updated. Returns its id. */
    suspend fun savePreset(preset: EqPreset): Long

    /** Deletes a user preset. Built-in presets are never removed. Profiles that used it fall back to no preset. */
    suspend fun deletePreset(id: Long)

    fun deviceProfiles(): Flow<List<DeviceProfile>>

    suspend fun deviceProfile(deviceKey: String): DeviceProfile?

    suspend fun saveDeviceProfile(profile: DeviceProfile)

    suspend fun deleteDeviceProfile(deviceKey: String)
}
