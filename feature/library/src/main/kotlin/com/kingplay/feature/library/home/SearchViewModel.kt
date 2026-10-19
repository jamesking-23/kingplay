package com.kingplay.feature.library.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.SearchResults
import com.kingplay.core.model.Track
import com.kingplay.core.model.repository.SearchRepository
import com.kingplay.feature.library.logic.TrackActions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** Library search: the typed text goes in, grouped matches come out (after a short pause in typing). */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    search: SearchRepository,
    private val actions: TrackActions,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val results: StateFlow<SearchResults> = _query
        .debounce(DEBOUNCE_MS)
        .flatMapLatest { text ->
            if (text.isBlank()) flowOf(SearchResults.Empty) else search.search(text).catch { emit(SearchResults.Empty) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SearchResults.Empty)

    fun onQueryChange(text: String) {
        _query.value = text
    }

    fun play(track: Track, among: List<Track>) = actions.playFrom(among.map { it.id }, track.id)

    private companion object {
        const val DEBOUNCE_MS = 200L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
