package com.kingplay.feature.library.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.Album
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.PlaybackController
import com.kingplay.core.model.repository.PlaylistRepository
import com.kingplay.core.ui.Async
import com.kingplay.feature.library.logic.TrackActions
import com.kingplay.feature.library.logic.entryPositionFor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    handle: SavedStateHandle,
    library: LibraryRepository,
    actions: TrackActions,
    playback: PlaybackController,
) : DetailViewModel(
    title = library.album(handle.requireLong(DetailArgs.ALBUM_ID)).map { album ->
        album?.let { DetailTitle(it.title, it.artistName) }
    },
    trackSource = library.albumTracks(handle.requireLong(DetailArgs.ALBUM_ID)),
    actions = actions,
    playback = playback,
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    handle: SavedStateHandle,
    library: LibraryRepository,
    actions: TrackActions,
    playback: PlaybackController,
) : DetailViewModel(
    title = library.artist(handle.requireLong(DetailArgs.ARTIST_ID)).map { artist ->
        artist?.let { DetailTitle(it.name) }
    },
    trackSource = library.artistTracks(handle.requireLong(DetailArgs.ARTIST_ID)),
    actions = actions,
    playback = playback,
) {
    private val artistId = handle.requireLong(DetailArgs.ARTIST_ID)

    /** The artist's albums, shown above the songs. */
    val albums: StateFlow<List<Album>> = library.artistAlbums(artistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

@HiltViewModel
class GenreDetailViewModel @Inject constructor(
    handle: SavedStateHandle,
    library: LibraryRepository,
    actions: TrackActions,
    playback: PlaybackController,
) : DetailViewModel(
    title = flowOf(DetailTitle(handle.requireString(DetailArgs.GENRE))),
    trackSource = library.genreTracks(handle.requireString(DetailArgs.GENRE)),
    actions = actions,
    playback = playback,
)

@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    handle: SavedStateHandle,
    library: LibraryRepository,
    actions: TrackActions,
    playback: PlaybackController,
) : DetailViewModel(
    title = flowOf(
        handle.requireString(DetailArgs.FOLDER_PATH).let { path ->
            DetailTitle(path.trimEnd('/').substringAfterLast('/'), path)
        },
    ),
    trackSource = library.folderTracks(handle.requireString(DetailArgs.FOLDER_PATH)),
    actions = actions,
    playback = playback,
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val playlists: PlaylistRepository,
    actions: TrackActions,
    playback: PlaybackController,
) : DetailViewModel(
    title = playlists.playlist(handle.requireLong(DetailArgs.PLAYLIST_ID)).map { playlist ->
        playlist?.let { DetailTitle(it.name) }
    },
    trackSource = playlists.tracks(handle.requireLong(DetailArgs.PLAYLIST_ID)),
    actions = actions,
    playback = playback,
) {
    private val playlistId = handle.requireLong(DetailArgs.PLAYLIST_ID)

    /** Removes the row at [rowIndex] of the track list shown on screen. */
    fun removeRow(rowIndex: Int) {
        viewModelScope.launch {
            val shown = (state.value.tracks as? Async.Content)?.data.orEmpty().map { it.id }
            val position = entryPositionFor(playlists.entries(playlistId).first(), shown, rowIndex) ?: return@launch
            playlists.removeAt(playlistId, position)
        }
    }
}
