package com.kingplay.feature.video

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.VideoItem
import com.kingplay.core.model.repository.PlaybackController
import com.kingplay.core.model.repository.VideoRepository
import com.kingplay.core.player.video.ExternalSubtitle
import com.kingplay.core.player.video.VideoSession
import com.kingplay.core.ui.MviViewModel
import com.kingplay.core.ui.NoEffect
import com.kingplay.core.ui.UiEvent
import com.kingplay.core.ui.UiState
import com.kingplay.feature.video.logic.naturalCompare
import com.kingplay.feature.video.logic.resumeStartMs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object VideoArgs {
    const val VIDEO_ID = "videoId"
}

sealed interface VideoLoad {
    data object Loading : VideoLoad

    /** The video was removed from the library, or the id was never valid. */
    data object Missing : VideoLoad

    data class Ready(val video: VideoItem) : VideoLoad
}

data class VideoScreenState(val load: VideoLoad = VideoLoad.Loading) : UiState

sealed interface VideoEvent : UiEvent {
    /** The screen is leaving or going to the background: store where playback is. */
    data object SavePosition : VideoEvent

    data class AddSubtitle(val subtitle: ExternalSubtitle) : VideoEvent
}

private const val SAVE_INTERVAL_MS = 5_000L

/**
 * Starts the video in the playback service (with the other videos in its folder lined up as next episodes) and keeps
 * the resume position saved while it plays.
 */
@HiltViewModel
class VideoViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val videos: VideoRepository,
    private val session: VideoSession,
    private val playback: PlaybackController,
) : MviViewModel<VideoScreenState, VideoEvent, NoEffect>(VideoScreenState()) {

    val player: StateFlow<Player?> = session.player

    // Leaving the screen clears this view model, which would cancel a save started on the way out.
    private val saveScope = CoroutineScope(SupervisorJob())

    init {
        session.connect()
        val id = savedState.get<Long>(VideoArgs.VIDEO_ID) ?: -1L
        viewModelScope.launch { load(id) }
        viewModelScope.launch {
            while (true) {
                delay(SAVE_INTERVAL_MS)
                if (playback.state.value.isPlaying) savePosition()
            }
        }
    }

    private suspend fun load(id: Long) {
        val video = videos.getVideos(listOf(id)).firstOrNull()
        if (video == null) {
            setState { copy(load = VideoLoad.Missing) }
            return
        }
        val folder = videos.folderVideos(video.folderPath).first()
            .sortedWith { a, b -> naturalCompare(a.title, b.title) }
        val queue = folder.ifEmpty { listOf(video) }
        val index = queue.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
        session.start(queue.map { it.id }, index, resumeStartMs(video.resumePositionMs, video.durationMs))
        setState { copy(load = VideoLoad.Ready(video)) }
    }

    override fun onEvent(event: VideoEvent) {
        when (event) {
            VideoEvent.SavePosition -> saveScope.launch { savePosition() }
            is VideoEvent.AddSubtitle -> session.attachSubtitle(event.subtitle)
        }
    }

    private suspend fun savePosition() {
        val current = playback.state.value
        val ref = current.current as? MediaRef.Video ?: return
        videos.saveResumePosition(ref.id, current.positionMs, System.currentTimeMillis())
    }
}
