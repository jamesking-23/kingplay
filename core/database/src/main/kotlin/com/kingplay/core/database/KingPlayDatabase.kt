package com.kingplay.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kingplay.core.database.dao.EqDao
import com.kingplay.core.database.dao.ExcludedFolderDao
import com.kingplay.core.database.dao.LyricsDao
import com.kingplay.core.database.dao.PlaylistDao
import com.kingplay.core.database.dao.QueueDao
import com.kingplay.core.database.dao.TrackDao
import com.kingplay.core.database.dao.VideoDao
import com.kingplay.core.database.entity.DeviceProfileEntity
import com.kingplay.core.database.entity.EqPresetEntity
import com.kingplay.core.database.entity.ExcludedFolderEntity
import com.kingplay.core.database.entity.ImportedLyricsEntity
import com.kingplay.core.database.entity.PlaylistEntity
import com.kingplay.core.database.entity.PlaylistEntryEntity
import com.kingplay.core.database.entity.QueueItemEntity
import com.kingplay.core.database.entity.QueueStateEntity
import com.kingplay.core.database.entity.TrackEntity
import com.kingplay.core.database.entity.TrackFtsEntity
import com.kingplay.core.database.entity.TrackUserDataEntity
import com.kingplay.core.database.entity.VideoEntity
import com.kingplay.core.database.entity.VideoFtsEntity
import com.kingplay.core.database.entity.VideoUserDataEntity

/**
 * The single source of truth for the library and user data. Schemas are exported to `schemas/`; every
 * change to an entity needs a version bump plus a `Migration` (never destructive: playlists live here).
 */
@Database(
    entities = [
        TrackEntity::class,
        TrackUserDataEntity::class,
        TrackFtsEntity::class,
        VideoEntity::class,
        VideoUserDataEntity::class,
        VideoFtsEntity::class,
        PlaylistEntity::class,
        PlaylistEntryEntity::class,
        QueueItemEntity::class,
        QueueStateEntity::class,
        EqPresetEntity::class,
        DeviceProfileEntity::class,
        ExcludedFolderEntity::class,
        ImportedLyricsEntity::class,
    ],
    version = KingPlayDatabase.VERSION,
    exportSchema = true,
)
abstract class KingPlayDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao

    abstract fun videoDao(): VideoDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun queueDao(): QueueDao

    abstract fun eqDao(): EqDao

    abstract fun excludedFolderDao(): ExcludedFolderDao

    abstract fun lyricsDao(): LyricsDao

    companion object {
        const val VERSION = 2
        const val NAME = "kingplay.db"
    }
}
