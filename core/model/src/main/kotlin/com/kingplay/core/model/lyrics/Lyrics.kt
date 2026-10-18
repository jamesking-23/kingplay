package com.kingplay.core.model.lyrics

/** One line of lyrics. [timeMs] is null for lyrics that carry no timing. A blank [text] is a pause between verses. */
data class LyricLine(val timeMs: Long?, val text: String)

/** [synced] lyrics follow the music (every line has a time). Plain lyrics are just text to scroll through. */
data class Lyrics(val lines: List<LyricLine>, val synced: Boolean)

/** What the screen knows about the lyrics of the song that is playing. */
sealed interface LyricsState {
    /** Nothing is playing, or the file is still being read. */
    data object Loading : LyricsState

    data object None : LyricsState

    data class Available(val mediaId: String, val lyrics: Lyrics) : LyricsState
}

private const val MAX_LINES = 5_000
private const val MAX_CHARS = 500_000

private val TIME_TAG = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
private val META_TAG = Regex("""^\[([A-Za-z#]+):(.*)]$""")
private val WORD_TAG = Regex("""<\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?>""")
private val OFFSET = Regex("""^\[offset:\s*([+-]?\d+)\s*]$""", RegexOption.IGNORE_CASE)

/**
 * Reads lyrics text in either form: LRC with `[mm:ss.xx]` timestamps (several per line, an `[offset:]` tag and
 * word-level `<mm:ss.xx>` tags are understood) or plain text. Returns null when there is no actual text.
 */
fun parseLyrics(raw: String): Lyrics? {
    if (raw.isBlank()) return null
    val text = raw.removePrefix("﻿").take(MAX_CHARS).replace("\r\n", "\n").replace('\r', '\n')
    val rows = text.split('\n').take(MAX_LINES)
    var offsetMs = 0L
    val timed = ArrayList<LyricLine>()
    val plain = ArrayList<String>()
    for (row in rows) {
        val line = row.trim()
        val offset = OFFSET.matchEntire(line)
        if (offset != null) {
            offsetMs = offset.groupValues[1].toLongOrNull() ?: 0L
            continue
        }
        if (META_TAG.matches(line) && !TIME_TAG.containsMatchIn(line)) continue
        val stamps = ArrayList<Long>()
        var rest = line
        while (true) {
            val match = TIME_TAG.find(rest)
            if (match == null || match.range.first != 0) break
            stamps += timeOf(match)
            rest = rest.substring(match.range.last + 1).trimStart()
        }
        val content = WORD_TAG.replace(rest, "").trim()
        if (stamps.isNotEmpty()) {
            // A positive offset in the file means the lyrics should appear earlier.
            stamps.forEach { timed += LyricLine((it - offsetMs).coerceAtLeast(0L), content) }
        } else {
            plain += WORD_TAG.replace(line, "").trim()
        }
    }
    if (timed.any { it.text.isNotEmpty() }) {
        return Lyrics(timed.sortedBy { it.timeMs }, synced = true)
    }
    return plainLyrics(plain)
}

private fun plainLyrics(rows: List<String>): Lyrics? {
    val lines = ArrayList<LyricLine>()
    for (row in rows) {
        if (row.isEmpty() && (lines.isEmpty() || lines.last().text.isEmpty())) continue
        lines += LyricLine(null, row)
    }
    while (lines.isNotEmpty() && lines.last().text.isEmpty()) lines.removeAt(lines.lastIndex)
    return if (lines.isEmpty()) null else Lyrics(lines, synced = false)
}

private fun timeOf(match: MatchResult): Long {
    val minutes = match.groupValues[1].toLong()
    val seconds = match.groupValues[2].toLong()
    val fraction = match.groupValues[3]
    val millis = when (fraction.length) {
        0 -> 0L
        1 -> fraction.toLong() * 100
        2 -> fraction.toLong() * 10
        else -> fraction.toLong()
    }
    return minutes * 60_000 + seconds * 1_000 + millis
}

/**
 * Index of the line that should be highlighted at [positionMs]: the last line whose time has passed.
 * Returns -1 before the first line, and always -1 for lyrics without timing.
 */
fun activeLineIndex(lyrics: Lyrics, positionMs: Long): Int {
    if (!lyrics.synced) return -1
    var low = 0
    var high = lyrics.lines.lastIndex
    var found = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        val time = lyrics.lines[mid].timeMs ?: Long.MAX_VALUE
        if (time <= positionMs) {
            found = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return found
}
