package com.kingplay.desktop.library

import com.kingplay.core.model.Track
import java.io.File

/** What is kept between runs: the scanned songs and the folders to scan. One JSON file per kind. */
data class SavedLibrary(val folders: List<String>, val tracks: List<Track>)

class LibraryStore(private val file: File) {

    fun load(): SavedLibrary {
        val root = JsonFile.read(file) ?: return SavedLibrary(emptyList(), emptyList())
        return SavedLibrary(
            folders = (root["folders"] as? List<*>).orEmpty().filterIsInstance<String>(),
            tracks = (root["tracks"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.toTrack() },
        ) // a damaged file means "rescan", never a crash
    }

    fun save(library: SavedLibrary) {
        JsonFile.write(
            file,
            mapOf(
                "version" to VERSION.toLong(),
                "folders" to library.folders,
                "tracks" to library.tracks.map { it.toMap() },
            ),
        )
    }

    private companion object {
        const val VERSION = 1
    }
}

internal fun Track.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "uri" to uri, "title" to title,
    "artistId" to artistId, "artistName" to artistName,
    "albumId" to albumId, "albumTitle" to albumTitle, "albumArtist" to albumArtist,
    "genre" to genre, "durationMs" to durationMs, "bitrateKbps" to bitrateKbps.toLong(),
    "sampleRateHz" to sampleRateHz.toLong(), "format" to format,
    "trackNumber" to trackNumber.toLong(), "discNumber" to discNumber.toLong(), "year" to year?.toLong(),
    "folderPath" to folderPath, "dateAddedMs" to dateAddedMs, "dateModifiedMs" to dateModifiedMs,
    "playCount" to playCount.toLong(), "lastPlayedMs" to lastPlayedMs, "isFavorite" to isFavorite,
)

private fun Map<*, *>.long(key: String): Long? = (this[key] as? Number)?.toLong()

private fun Map<*, *>.int(key: String): Int? = long(key)?.toInt()

private fun Map<*, *>.string(key: String): String? = this[key] as? String

internal fun Map<*, *>.toTrack(): Track? {
    val id = long("id") ?: return null
    val uri = string("uri") ?: return null
    return Track(
        id = id,
        uri = uri,
        title = string("title") ?: File(uri).nameWithoutExtension,
        artistId = long("artistId") ?: 0,
        artistName = string("artistName").orEmpty(),
        albumId = long("albumId") ?: 0,
        albumTitle = string("albumTitle").orEmpty(),
        albumArtist = string("albumArtist"),
        genre = string("genre"),
        durationMs = long("durationMs") ?: 0,
        bitrateKbps = int("bitrateKbps") ?: 0,
        sampleRateHz = int("sampleRateHz") ?: 0,
        format = string("format").orEmpty(),
        trackNumber = int("trackNumber") ?: 0,
        discNumber = int("discNumber") ?: 0,
        year = int("year"),
        folderPath = string("folderPath").orEmpty(),
        dateAddedMs = long("dateAddedMs") ?: 0,
        dateModifiedMs = long("dateModifiedMs") ?: 0,
        playCount = int("playCount") ?: 0,
        lastPlayedMs = long("lastPlayedMs"),
        isFavorite = this["isFavorite"] as? Boolean ?: false,
    )
}
