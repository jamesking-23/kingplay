package com.kingplay.core.player.engine

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.repository.NowPlayingPublisher
import com.kingplay.core.model.repository.NowPlayingSnapshot
import com.kingplay.core.player.logic.MediaIds
import dagger.hilt.android.scopes.ServiceScoped
import java.util.Optional
import javax.inject.Inject

/** Tells the widget (when the app has one) which song is playing and whether it is playing. */
@ServiceScoped
class NowPlayingBinder @Inject constructor(
    private val player: Player,
    private val publisher: Optional<NowPlayingPublisher>,
) {
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (
                events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_MEDIA_METADATA_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_TIMELINE_CHANGED,
                )
            ) {
                publish()
            }
        }
    }

    fun start() {
        player.addListener(listener)
        publish()
    }

    fun stop() {
        player.removeListener(listener)
        publisher.orElse(null)?.publish(NowPlayingSnapshot.Idle)
    }

    private fun publish() {
        val target = publisher.orElse(null) ?: return
        target.publish(snapshot(player.currentMediaItem))
    }

    private fun snapshot(item: MediaItem?): NowPlayingSnapshot {
        val isMusic = MediaIds.parse(item?.mediaId) is MediaRef.Audio
        if (item == null || !isMusic) return NowPlayingSnapshot.Idle
        val meta = item.mediaMetadata
        return NowPlayingSnapshot(
            mediaId = item.mediaId,
            title = meta.title?.toString().orEmpty(),
            artist = meta.artist?.toString().orEmpty(),
            // "Playing" for a widget means the play button would show pause: also true while buffering.
            isPlaying = player.playWhenReady && player.playbackState != Player.STATE_ENDED,
            artworkSource = item.localConfiguration?.uri?.toString(),
        )
    }
}
