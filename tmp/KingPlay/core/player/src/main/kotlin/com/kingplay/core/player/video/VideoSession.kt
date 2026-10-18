package com.kingplay.core.player.video

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.kingplay.core.model.MediaRef
import com.kingplay.core.player.controller.MediaControllerPlaybackController
import com.kingplay.core.player.logic.MediaIds
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

/** A subtitle file the user picked for the video that is playing. */
data class ExternalSubtitle(val uri: String, val mimeType: String, val label: String)

/** Carries an [ExternalSubtitle] from the video screen to the playback service inside a media item. */
object SubtitleExtras {
    private const val URI = "kp_subtitle_uri"
    private const val MIME = "kp_subtitle_mime"
    private const val LABEL = "kp_subtitle_label"

    fun write(subtitle: ExternalSubtitle): Bundle = Bundle().apply {
        putString(URI, subtitle.uri)
        putString(MIME, subtitle.mimeType)
        putString(LABEL, subtitle.label)
    }

    fun read(item: MediaItem): ExternalSubtitle? {
        val extras = item.requestMetadata.extras ?: return null
        val uri = extras.getString(URI) ?: return null
        val mime = extras.getString(MIME) ?: return null
        return ExternalSubtitle(uri, mime, extras.getString(LABEL).orEmpty())
    }
}

/**
 * What the video screen needs from the playback service: the player to draw and control, and two operations that
 * plain [Player] calls cannot express (start a queue at a position, attach a subtitle file).
 */
@Singleton
class VideoSession @Inject constructor(
    private val controller: MediaControllerPlaybackController,
) {
    /** The connected player; null until the service is up. */
    val player: StateFlow<Player?> get() = controller.player

    fun connect() = controller.connect()

    /** Plays [videos] starting at [index], [startPositionMs] into that video. Replaces whatever was queued. */
    fun start(videos: List<Long>, index: Int, startPositionMs: Long) {
        if (videos.isEmpty()) return
        controller.withController { c ->
            val items = videos.map { MediaItem.Builder().setMediaId(MediaIds.of(MediaRef.Video(it))).build() }
            c.setMediaItems(items, index.coerceIn(0, items.lastIndex), startPositionMs)
            c.prepare()
            c.play()
        }
    }

    /** Reloads the current video with [subtitle] added, keeping the position. */
    fun attachSubtitle(subtitle: ExternalSubtitle) {
        controller.withController { c ->
            val current = c.currentMediaItem ?: return@withController
            val index = c.currentMediaItemIndex
            val position = c.currentPosition
            val wasPlaying = c.playWhenReady
            val reloaded = MediaItem.Builder()
                .setMediaId(current.mediaId)
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder().setExtras(SubtitleExtras.write(subtitle)).build(),
                )
                .build()
            c.replaceMediaItem(index, reloaded)
            c.seekTo(index, position)
            c.playWhenReady = wasPlaying
        }
    }
}
