package com.kingplay.core.model.update

/**
 * The "What's new" list shown once after an update. Each release replaces [highlights]; the pop-up shows it only
 * when the installed version differs from the last one the person has seen, and never on a fresh install.
 */
object WhatsNew {
    val highlights: List<String> = listOf(
        "Playlists come right after Songs, and a new playlist opens ready for you to add songs to it.",
        "Add songs to any playlist from inside it, with search.",
        "The queue is one tap away from the mini player.",
        "Search has Play all and Shuffle for everything that matches.",
        "Long-press a song to add it to a playlist, share it or delete it.",
        "The app's colours follow the cover art of the song that is playing.",
        "Bigger default text, and Fredoka as the starting font.",
        "Five designs, each with its own look for lists, tabs and the mini player.",
    )

    /** True when the pop-up should appear: an update happened, not a first run or a development build. */
    fun shouldShow(lastSeen: String, installed: String): Boolean =
        lastSeen.isNotBlank() && installed.isNotBlank() && installed != "dev" && lastSeen != installed
}
