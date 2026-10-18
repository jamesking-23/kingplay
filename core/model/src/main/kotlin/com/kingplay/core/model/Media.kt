package com.kingplay.core.model

/** Unified reference to anything playable. Queues and playlists hold these. */
sealed interface MediaRef {
    val id: Long

    data class Audio(override val id: Long) : MediaRef

    data class Video(override val id: Long) : MediaRef
}

data class ReplayGain(
    val trackGainDb: Float?,
    val albumGainDb: Float?,
    val trackPeak: Float?,
    val albumPeak: Float?,
)

data class Track(
    val id: Long,
    val uri: String,
    val title: String,
    val artistId: Long,
    val artistName: String,
    val albumId: Long,
    val albumTitle: String,
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
    val dateAddedMs: Long,
    val dateModifiedMs: Long,
    val replayGain: ReplayGain? = null,
    val playCount: Int = 0,
    val lastPlayedMs: Long? = null,
    val isFavorite: Boolean = false,
)

data class Album(
    val id: Long,
    val title: String,
    val artistName: String,
    val artworkUri: String?,
    val year: Int?,
    val trackCount: Int,
    val totalDurationMs: Long,
)

data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val trackCount: Int,
    val isAlbumArtist: Boolean,
)

data class SubtitleTrack(
    val id: String,
    val language: String?,
    val label: String?,
    val uri: String?,
    val isEmbedded: Boolean,
)

data class VideoItem(
    val id: Long,
    val uri: String,
    val title: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val videoCodec: String?,
    val container: String,
    val folderPath: String,
    val sizeBytes: Long,
    val dateAddedMs: Long,
    val resumePositionMs: Long = 0,
    val lastPlayedMs: Long? = null,
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
)

data class Playlist(
    val id: Long,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val entryCount: Int,
)

data class PlaylistEntry(
    val playlistId: Long,
    val position: Int,
    val ref: MediaRef,
)
