package com.kingplay.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "videos",
    indices = [Index("folderPath"), Index("dateAddedMs")],
)
data class VideoEntity(
    @PrimaryKey val id: Long,
    val uri: String,
    val title: String,
    val titleSort: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val videoCodec: String?,
    val container: String,
    val folderPath: String,
    val fileName: String,
    val sizeBytes: Long,
    val dateAddedMs: Long,
    val dateModifiedMs: Long,
)

@Entity(tableName = "video_user_data")
data class VideoUserDataEntity(
    @PrimaryKey val videoId: Long,
    val resumePositionMs: Long,
    val lastPlayedMs: Long?,
)

@Fts4(contentEntity = VideoEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "videos_fts")
data class VideoFtsEntity(
    val title: String,
)
