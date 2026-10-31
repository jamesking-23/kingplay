package com.kingplay.feature.library.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.Track
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.PlaylistRepository
import com.kingplay.core.model.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Behind the "Add songs" picker of a playlist: the whole library to browse, or search matches while typing. */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class AddSongsViewModel @Inject constructor(
    library: LibraryRepository,
    search: SearchRepository,
    private val playlists: PlaylistRepository,
) : ViewModel() {
    val query = MutableStateFlow("")

    val all: Flow<PagingData<Track>> = library.songs().cachedIn(viewModelScope)

    /** Matches for the typed text, or null while the box is empty (then [all] is shown). */
    val found: StateFlow<List<Track>?> = query
        .debounce(DEBOUNCE_MS)
        .flatMapLatest { text ->
            if (text.isBlank()) {
                flowOf(null)
            } else {
                search.search(text, MAX_MATCHES).map { it.tracks }.catch { emit(emptyList()) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), null)

    fun add(playlistId: Long, trackIds: Collection<Long>) {
        if (trackIds.isEmpty()) return
        viewModelScope.launch { playlists.add(playlistId, trackIds.map { MediaRef.Audio(it) }) }
    }

    private companion object {
        const val DEBOUNCE_MS = 200L
        const val MAX_MATCHES = 500
        const val STOP_MS = 5_000L
    }
}
