package com.kingplay.core.model.repository

/** What a home-screen widget (or any small surface outside the app) needs to show about the music playing now. */
data class NowPlayingSnapshot(
    val mediaId: String?,
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    /** Where the cover can be loaded from: the song's own content URI (the cover is read out of the file). */
    val artworkSource: String?,
) {
    val isIdle: Boolean get() = mediaId == null

    companion object {
        val Idle = NowPlayingSnapshot(null, "", "", false, null)
    }
}

/** Receives a new snapshot whenever the song or the play state changes. Implemented by the widget module. */
interface NowPlayingPublisher {
    fun publish(snapshot: NowPlayingSnapshot)
}
