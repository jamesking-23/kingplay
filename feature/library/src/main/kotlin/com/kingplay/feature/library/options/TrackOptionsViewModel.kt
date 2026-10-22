package com.kingplay.feature.library.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.Playlist
import com.kingplay.core.model.Track
import com.kingplay.core.model.repository.LibraryScanner
import com.kingplay.core.model.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TrackOptionsViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val scanner: LibraryScanner,
) : ViewModel() {
    val playlists: StateFlow<List<Playlist>> = repository.playlists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    fun add(playlistId: Long, track: Track) {
        viewModelScope.launch { repository.add(playlistId, listOf(MediaRef.Audio(track.id))) }
    }

    fun createWith(name: String, track: Track) {
        viewModelScope.launch {
            val id = repository.create(name)
            repository.add(id, listOf(MediaRef.Audio(track.id)))
        }
    }

    /** The file is gone from the phone; a scan removes it from the library. */
    fun afterDeleted() = scanner.requestScan(full = false)

    private companion object {
        const val STOP_MS = 5_000L
    }
}
