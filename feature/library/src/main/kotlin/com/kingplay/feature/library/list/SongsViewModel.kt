package com.kingplay.feature.library.list

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.SongSort
import com.kingplay.core.model.SongSortField
import com.kingplay.core.model.Track
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.PlaybackController
import com.kingplay.core.ui.MviViewModel
import com.kingplay.core.ui.UiEvent
import com.kingplay.core.ui.UiState
import com.kingplay.feature.library.LibraryEffect
import com.kingplay.feature.library.R
import com.kingplay.feature.library.logic.TrackActions
import com.kingplay.feature.library.logic.chooseField
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class SongsState(
    val sort: SongSort = SongSort(),
    val currentTrackId: Long? = null,
) : UiState

sealed interface SongsEvent : UiEvent {
    /** Picking the active field again flips the direction. */
    data class SortBy(val field: SongSortField) : SongsEvent

    data class Play(val trackId: Long) : SongsEvent

    data object ShuffleAll : SongsEvent

    data class PlayNext(val track: Track) : SongsEvent

    data class AddToQueue(val track: Track) : SongsEvent

    data class ToggleFavorite(val track: Track) : SongsEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SongsViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val actions: TrackActions,
    playback: PlaybackController,
) : MviViewModel<SongsState, SongsEvent, LibraryEffect>(SongsState()) {

    /** Paged list for the current sort. Cached so rotation and tab switches do not reload it. */
    val songs: Flow<PagingData<Track>> = state
        .map { it.sort }
        .distinctUntilChanged()
        .flatMapLatest { sort -> library.songs(sort) }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            playback.state
                .map { (it.current as? MediaRef.Audio)?.id }
                .distinctUntilChanged()
                .collect { id -> setState { copy(currentTrackId = id) } }
        }
    }

    override fun onEvent(event: SongsEvent) {
        when (event) {
            is SongsEvent.SortBy -> setState { copy(sort = sort.chooseField(event.field)) }
            is SongsEvent.Play -> viewModelScope.launch {
                actions.playFrom(library.songIds(state.value.sort), event.trackId)
            }
            SongsEvent.ShuffleAll -> viewModelScope.launch { actions.shuffle(library.songIds(state.value.sort)) }
            is SongsEvent.PlayNext -> {
                actions.playNext(event.track)
                sendEffect(LibraryEffect.Message(R.string.library_added_next))
            }
            is SongsEvent.AddToQueue -> {
                actions.addToQueue(event.track)
                sendEffect(LibraryEffect.Message(R.string.library_added_queue))
            }
            is SongsEvent.ToggleFavorite -> viewModelScope.launch { actions.toggleFavorite(event.track) }
        }
    }
}
