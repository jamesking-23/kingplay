package com.kingplay.feature.nowplaying.logic

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.RepeatMode
import com.kingplay.core.model.Track
import com.kingplay.core.model.VideoItem

/** One row of the queue sheet. A null [title] means the file is no longer in the library. */
data class QueueItem(
    val ref: MediaRef,
    val title: String?,
    val subtitle: String?,
    val artworkUri: String?,
    val artworkKey: String,
    val isVideo: Boolean,
)

fun buildQueueItems(
    queue: List<MediaRef>,
    tracks: Map<Long, Track>,
    videos: Map<Long, VideoItem>,
): List<QueueItem> = queue.map { ref ->
    when (ref) {
        is MediaRef.Audio -> tracks[ref.id]?.let {
            QueueItem(ref, it.title, it.artistName, it.uri, "album:${it.albumId}", isVideo = false)
        } ?: QueueItem(ref, null, null, null, "", isVideo = false)
        is MediaRef.Video -> videos[ref.id]?.let {
            QueueItem(ref, it.title, null, null, "", isVideo = true)
        } ?: QueueItem(ref, null, null, null, "", isVideo = true)
    }
}

/** Off -> all -> one -> off, like every music app. */
fun nextRepeatMode(mode: RepeatMode): RepeatMode = when (mode) {
    RepeatMode.OFF -> RepeatMode.ALL
    RepeatMode.ALL -> RepeatMode.ONE
    RepeatMode.ONE -> RepeatMode.OFF
}

/** Position in milliseconds for a slider value in 0..1. Unknown durations map to 0. */
fun seekTargetMs(fraction: Float, durationMs: Long): Long =
    if (durationMs <= 0) 0 else (fraction.coerceIn(0f, 1f) * durationMs).toLong()
