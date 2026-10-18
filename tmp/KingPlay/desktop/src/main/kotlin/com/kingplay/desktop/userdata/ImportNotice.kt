package com.kingplay.desktop.userdata

/** The one-line result shown after importing a backup. Extras are mentioned only when something came across. */
fun importNotice(s: ImportSummary): String {
    val parts = mutableListOf(
        "Added ${s.playlistsAdded} playlists (${s.playlistsSkipped} already existed). " +
            "Found ${s.tracksMatched} of ${s.tracksTotal} playlist songs and " +
            "${s.favoritesMatched} of ${s.favoritesTotal} favorites.",
    )
    if (s.presetsAdded > 0) parts += "Added ${s.presetsAdded} equalizer presets."
    if (s.lyricsAdded > 0) parts += "Added lyrics for ${s.lyricsAdded} songs."
    if (s.statsMatched > 0) parts += "Restored play counts for ${s.statsMatched} songs."
    if (s.videosMatched > 0) parts += "Restored where you stopped in ${s.videosMatched} videos."
    return parts.joinToString(" ")
}
