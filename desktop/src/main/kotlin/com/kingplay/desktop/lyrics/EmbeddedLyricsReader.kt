package com.kingplay.desktop.lyrics

import java.io.File
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey

/** Lyrics stored inside the song's tags (the ID3 USLT frame, Vorbis LYRICS and similar). Null when absent. */
fun readEmbeddedLyrics(file: File): String? = runCatching {
    AudioFileIO.read(file).tag?.getFirst(FieldKey.LYRICS)?.takeIf { it.isNotBlank() }
}.getOrNull()
