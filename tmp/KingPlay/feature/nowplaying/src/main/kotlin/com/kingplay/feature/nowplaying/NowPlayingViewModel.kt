package com.kingplay.feature.nowplaying

import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.Track
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.PlaybackController
import com.kingplay.core.model.repository.PlaybackState
import com.kingplay.core.model.repository.VideoRepository
import com.kingplay.core.ui.MviViewModel
import com.kingplay.core.ui.NoEffect
import com.kingplay.core.ui.UiEvent
import com.kingplay.core.ui.UiState
import com.kingplay.feature.nowplaying.logic.QueueItem
import com.kingplay.feature.nowplaying.logic.buildQueueItems
import com.kingplay.feature.nowplaying.logic.nextRepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * [playback] never carries the position: the position changes twice a second and would redraw the whole screen.
 * It comes separately through [NowPlayingViewModel.positionMs].
 */
data class NowPlayingState(
    val playback: PlaybackState = PlaybackState(),
    val track: Track? = null,
    val queue: List<QueueItem> = emptyList(),
) : UiState

sealed interface NowPlayingEvent : UiEvent {
    data object TogglePlayPause : NowPlayingEvent

    data object Next : NowPlayingEvent

    data object Previous : NowPlayingEvent

    data class SeekTo(val positionMs: Long) : NowPlayingEvent

    data object ToggleShuffle : NowPlayingEvent

    data object CycleRepeat : NowPlayingEvent

    data object ToggleFavorite : NowPlayingEvent

    data class SkipTo(val index: Int) : NowPlayingEvent

    data class RemoveAt(val index: Int) : NowPlayingEvent

    data class MoveUp(val index: Int) : NowPlayingEvent

    data class MoveDown(val index: Int) : NowPlayingEvent

    data object ClearQueue : NowPlayingEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playback: PlaybackController,
    private val library: LibraryRepository,
    private val videos: VideoRepository,
) : MviViewModel<NowPlayingState, NowPlayingEvent, NoEffect>(NowPlayingState()) {

    val positionMs: StateFlow<Long> = playback.state
        .map { it.positionMs }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0L)

    init {
        playback.connect()
        playback.state
            .map { it.copy(positionMs = 0) }
            .distinctUntilChanged()
            .onEach { current -> setState { copy(playback = current) } }
            .launchIn(viewModelScope)

        // The full track row, kept live so the favorite heart updates the moment it changes.
        playback.state
            .map { (it.current as? MediaRef.Audio)?.id }
            .distinctUntilChanged()
            .flatMapLatest { id -> if (id == null) flowOf(null) else library.track(id) }
            .onEach { track -> setState { copy(track = track) } }
            .launchIn(viewModelScope)

        playback.state
            .map { it.queue }
            .distinctUntilChanged()
            .mapLatest { refs -> resolveQueue(refs) }
            .onEach { items -> setState { copy(queue = items) } }
            .launchIn(viewModelScope)
    }

    private suspend fun resolveQueue(refs: List<MediaRef>): List<QueueItem> {
        val audio = library.getTracks(refs.filterIsInstance<MediaRef.Audio>().map { it.id }).associateBy { it.id }
        val video = videos.getVideos(refs.filterIsInstance<MediaRef.Video>().map { it.id }).associateBy { it.id }
        return buildQueueItems(refs, audio, video)
    }

    override fun onEvent(event: NowPlayingEvent) {
        val current = state.value.playback
        when (event) {
            NowPlayingEvent.TogglePlayPause -> playback.togglePlayPause()
            NowPlayingEvent.Next -> playback.skipToNext()
            NowPlayingEvent.Previous -> playback.skipToPrevious()
            is NowPlayingEvent.SeekTo -> playback.seekTo(event.positionMs)
            NowPlayingEvent.ToggleShuffle -> playback.setShuffleEnabled(!current.shuffleEnabled)
            NowPlayingEvent.CycleRepeat -> playback.setRepeatMode(nextRepeatMode(current.repeatMode))
            NowPlayingEvent.ToggleFavorite -> state.value.track?.let { track ->
                viewModelScope.launch { library.setFavorite(track.id, !track.isFavorite) }
            }
            is NowPlayingEvent.SkipTo -> playback.skipToQueueIndex(event.index)
            is NowPlayingEvent.RemoveAt -> playback.removeFromQueue(event.index)
            is NowPlayingEvent.MoveUp -> playback.moveInQueue(event.index, event.index - 1)
            is NowPlayingEvent.MoveDown -> playback.moveInQueue(event.index, event.index + 1)
            NowPlayingEvent.ClearQueue -> playback.clearQueue()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
