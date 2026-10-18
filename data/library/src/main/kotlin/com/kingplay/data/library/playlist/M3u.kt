package com.kingplay.data.library.playlist

import java.net.URLDecoder
import java.util.Locale

data class M3uEntry(
    val path: String,
    val durationSec: Int,
    val title: String,
)

object M3uWriter {
    fun write(entries: List<M3uEntry>): String = buildString {
        append("#EXTM3U\n")
        for (entry in entries) {
            append("#EXTINF:").append(entry.durationSec).append(',').append(entry.title.replace('\n', ' ')).append('\n')
            append(entry.path).append('\n')
        }
    }
}

object M3uParser {
    /** Returns the file paths in order. Comments, directives and blank lines are dropped. */
    fun parse(text: String): List<String> =
        text.removePrefix("﻿")
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map(::normalizePath)
            .toList()

    internal fun normalizePath(raw: String): String {
        var path = raw
        if (path.startsWith("file://", ignoreCase = true)) {
            path = path.substring("file://".length)
            path = runCatching { URLDecoder.decode(path.replace("+", "%2B"), "UTF-8") }.getOrDefault(path)
        }
        return path.replace('\\', '/')
    }
}

data class PathRow(
    val id: Long,
    val folderPath: String,
    val fileName: String,
)

/**
 * Finds the library track a playlist path refers to. Playlists often come from another device, so exact paths
 * rarely match; it falls back to progressively looser keys and only accepts a loose match when it is unambiguous.
 */
class TrackPathIndex(rows: List<PathRow>) {
    private val exact = HashMap<String, Long>(rows.size)
    private val exactLower = HashMap<String, Long>(rows.size)
    private val byParentAndName = HashMap<String, MutableSet<Long>>()
    private val byName = HashMap<String, MutableSet<Long>>()

    init {
        for (row in rows) {
            val folder = row.folderPath.trimEnd('/')
            val full = "$folder/${row.fileName}"
            exact.putIfAbsent(full, row.id)
            exactLower.putIfAbsent(full.lowercase(Locale.ROOT), row.id)
            val parent = folder.substringAfterLast('/')
            byParentAndName.getOrPut("$parent/${row.fileName}".lowercase(Locale.ROOT)) { linkedSetOf() }.add(row.id)
            byName.getOrPut(row.fileName.lowercase(Locale.ROOT)) { linkedSetOf() }.add(row.id)
        }
    }

    fun match(rawPath: String): Long? {
        val path = M3uParser.normalizePath(rawPath.trim())
        exact[path]?.let { return it }
        exactLower[path.lowercase(Locale.ROOT)]?.let { return it }
        val segments = path.split('/').filter { it.isNotEmpty() }
        val name = segments.lastOrNull() ?: return null
        if (segments.size >= 2) {
            val key = "${segments[segments.size - 2]}/$name".lowercase(Locale.ROOT)
            byParentAndName[key]?.singleOrNull()?.let { return it }
        }
        return byName[name.lowercase(Locale.ROOT)]?.singleOrNull()
    }
}
