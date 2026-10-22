package com.kingplay.core.player.logic

/**
 * Decides when a listen counts as a play: after [thresholdFor] milliseconds of actual playback, once per start of
 * an item. Only real elapsed play time is added, so seeking to the end does not count.
 */
class PlayCountTracker(
    private val thresholdFor: (durationMs: Long) -> Long = ::defaultPlayThresholdMs,
) {
    private var currentId: String? = null
    private var playedMs = 0L
    private var counted = false

    /** Call when the current item changes (including to null). Resets progress, even for a repeat of the same id. */
    fun onItemStarted(mediaId: String?) {
        currentId = mediaId
        playedMs = 0
        counted = false
    }

    /** Adds [deltaMs] of play time. Returns the media id exactly once, when the threshold is crossed. */
    fun addPlayed(deltaMs: Long, durationMs: Long): String? {
        val id = currentId ?: return null
        if (counted || deltaMs <= 0) return null
        playedMs += deltaMs
        if (playedMs < thresholdFor(durationMs)) return null
        counted = true
        return id
    }
}

private const val COUNT_AFTER_MS = 3_000L

/** A song counts as played once it has really played for 3 seconds (or its whole length, if it is shorter). */
fun defaultPlayThresholdMs(durationMs: Long): Long =
    if (durationMs <= 0) COUNT_AFTER_MS else minOf(COUNT_AFTER_MS, durationMs)
