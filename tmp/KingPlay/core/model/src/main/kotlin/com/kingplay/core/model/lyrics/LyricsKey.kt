package com.kingplay.core.model.lyrics

import java.util.Locale

/**
 * Identifies a song for saved lyrics by artist and title rather than by library id, because ids change when Android
 * rebuilds its media index and the key has to survive that (and a backup restored on another phone).
 */
fun lyricsKey(artist: String, title: String): String = "${squash(artist)}|${squash(title)}"

private fun squash(text: String): String =
    text.lowercase(Locale.ROOT).trim().replace(Regex("\\s+"), " ")
