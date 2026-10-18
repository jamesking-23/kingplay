package com.kingplay.desktop.library

import java.io.File

/** What a tag reader found in one file. Any field can be missing; the scanner fills in sensible defaults. */
data class RawTags(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val year: Int? = null,
    val durationMs: Long = 0,
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 0,
)

fun interface TagReader {
    /** Returns null when the file cannot be read as audio at all. */
    fun read(file: File): RawTags?
}

/** "3/12" -> 3, "07" -> 7, junk -> 0. */
fun parseNumberPart(text: String?): Int =
    text?.substringBefore('/')?.trim()?.toIntOrNull()?.coerceAtLeast(0) ?: 0

/** "2019-05-03" -> 2019, "2019" -> 2019, junk or out of range -> null. */
fun parseYear(text: String?): Int? =
    text?.trim()?.take(4)?.toIntOrNull()?.takeIf { it in 1000..2999 }
