package com.kingplay.desktop.library

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

/** Reads tags with jaudiotagger (mp3, flac, m4a, ogg, wav and more). Unreadable files return null. */
class JAudioTaggerReader : TagReader {
    init {
        // jaudiotagger logs every oddity at INFO; that floods the console on a big library.
        Logger.getLogger("org.jaudiotagger").level = Level.OFF
    }

    override fun read(file: File): RawTags? = runCatching {
        val audio = AudioFileIO.read(file)
        val header = audio.audioHeader
        val tag: Tag? = audio.tag
        RawTags(
            title = tag?.firstOrNull(FieldKey.TITLE),
            artist = tag?.firstOrNull(FieldKey.ARTIST),
            album = tag?.firstOrNull(FieldKey.ALBUM),
            albumArtist = tag?.firstOrNull(FieldKey.ALBUM_ARTIST),
            genre = tag?.firstOrNull(FieldKey.GENRE),
            trackNumber = parseNumberPart(tag?.firstOrNull(FieldKey.TRACK)),
            discNumber = parseNumberPart(tag?.firstOrNull(FieldKey.DISC_NO)),
            year = parseYear(tag?.firstOrNull(FieldKey.YEAR)),
            durationMs = header.preciseTrackLength.times(MS_PER_SECOND).toLong(),
            bitrateKbps = header.bitRateAsNumber.toInt(),
            sampleRateHz = header.sampleRateAsNumber,
        )
    }.getOrNull()

    private fun Tag.firstOrNull(key: FieldKey): String? =
        runCatching { getFirst(key) }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        const val MS_PER_SECOND = 1000.0
    }
}
