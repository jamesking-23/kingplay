package com.kingplay.core.database.dao

import androidx.room.Embedded
import com.kingplay.core.database.entity.TrackEntity
import com.kingplay.core.database.entity.VideoEntity

/** A track joined with its user data. Room fills [track] from the `t.*` columns. */
data class TrackRow(
    @Embedded val track: TrackEntity,
    val playCount: Int,
    val lastPlayedMs: Long?,
    val isFavorite: Boolean,
)

data class VideoRow(
    @Embedded val video: VideoEntity,
    val resumePositionMs: Long,
    val lastPlayedMs: Long?,
)

data class AlbumRow(
    val id: Long,
    val title: String,
    val artistName: String,
    val artworkUri: String?,
    val year: Int?,
    val trackCount: Int,
    val totalDurationMs: Long,
)

data class ArtistRow(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val trackCount: Int,
    val isAlbumArtist: Boolean,
)

data class GenreRow(
    val name: String,
    val trackCount: Int,
)

data class FolderCountRow(
    val path: String,
    val count: Int,
)

data class PlaylistRow(
    val id: Long,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val entryCount: Int,
)

/** Cheap per-file snapshot used to find new, changed and removed files without loading full rows. */
data class SyncRow(
    val id: Long,
    val dateModifiedMs: Long,
    val sizeBytes: Long,
    val folderPath: String,
)

data class PendingDetailsRow(
    val id: Long,
    val uri: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val bitrateKbps: Int,
)

data class TrackPathRow(
    val id: Long,
    val folderPath: String,
    val fileName: String,
)
