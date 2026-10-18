package com.kingplay.desktop.library

import com.kingplay.core.model.Track
import com.kingplay.core.model.UnknownNames
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

val AUDIO_EXTENSIONS = setOf(
    "mp3", "flac", "m4a", "aac", "ogg", "oga", "opus", "wav", "wma", "aiff", "aif", "ape", "wv", "mka",
)

fun isAudioFile(name: String): Boolean = name.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS

/** The same file always gets the same id, so playlists and backups keep working after a rescan. (FNV-1a, 63 bits.) */
fun stableId(text: String): Long {
    var hash = -0x340d631b7bdddcdbL // FNV offset basis
    for (byte in text.toByteArray(Charsets.UTF_8)) {
        hash = (hash xor (byte.toLong() and 0xff)) * 0x100000001b3L
    }
    return hash and Long.MAX_VALUE
}

private fun String.key(): String = trim().lowercase().replace(Regex("\\s+"), " ")

class TrackScanner(
    private val tags: TagReader,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /**
     * Walks [roots] and returns every audio file as a [Track]. Files whose modified time did not change since
     * [previous] are not read again. Folders that cannot be opened are skipped.
     */
    fun scan(
        roots: List<String>,
        previous: Map<String, Track> = emptyMap(),
        onProgress: (found: Int) -> Unit = {},
    ): List<Track> {
        val found = LinkedHashMap<String, Track>()
        for (root in roots.distinct()) {
            val start = Path.of(root)
            if (!Files.isDirectory(start)) continue
            Files.walkFileTree(
                start,
                emptySet<FileVisitOption>(), // never follow links: a loop would run forever
                MAX_DEPTH,
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val name = file.fileName?.toString().orEmpty()
                        if (attrs.isRegularFile && isAudioFile(name)) {
                            val track = trackFor(file.toFile(), attrs.lastModifiedTime().toMillis(), previous)
                            found.putIfAbsent(track.uri, track)
                            onProgress(found.size)
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                        FileVisitResult.CONTINUE
                },
            )
        }
        return found.values.toList()
    }

    private fun trackFor(file: File, modifiedMs: Long, previous: Map<String, Track>): Track {
        val path = file.absolutePath
        previous[path]?.takeIf { it.dateModifiedMs == modifiedMs }?.let { return it }
        val raw = tags.read(file) ?: RawTags()
        val artist = raw.artist ?: UnknownNames.ARTIST
        val album = raw.album ?: UnknownNames.ALBUM
        val albumArtist = raw.albumArtist ?: raw.artist
        return Track(
            id = stableId(path),
            uri = path,
            title = raw.title ?: file.nameWithoutExtension,
            artistId = stableId("artist:" + artist.key()),
            artistName = artist,
            albumId = stableId("album:" + (albumArtist ?: "").key() + "|" + album.key()),
            albumTitle = album,
            albumArtist = raw.albumArtist,
            genre = raw.genre,
            durationMs = raw.durationMs,
            bitrateKbps = raw.bitrateKbps,
            sampleRateHz = raw.sampleRateHz,
            format = file.extension.lowercase(),
            trackNumber = raw.trackNumber,
            discNumber = raw.discNumber,
            year = raw.year,
            folderPath = file.parent.orEmpty(),
            dateAddedMs = previous[path]?.dateAddedMs ?: now(),
            dateModifiedMs = modifiedMs,
            playCount = previous[path]?.playCount ?: 0,
            lastPlayedMs = previous[path]?.lastPlayedMs,
            isFavorite = previous[path]?.isFavorite ?: false,
        )
    }

    private companion object {
        const val MAX_DEPTH = 32
    }
}
