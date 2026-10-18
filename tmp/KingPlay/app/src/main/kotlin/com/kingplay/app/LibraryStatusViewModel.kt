package com.kingplay.app

import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.ScanState
import com.kingplay.core.model.Track
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.LibraryScanner
import com.kingplay.core.model.repository.VideoRepository
import com.kingplay.core.ui.MviViewModel
import com.kingplay.core.ui.UiEffect
import com.kingplay.core.ui.UiEvent
import com.kingplay.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class LibraryStatusState(
    val scan: ScanState = ScanState.Idle,
    val songs: Int = 0,
    val albums: Int = 0,
    val artists: Int = 0,
    val videos: Int = 0,
    val recent: List<Track> = emptyList(),
) : UiState

sealed interface LibraryStatusEvent : UiEvent {
    /** Media permission was just granted or refused. */
    data object PermissionResult : LibraryStatusEvent

    data object Rescan : LibraryStatusEvent

    data object FullRescan : LibraryStatusEvent
}

data object LibraryStatusEffect : UiEffect

/** Temporary dev screen backing: proves scanning and the database work on a device before the library UI (step 2d). */
@HiltViewModel
class LibraryStatusViewModel @Inject constructor(
    private val scanner: LibraryScanner,
    library: LibraryRepository,
    videos: VideoRepository,
) : MviViewModel<LibraryStatusState, LibraryStatusEvent, LibraryStatusEffect>(LibraryStatusState()) {

    init {
        val counts = combine(
            library.songCount(),
            library.albums().map { it.size },
            library.artists().map { it.size },
            videos.videoCount(),
        ) { songs, albums, artists, videoCount -> listOf(songs, albums, artists, videoCount) }

        viewModelScope.launch {
            combine(scanner.state, counts, library.recentlyAdded(RECENT_LIMIT)) { scan, c, recent ->
                LibraryStatusState(scan, c[0], c[1], c[2], c[3], recent)
            }.collect { next -> setState { next } }
        }
    }

    override fun onEvent(event: LibraryStatusEvent) {
        when (event) {
            LibraryStatusEvent.PermissionResult -> scanner.start()
            LibraryStatusEvent.Rescan -> scanner.requestScan()
            LibraryStatusEvent.FullRescan -> scanner.requestScan(full = true)
        }
    }

    private companion object {
        const val RECENT_LIMIT = 30
    }
}
