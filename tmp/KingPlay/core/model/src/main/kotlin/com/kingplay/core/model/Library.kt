package com.kingplay.core.model

/** Placeholder names stored when a file has no artist or album tag. The UI maps these to localized strings. */
object UnknownNames {
    const val ARTIST = "Unknown artist"
    const val ALBUM = "Unknown album"

    /** Shown as the artist of an album whose tracks have different artists and no album-artist tag. */
    const val VARIOUS_ARTISTS = "Various artists"
}

enum class SongSortField { TITLE, ARTIST, ALBUM, DATE_ADDED, DURATION, PLAY_COUNT }

data class SongSort(
    val field: SongSortField = SongSortField.TITLE,
    val ascending: Boolean = true,
)

/** Library filters. A default instance matches everything. */
data class SongFilter(
    val format: String? = null,
    val minBitrateKbps: Int = 0,
    val minDurationMs: Long = 0,
    val maxDurationMs: Long = Long.MAX_VALUE,
) {
    companion object {
        val None = SongFilter()
    }
}

enum class VideoSortField { TITLE, DATE_ADDED, DURATION, SIZE }

data class VideoSort(
    val field: VideoSortField = VideoSortField.TITLE,
    val ascending: Boolean = true,
)

data class Genre(val name: String, val trackCount: Int)

/** A folder that contains audio and/or video. [path] is an absolute directory path. */
data class MediaFolder(
    val path: String,
    val audioCount: Int,
    val videoCount: Int,
) {
    val name: String get() = path.trimEnd('/').substringAfterLast('/')
}

data class SearchResults(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val videos: List<VideoItem> = emptyList(),
) {
    val isEmpty: Boolean get() = tracks.isEmpty() && albums.isEmpty() && artists.isEmpty() && videos.isEmpty()

    companion object {
        val Empty = SearchResults()
    }
}

data class PlaylistImportResult(
    val playlistId: Long,
    val matched: Int,
    val total: Int,
)
