package com.kingplay.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** [refType] values for entries that point at media. */
object RefType {
    const val AUDIO = 0
    const val VIDEO = 1
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

@Entity(
    tableName = "playlist_entries",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["playlistId", "position"])],
)
data class PlaylistEntryEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    val playlistId: Long,
    val position: Int,
    val refType: Int,
    val refId: Long,
)

@Entity(tableName = "queue_items")
data class QueueItemEntity(
    @PrimaryKey val position: Int,
    val refType: Int,
    val refId: Long,
)

/** Single-row table: [id] is always [SINGLETON_ID]. */
@Entity(tableName = "queue_state")
data class QueueStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val currentIndex: Int,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    /** Comma-separated indices into the queue. */
    val shuffleOrder: String,
    val repeatMode: String,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Entity(tableName = "eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val preampDb: Float,
    /** Comma-separated gains, one per band. */
    val bandGainsDb: String,
)

@Entity(tableName = "device_profiles")
data class DeviceProfileEntity(
    @PrimaryKey val deviceKey: String,
    val deviceType: String,
    val displayName: String,
    val eqPresetId: Long?,
    val bassBoostStrength: Int,
    val loudnessGainMb: Int,
    val tempo: Float,
    val pitchSemitones: Float,
    val replayGainMode: String,
)

@Entity(tableName = "excluded_folders")
data class ExcludedFolderEntity(
    @PrimaryKey val path: String,
)
