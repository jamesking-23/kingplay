package com.kingplay.core.player.engine

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.lyrics.LyricsState
import com.kingplay.core.player.logic.MediaIds
import com.kingplay.core.player.lyrics.parseUslt
import com.kingplay.core.player.lyrics.pickLyrics
import com.kingplay.core.player.lyrics.syltToLrc
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Inject

/**
 * Looks for lyrics in the tags of the song that is playing and publishes them. Only tags inside the file are read;
 * a separate .lrc file next to the song is not reachable under Android's storage rules.
 */
@OptIn(UnstableApi::class)
@ServiceScoped
class LyricsBinder @Inject constructor(
    private val player: Player,
    private val lyrics: PlayingLyrics,
) {
    private val listener = object : Player.Listener {
        override fun onTracksChanged(tracks: Tracks) = refresh(tracks)

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = refresh(player.currentTracks)
    }

    fun start() {
        player.addListener(listener)
        refresh(player.currentTracks)
    }

    fun stop() {
        player.removeListener(listener)
        lyrics.publish(LyricsState.None)
    }

    private fun refresh(tracks: Tracks) {
        val mediaId = player.currentMediaItem?.mediaId
        if (MediaIds.parse(mediaId) !is MediaRef.Audio) {
            lyrics.publish(LyricsState.None)
            return
        }
        val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        if (audioGroups.isEmpty()) {
            lyrics.publish(LyricsState.Loading)
            return
        }
        val tags = buildList {
            for (group in audioGroups) {
                for (index in 0 until group.length) {
                    group.getTrackFormat(index).metadata?.let { addAll(it.toLyricTags()) }
                }
            }
        }
        val found = pickLyrics(tags)
        val result = if (found == null || mediaId == null) LyricsState.None else LyricsState.Available(mediaId, found)
        lyrics.publish(result)
    }
}

@OptIn(UnstableApi::class)
internal fun Metadata.toLyricTags(): List<Pair<String, String>> = buildList {
    for (index in 0 until length()) {
        when (val entry = get(index)) {
            is BinaryFrame -> when (entry.id) {
                "USLT" -> parseUslt(entry.data)?.let { add("USLT" to it) }
                "SYLT" -> syltToLrc(entry.data)?.let { add("SYLT" to it) }
            }
            is VorbisComment -> add(entry.key to entry.value)
            is TextInformationFrame -> {
                val value = entry.values.firstOrNull() ?: continue
                add((entry.description?.takeIf { it.isNotBlank() } ?: entry.id) to value)
            }
        }
    }
}
