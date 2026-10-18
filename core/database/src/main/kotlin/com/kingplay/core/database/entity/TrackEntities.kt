package com.kingplay.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One audio file known to the library. [id] is the MediaStore id, so it is also what playlists and the
 * queue refer to. Everything the user changes (favorites, play counts) lives in [TrackUserDataEntity] so a
 * rescan can replace rows here without losing it.
 */
@Entity(
    tableName = "tracks",
    indices = [
        Index("albumId"),
        Index("artistId"),
        Index("albumArtistId"),
        Index("folderPath"),
        Index("genre"),
        Index("dateAddedMs"),
        Index("titleSort"),
        Index("needsDetails"),
    ],
)
data class TrackEntity(
    @PrimaryKey val id: Long,
    val uri: String,
    val title: String,
    /** Lower-cased title without a leading article, used for sorting. */
    val titleSort: String,
    val artistId: Long,
    val artistName: String,
    val albumId: Long,
    val albumTitle: String,
    val albumArtistId: Long?,
    val albumArtist: String?,
    val genre: String?,
    val durationMs: Long,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val format: String,
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int?,
    val folderPath: String,
    val fileName: String,
    val sizeBytes: Long,
    val dateAddedMs: Long,
    val dateModifiedMs: Long,
    /** True until the background pass has read the real sample rate and bitrate. */
    val needsDetails: Boolean,
    val rgTrackGainDb: Float?,
    val rgAlbumGainDb: Float?,
    val rgTrackPeak: Float?,
    val rgAlbumPeak: Float?,
)

/** User-owned data for a track. Deliberately has no foreign key so rescans never cascade into it. */
@Entity(tableName = "track_user_data")
data class TrackUserDataEntity(
    @PrimaryKey val trackId: Long,
    val playCount: Int,
    val lastPlayedMs: Long?,
    val isFavorite: Boolean,
)

/** Full-text index over [TrackEntity]. Room keeps it in sync with triggers. */
@Fts4(contentEntity = TrackEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "tracks_fts")
data class TrackFtsEntity(
    val title: String,
    val artistName: String,
    val albumTitle: String,
    val genre: String?,
)
