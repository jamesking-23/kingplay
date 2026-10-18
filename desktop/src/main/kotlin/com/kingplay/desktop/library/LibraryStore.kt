package com.kingplay.desktop.library

import com.kingplay.core.model.Track
import com.kingplay.core.model.backup.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** What is kept between runs: the scanned songs and the folders to scan. One JSON file per kind. */
data class SavedLibrary(val folders: List<String>, val tracks: List<Track>)

class LibraryStore(private val file: File) {

    fun load(): SavedLibrary {
        if (!file.isFile) return SavedLibrary(emptyList(), emptyList())
        return runCatching {
            val root = Json.parse(file.readText(Charsets.UTF_8)) as Map<*, *>
            SavedLibrary(
                folders = (root["folders"] as? List<*>).orEmpty().filterIsInstance<String>(),
                tracks = (root["tracks"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.toTrack() },
            )
        }.getOrElse { SavedLibrary(emptyList(), emptyList()) } // a damaged file means "rescan", never a crash
    }

    /** Writes to a temporary file first so a crash mid-write cannot destroy the old library. */
    fun save(library: SavedLibrary) {
        file.parentFile?.mkdirs()
        val text = Json.write(
            mapOf(
                "version" to VERSION.toLong(),
                "folders" to library.folders,
                "tracks" to library.tracks.map { it.toMap() },
            ),
        )
        val temp = File(file.path + ".tmp")
        temp.writeText(text, Charsets.UTF_8)
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
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
