package com.kingplay.app

import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.ScanState
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.Track
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.LibraryScanner
import com.kingplay.core.model.repository.PlaybackController
import com.kingplay.core.model.repository.PlaybackState
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
    val playback: PlaybackState = PlaybackState(),
) : UiState

sealed interface LibraryStatusEvent : UiEvent {
    /** Media permission was just granted or refused. */
    data object PermissionResult : LibraryStatusEvent

    data object Rescan : LibraryStatusEvent

    data object FullRescan : LibraryStatusEvent

    /** Play the recent-tracks list starting at [index]. */
    data class PlayRecent(val index: Int) : LibraryStatusEvent

    data object TogglePlayPause : LibraryStatusEvent

    data object Next : LibraryStatusEvent

    data object Previous : LibraryStatusEvent
}

data object LibraryStatusEffect : UiEffect

/** Temporary dev screen backing: proves scanning and the database work on a device before the library UI (step 2d). */
@HiltViewModel
class LibraryStatusViewModel @Inject constructor(
    private val scanner: LibraryScanner,
    private val playback: PlaybackController,
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
            val recent = library.recentlyAdded(RECENT_LIMIT)
            combine(scanner.state, counts, recent, playback.state) { scan, c, tracks, now ->
                LibraryStatusState(scan, c[0], c[1], c[2], c[3], tracks, now)
            }.collect { next -> setState { next } }
        }
        // Binding to the service restores the saved queue (paused), so Now Playing survives app restarts.
        playback.connect()
    }

    override fun onEvent(event: LibraryStatusEvent) {
        when (event) {
            LibraryStatusEvent.PermissionResult -> scanner.start()
            LibraryStatusEvent.Rescan -> scanner.requestScan()
            LibraryStatusEvent.FullRescan -> scanner.requestScan(full = true)
            is LibraryStatusEvent.PlayRecent ->
                playback.playMedia(state.value.recent.map { MediaRef.Audio(it.id) }, event.index)
            LibraryStatusEvent.TogglePlayPause -> playback.togglePlayPause()
            LibraryStatusEvent.Next -> playback.skipToNext()
            LibraryStatusEvent.Previous -> playback.skipToPrevious()
        }
    }

    private companion object {
        const val RECENT_LIMIT = 30
    }
}
