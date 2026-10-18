package com.kingplay.core.player.controller

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.kingplay.core.common.ApplicationScope
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.RepeatMode
import com.kingplay.core.model.repository.PlaybackController
import com.kingplay.core.model.repository.PlaybackState
import com.kingplay.core.player.logic.MediaIds
import com.kingplay.core.player.logic.playNextInsertIndex
import com.kingplay.core.player.service.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Talks to the playback service through a Media3 [MediaController]. Everything here runs on the main thread, which
 * is where the controller must be used. Commands issued before the connection is ready are queued, not dropped.
 */
@Singleton
class MediaControllerPlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope appScope: CoroutineScope,
) : PlaybackController, Player.Listener {

    private val mainScope = CoroutineScope(appScope.coroutineContext + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var connecting: ListenableFuture<MediaController>? = null
    private val pending = ArrayDeque<(MediaController) -> Unit>()

    override fun connect() {
        mainScope.launch { ensureConnecting() }
    }

    private fun ensureConnecting() {
        if (controller != null || connecting != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        connecting = future
        future.addListener(
            {
                val connected = runCatching { future.get() }.getOrNull()
                connecting = null
                if (connected == null) {
                    _state.update { it.copy(error = CONNECT_ERROR) }
                    pending.clear()
                    return@addListener
                }
                controller = connected
                connected.addListener(this)
                while (pending.isNotEmpty()) pending.removeFirst().invoke(connected)
                refresh()
                startPositionTicker()
            },
            MoreExecutors.directExecutor(),
        )
    }

    /** Runs [command] now if connected, otherwise after the connection completes. */
    private fun withController(command: (MediaController) -> Unit) {
        mainScope.launch {
            val ready = controller
            if (ready != null) {
                command(ready)
            } else {
                pending.addLast(command)
                ensureConnecting()
            }
        }
    }

    override fun playMedia(refs: List<MediaRef>, startIndex: Int) {
        if (refs.isEmpty()) return
        withController { c ->
            c.setMediaItems(refs.map(::stubItem), startIndex.coerceIn(0, refs.lastIndex), C.TIME_UNSET)
            c.prepare()
            c.play()
        }
    }

    override fun play() = withController {
        if (it.playbackState == Player.STATE_IDLE) it.prepare()
        it.play()
    }

    override fun pause() = withController { it.pause() }

    override fun togglePlayPause() = withController { if (it.isPlaying) it.pause() else it.play() }

    override fun skipToNext() = withController { it.seekToNext() }

    override fun skipToPrevious() = withController { it.seekToPrevious() }

    override fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs.coerceAtLeast(0)) }

    override fun skipToQueueIndex(index: Int) = withController {
        if (index in 0 until it.mediaItemCount) {
            it.seekToDefaultPosition(index)
            it.play()
        }
    }

    override fun playNext(refs: List<MediaRef>) {
        if (refs.isEmpty()) return
        withController { c ->
            val at = playNextInsertIndex(c.currentMediaItemIndex, c.mediaItemCount)
            c.addMediaItems(at, refs.map(::stubItem))
        }
    }

    override fun addToQueue(refs: List<MediaRef>) {
        if (refs.isEmpty()) return
        withController { c -> c.addMediaItems(refs.map(::stubItem)) }
    }

    override fun removeFromQueue(index: Int) = withController {
        if (index in 0 until it.mediaItemCount) it.removeMediaItem(index)
    }

    override fun moveInQueue(from: Int, to: Int) = withController {
        val last = it.mediaItemCount - 1
        if (from in 0..last && to in 0..last && from != to) it.moveMediaItem(from, to)
    }

    override fun clearQueue() = withController {
        it.stop()
        it.clearMediaItems()
    }

    override fun setShuffleEnabled(enabled: Boolean) = withController { it.shuffleModeEnabled = enabled }

    override fun setRepeatMode(mode: RepeatMode) = withController {
        it.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    override fun onEvents(player: Player, events: Player.Events) = refresh()

    private fun refresh() {
        val c = controller ?: return
        val queue = (0 until c.mediaItemCount).mapNotNull { MediaIds.parse(c.getMediaItemAt(it).mediaId) }
        val meta = c.mediaMetadata
        val duration = c.duration
        _state.value = PlaybackState(
            isConnected = true,
            isPlaying = c.isPlaying,
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            queue = queue,
            currentIndex = if (queue.isEmpty()) -1 else c.currentMediaItemIndex,
            title = meta.title?.toString(),
            artist = meta.artist?.toString(),
            album = meta.albumTitle?.toString(),
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = if (duration == C.TIME_UNSET) 0 else duration.coerceAtLeast(0),
            shuffleEnabled = c.shuffleModeEnabled,
            repeatMode = when (c.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                else -> RepeatMode.OFF
            },
            error = c.playerError?.message,
        )
    }

    private var tickerRunning = false

    private fun startPositionTicker() {
        if (tickerRunning) return
        tickerRunning = true
        mainScope.launch {
            while (true) {
                delay(POSITION_TICK_MS)
                val c = controller ?: continue
                if (c.isPlaying) _state.update { it.copy(positionMs = c.currentPosition.coerceAtLeast(0)) }
            }
        }
    }

    /** Id-only item: the service resolves it to a playable item with URI and metadata. */
    private fun stubItem(ref: MediaRef): MediaItem = MediaItem.Builder().setMediaId(MediaIds.of(ref)).build()

    private companion object {
        const val POSITION_TICK_MS = 500L
        const val CONNECT_ERROR = "Could not connect to the playback service"
    }
}
