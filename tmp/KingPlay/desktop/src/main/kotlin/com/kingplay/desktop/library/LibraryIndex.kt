package com.kingplay.desktop.library

import com.kingplay.core.model.Album
import com.kingplay.core.model.Artist
import com.kingplay.core.model.Track
import com.kingplay.core.model.UnknownNames

/** Read-only views over a list of tracks: lookup, albums, artists and search. Built once per scan. */
class LibraryIndex(val tracks: List<Track>) {
    private val byId: Map<Long, Track> = tracks.associateBy { it.id }

    operator fun get(id: Long): Track? = byId[id]

    /** The first song of each album, used to look up the album's cover. */
    val firstTrackOfAlbum: Map<Long, Track> by lazy {
        tracks.groupBy { it.albumId }.mapValues { (_, group) -> group.first() }
    }

    val albums: List<Album> by lazy {
        tracks.groupBy { it.albumId }.values.map { group ->
            val first = group.first()
            val artists = group.map { it.albumArtist ?: it.artistName }.distinct()
            Album(
                id = first.albumId,
                title = first.albumTitle,
                artistName = artists.singleOrNull() ?: UnknownNames.VARIOUS_ARTISTS,
                artworkUri = null,
                year = group.mapNotNull { it.year }.maxOrNull(),
                trackCount = group.size,
                totalDurationMs = group.sumOf { it.durationMs },
            )
        }.sortedBy { it.title.lowercase() }
    }

    val artists: List<Artist> by lazy {
        tracks.groupBy { it.artistId }.values.map { group ->
            Artist(
                id = group.first().artistId,
                name = group.first().artistName,
                albumCount = group.map { it.albumId }.distinct().size,
                trackCount = group.size,
                isAlbumArtist = false,
            )
        }.sortedBy { it.name.lowercase() }
    }

    /** Tracks of one album in disc and track order. */
    fun albumTracks(albumId: Long): List<Track> =
        tracks.filter { it.albumId == albumId }
            .sortedWith(compareBy({ it.discNumber }, { it.trackNumber }, { it.title }))

    fun artistTracks(artistId: Long): List<Track> =
        tracks.filter { it.artistId == artistId }
            .sortedWith(compareBy({ it.albumTitle }, { it.discNumber }, { it.trackNumber }))

    /** Every word typed must appear in the title, artist or album (any order, any case). */
    fun search(query: String): List<Track> {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return tracks
        return tracks.filter { track ->
            val haystack = "${track.title} ${track.artistName} ${track.albumTitle}".lowercase()
            words.all { it in haystack }
        }
    }

    companion object {
        val Empty = LibraryIndex(emptyList())
    }
}

fun List<Track>.sortedForList(): List<Track> =
    sortedWith(compareBy({ it.title.lowercase() }, { it.artistName.lowercase() }))
