package com.kingplay.feature.video.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import kotlinx.coroutines.delay

private const val POSITION_TICK_MS = 250L

/** What the controls need to know about the player, as Compose state. Updated from the player's own events. */
@Stable
class VideoPlayerState(val player: Player) {
    var isPlaying by mutableStateOf(player.isPlaying)
        private set
    var isBuffering by mutableStateOf(player.playbackState == Player.STATE_BUFFERING)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var positionMs by mutableLongStateOf(player.currentPosition)
        private set
    var speed by mutableFloatStateOf(player.playbackParameters.speed)
        private set
    var tracks by mutableStateOf(player.currentTracks)
        private set
    var videoWidth by mutableIntStateOf(0)
        private set
    var videoHeight by mutableIntStateOf(0)
        private set
    var title by mutableStateOf("")
        private set
    var hasNext by mutableStateOf(false)
        private set
    var hasPrevious by mutableStateOf(false)
        private set

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync()

        override fun onTracksChanged(tracks: Tracks) {
            this@VideoPlayerState.tracks = tracks
        }
    }

    init {
        sync()
    }

    fun attach() = player.addListener(listener)

    fun detach() = player.removeListener(listener)

    fun sync() {
        isPlaying = player.isPlaying
        isBuffering = player.playbackState == Player.STATE_BUFFERING
        durationMs = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        positionMs = player.currentPosition
        speed = player.playbackParameters.speed
        tracks = player.currentTracks
        videoWidth = player.videoSize.width
        videoHeight = player.videoSize.height
        title = player.mediaMetadata.title?.toString().orEmpty()
        hasNext = player.hasNextMediaItem()
        hasPrevious = player.hasPreviousMediaItem()
    }

    fun refreshPosition() {
        positionMs = player.currentPosition
    }
}

@Composable
fun rememberVideoPlayerState(player: Player?): VideoPlayerState? {
    val state = remember(player) { player?.let { VideoPlayerState(it) } }
    DisposableEffect(state) {
        state?.attach()
        onDispose { state?.detach() }
    }
    LaunchedEffect(state, state?.isPlaying) {
        while (state != null && state.isPlaying) {
            state.refreshPosition()
            delay(POSITION_TICK_MS)
        }
    }
    return state
}
