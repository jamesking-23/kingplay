package com.kingplay.desktop.lyrics

import com.kingplay.core.model.Track
import com.kingplay.core.model.lyrics.Lyrics
import com.kingplay.core.model.lyrics.lyricsKey
import com.kingplay.core.model.lyrics.parseLyrics
import java.io.File

/** Lyrics files larger than this are not read; real lyrics are a few kilobytes. */
const val MAX_LYRICS_BYTES = 1_000_000L

/** Turns file bytes into text: honours a UTF-16 or UTF-8 byte-order mark, otherwise reads UTF-8. */
fun decodeLyricsBytes(bytes: ByteArray): String = when {
    bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
        String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
    bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
        String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
    bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() ->
        String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
    else -> String(bytes, Charsets.UTF_8)
}

/** The `.lrc` file that sits next to a song and has the same name (any letter case). */
fun findSidecarLrc(song: File): File? {
    val wanted = song.nameWithoutExtension + ".lrc"
    val files = song.absoluteFile.parentFile?.listFiles() ?: return null
    return files.firstOrNull {
        it.isFile && it.name.equals(wanted, ignoreCase = true) && it.length() in 1..MAX_LYRICS_BYTES
    }
}

fun readSidecarLrc(song: File): String? =
    findSidecarLrc(song)?.let { runCatching { decodeLyricsBytes(it.readBytes()) }.getOrNull() }

/** Where lyrics can come from; the order of the checks is the order of preference. */
class LyricsResolver(
    private val readSidecar: (Track) -> String?,
    private val readEmbedded: (Track) -> String?,
) {
    /** [imported] are the lyrics the person added themselves, by [lyricsKey]. Returns null when there are none. */
    fun resolve(track: Track, imported: Map<String, String>): Lyrics? {
        imported[keyOf(track)]?.let { parseLyrics(it) }?.let { return it }
        readSidecar(track)?.let { parseLyrics(it) }?.let { return it }
        return readEmbedded(track)?.let { parseLyrics(it) }
    }

    companion object {
        fun keyOf(track: Track): String = lyricsKey(track.artistName, track.title)
    }
}
