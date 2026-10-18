package com.kingplay.feature.nowplaying

import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.PlaybackController
import com.kingplay.core.ui.MviViewModel
import com.kingplay.core.ui.NoEffect
import com.kingplay.core.ui.UiEvent
import com.kingplay.core.ui.UiState
import com.kingplay.core.ui.format.progressFraction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

data class MiniPlayerState(
    val visible: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val artworkUri: String? = null,
    val artworkKey: String = "",
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
) : UiState

sealed interface MiniPlayerEvent : UiEvent {
    data object TogglePlayPause : MiniPlayerEvent

    data object Next : MiniPlayerEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MiniPlayerViewModel @Inject constructor(
    private val playback: PlaybackController,
    library: LibraryRepository,
) : MviViewModel<MiniPlayerState, MiniPlayerEvent, NoEffect>(MiniPlayerState()) {

    init {
        playback.connect()
        playback.state
            .map { current ->
                MiniPlayerState(
                    visible = current.hasMedia,
                    title = current.title.orEmpty(),
                    artist = current.artist.orEmpty(),
                    isPlaying = current.isPlaying,
                    progress = progressFraction(current.positionMs, current.durationMs),
                )
            }
            .distinctUntilChanged()
            .onEach { next -> setState { next.copy(artworkUri = artworkUri, artworkKey = artworkKey) } }
            .launchIn(viewModelScope)

        playback.state
            .map { (it.current as? MediaRef.Audio)?.id }
            .distinctUntilChanged()
            .flatMapLatest { id -> if (id == null) flowOf(null) else library.track(id) }
            .onEach { track ->
                setState {
                    copy(artworkUri = track?.uri, artworkKey = track?.let { "album:${it.albumId}" }.orEmpty())
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: MiniPlayerEvent) {
        when (event) {
            MiniPlayerEvent.TogglePlayPause -> playback.togglePlayPause()
            MiniPlayerEvent.Next -> playback.skipToNext()
        }
    }
}
