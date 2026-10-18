package com.kingplay.data.library.scan

import com.kingplay.core.model.UnknownNames
import java.util.Locale

private val WHITESPACE = Regex("\\s+")
private val LEADING_ARTICLES = listOf("the ", "a ", "an ")

/** Stable 64-bit ids for artists and albums, derived from names so they survive rescans and MediaStore resets. */
object StableIds {
    private const val FNV_OFFSET = -3750763034362895579L // 0xcbf29ce484222325
    private const val FNV_PRIME = 1099511628211L

    fun artist(name: String): Long = hash("artist", name)

    /** [groupKey] is the album artist, or a folder key when the file has no album artist (see [albumId]). */
    fun album(title: String, groupKey: String): Long = hash("album", title, groupKey)

    internal fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT).replace(WHITESPACE, " ")

    private fun hash(vararg parts: String): Long {
        var h = FNV_OFFSET
        for (part in parts) {
            for (b in normalize(part).toByteArray(Charsets.UTF_8)) {
                h = (h xor (b.toLong() and 0xff)) * FNV_PRIME
            }
            h = (h xor SEPARATOR) * FNV_PRIME
        }
        val positive = h and Long.MAX_VALUE
        return if (positive == 0L) 1L else positive
    }

    private const val SEPARATOR = 0x1fL
}

/** Lower-cased title without a leading article, so "The Wall" sorts under W. */
fun titleSortKey(title: String): String {
    val lower = title.trim().lowercase(Locale.ROOT)
    for (article in LEADING_ARTICLES) {
        if (lower.startsWith(article) && lower.length > article.length) {
            return lower.substring(article.length).trimStart()
        }
    }
    return lower
}

/** MediaStore reports missing tags as `<unknown>`. Blank and unknown values collapse to the shared placeholder. */
fun resolveArtist(raw: String?): String = raw.cleanTag() ?: UnknownNames.ARTIST

fun resolveAlbum(raw: String?): String = raw.cleanTag() ?: UnknownNames.ALBUM

fun String?.cleanTag(): String? {
    val trimmed = this?.trim().orEmpty()
    return if (trimmed.isEmpty() || trimmed.equals("<unknown>", ignoreCase = true)) null else trimmed
}

fun titleOrFileName(title: String?, fileName: String): String =
    title.cleanTag() ?: fileName.substringBeforeLast('.', fileName).ifBlank { fileName }

/** Upper-case extension ("FLAC", "MP3"), falling back to the MIME subtype. */
fun audioFormat(fileName: String, mimeType: String?): String {
    val ext = fileName.substringAfterLast('.', "").trim()
    if (ext.isNotEmpty() && ext.length <= MAX_EXTENSION) return ext.uppercase(Locale.ROOT)
    val sub = mimeType?.substringAfter('/', "")?.removePrefix("x-")?.trim().orEmpty()
    return if (sub.isEmpty()) "UNKNOWN" else sub.uppercase(Locale.ROOT)
}

/** Lower-case container name for video ("mp4", "mkv"). */
fun videoContainer(fileName: String, mimeType: String?): String =
    audioFormat(fileName, mimeType).lowercase(Locale.ROOT)

private const val MAX_EXTENSION = 5

/** MediaStore packs disc and track as `disc * 1000 + track`. Returns disc to track. */
fun decodeLegacyTrackNumber(raw: Int): Pair<Int, Int> =
    if (raw >= LEGACY_DISC_FACTOR) {
        (raw / LEGACY_DISC_FACTOR) to (raw % LEGACY_DISC_FACTOR)
    } else {
        1 to raw.coerceAtLeast(0)
    }

private const val LEGACY_DISC_FACTOR = 1000

/** Parses the leading number of tags like "3/12". Returns null when there is none. */
fun parseLeadingInt(raw: String?): Int? {
    val digits = raw?.trim()?.takeWhile { it.isDigit() }.orEmpty()
    return digits.toIntOrNull()
}

/** Average bitrate from file size, used when no decoder reports one. Bits per millisecond equals kbit/s. */
fun estimateBitrateKbps(sizeBytes: Long, durationMs: Long): Int =
    if (durationMs <= 0 || sizeBytes <= 0) {
        0
    } else {
        ((sizeBytes * 8) / durationMs).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

/** True when [folderPath] is an excluded folder or sits inside one. */
fun isExcluded(folderPath: String, excluded: Collection<String>): Boolean {
    if (excluded.isEmpty()) return false
    val folder = folderPath.trimEnd('/')
    return excluded.any { raw ->
        val ex = raw.trimEnd('/')
        ex.isNotEmpty() && (folder == ex || folder.startsWith("$ex/"))
    }
}

/** Folder of a file from its absolute path, or from MediaStore's relative path when the absolute one is hidden. */
fun resolveFolder(absolutePath: String?, relativePath: String?): String {
    val abs = absolutePath?.trim().orEmpty()
    if (abs.isNotEmpty()) return abs.substringBeforeLast('/', "").ifEmpty { "/" }
    val rel = relativePath?.trim().orEmpty().trim('/')
    return if (rel.isEmpty()) "" else "/$rel"
}

private const val FOLDER_KEY_PREFIX = "folder:"

/**
 * Album identity. With an album-artist tag that is the grouping key; without one the folder is used, so a
 * compilation folder stays one album even though its tracks have different artists, while two unrelated albums
 * with the same title in different folders stay apart.
 */
fun albumId(albumTitle: String, albumArtist: String?, folderPath: String): Long =
    StableIds.album(albumTitle, albumArtist ?: (FOLDER_KEY_PREFIX + folderPath))
