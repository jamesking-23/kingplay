package com.kingplay.core.player.lyrics

import com.kingplay.core.model.lyrics.Lyrics
import com.kingplay.core.model.lyrics.parseLyrics
import java.nio.charset.Charset
import java.util.Locale

private const val ENC_LATIN1 = 0
private const val ENC_UTF16_BOM = 1
private const val ENC_UTF16_BE = 2
private const val ENC_UTF8 = 3
private const val TIME_FORMAT_MS = 2
private const val LANGUAGE_BYTES = 3

private val LYRICS_KEYS = setOf("uslt", "sylt", "lyrics", "unsyncedlyrics", "syncedlyrics", "lyric")

/**
 * Picks the best lyrics out of a file's tags. [tags] are name/value pairs from any container; only names that mean
 * lyrics are looked at. Timed lyrics win over plain ones; between equals the longer text wins.
 */
fun pickLyrics(tags: List<Pair<String, String>>): Lyrics? = tags
    .filter { (name, _) -> name.trim().lowercase(Locale.ROOT) in LYRICS_KEYS }
    .mapNotNull { (_, value) -> parseLyrics(value) }
    .maxWithOrNull(compareBy<Lyrics> { it.synced }.thenBy { it.lines.sumOf { line -> line.text.length } })

/** Text of an ID3v2 USLT (unsynchronised lyrics) frame body, or null when it is malformed. */
fun parseUslt(data: ByteArray): String? {
    if (data.size < 1 + LANGUAGE_BYTES) return null
    val encoding = data[0].toInt()
    val charset = charsetOf(encoding) ?: return null
    val start = skipTerminated(data, 1 + LANGUAGE_BYTES, encoding) ?: return null
    return String(data, start, data.size - start, charset).trimEnd('\u0000')
}

/**
 * Turns an ID3v2 SYLT (synchronised lyrics) frame body into LRC text. Only the millisecond time format is read;
 * frame-based timing needs the file's frame rate and is skipped.
 */
fun syltToLrc(data: ByteArray): String? {
    val header = 1 + LANGUAGE_BYTES + 1 + 1
    if (data.size < header) return null
    val encoding = data[0].toInt()
    val charset = charsetOf(encoding) ?: return null
    if (data[1 + LANGUAGE_BYTES].toInt() != TIME_FORMAT_MS) return null
    var pos = skipTerminated(data, header, encoding) ?: return null
    val unit = if (encoding == ENC_UTF16_BOM || encoding == ENC_UTF16_BE) 2 else 1
    val out = StringBuilder()
    while (pos < data.size) {
        var end = pos
        while (end + unit <= data.size && !isTerminator(data, end, unit)) end += unit
        if (end + unit + 4 > data.size) break
        val text = String(data, pos, end - pos, charset).trim('\n', '\r')
        val time = ((data[end + unit].toLong() and 0xFF) shl 24) or ((data[end + unit + 1].toLong() and 0xFF) shl 16) or
            ((data[end + unit + 2].toLong() and 0xFF) shl 8) or (data[end + unit + 3].toLong() and 0xFF)
        out.append(lrcStamp(time)).append(text).append('\n')
        pos = end + unit + 4
    }
    return out.toString().ifBlank { null }
}

internal fun lrcStamp(ms: Long): String {
    val minutes = ms / 60_000
    val seconds = ms / 1_000 % 60
    val hundredths = ms % 1_000 / 10
    return String.format(Locale.ROOT, "[%02d:%02d.%02d]", minutes, seconds, hundredths)
}

private fun charsetOf(encoding: Int): Charset? = when (encoding) {
    ENC_LATIN1 -> Charsets.ISO_8859_1
    ENC_UTF16_BOM -> Charsets.UTF_16
    ENC_UTF16_BE -> Charsets.UTF_16BE
    ENC_UTF8 -> Charsets.UTF_8
    else -> null
}

private fun isTerminator(data: ByteArray, at: Int, unit: Int): Boolean =
    (0 until unit).all { data[at + it].toInt() == 0 }

/** Index just after the terminated descriptor that starts at [from], or null if it never ends. */
private fun skipTerminated(data: ByteArray, from: Int, encoding: Int): Int? {
    val unit = if (encoding == ENC_UTF16_BOM || encoding == ENC_UTF16_BE) 2 else 1
    var pos = from
    while (pos + unit <= data.size) {
        if (isTerminator(data, pos, unit)) return pos + unit
        pos += unit
    }
    return null
}
