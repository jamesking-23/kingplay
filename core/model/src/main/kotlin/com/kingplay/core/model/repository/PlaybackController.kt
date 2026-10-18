package com.kingplay.core.model.repository

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.RepeatMode
import kotlinx.coroutines.flow.StateFlow

/** What the UI needs to know about playback. Built from the player; never holds Media3 types. */
data class PlaybackState(
    /** False until the app has connected to the playback service. Commands sent earlier are queued. */
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val queue: List<MediaRef> = emptyList(),
    val currentIndex: Int = -1,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val error: String? = null,
) {
    val current: MediaRef? get() = queue.getOrNull(currentIndex)
    val hasMedia: Boolean get() = current != null
}

/** The single entry point for controlling playback. Implemented in :core:player on top of a Media3 session. */
interface PlaybackController {
    val state: StateFlow<PlaybackState>

    /** Binds to the playback service (starting it if needed) and restores the saved queue. Safe to call often. */
    fun connect()

    /** Replaces the queue and starts playing at [startIndex]. */
    fun playMedia(refs: List<MediaRef>, startIndex: Int = 0)

    fun play()

    fun pause()

    fun togglePlayPause()

    fun skipToNext()

    /** Restarts the current item when it has played for a few seconds, otherwise goes to the previous one. */
    fun skipToPrevious()

    fun seekTo(positionMs: Long)

    fun skipToQueueIndex(index: Int)

    /** Inserts right after the current item. */
    fun playNext(refs: List<MediaRef>)

    fun addToQueue(refs: List<MediaRef>)

    fun removeFromQueue(index: Int)

    fun moveInQueue(from: Int, to: Int)

    fun clearQueue()

    fun setShuffleEnabled(enabled: Boolean)

    fun setRepeatMode(mode: RepeatMode)
}
