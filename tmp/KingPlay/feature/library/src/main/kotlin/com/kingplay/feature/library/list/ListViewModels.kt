package com.kingplay.feature.library.list

import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.Album
import com.kingplay.core.model.Artist
import com.kingplay.core.model.Genre
import com.kingplay.core.model.MediaFolder
import com.kingplay.core.model.Playlist
import com.kingplay.core.model.VideoItem
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.PlaylistRepository
import com.kingplay.core.model.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AlbumsViewModel @Inject constructor(library: LibraryRepository) : ListViewModel<Album>(library.albums())

@HiltViewModel
class ArtistsViewModel @Inject constructor(library: LibraryRepository) : ListViewModel<Artist>(library.artists())

@HiltViewModel
class GenresViewModel @Inject constructor(library: LibraryRepository) : ListViewModel<Genre>(library.genres())

@HiltViewModel
class VideosViewModel @Inject constructor(videos: VideoRepository) : ListViewModel<VideoItem>(videos.videos())

@HiltViewModel
class FoldersViewModel @Inject constructor(library: LibraryRepository) : ListViewModel<MediaFolder>(library.folders())

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlists: PlaylistRepository,
) : ListViewModel<Playlist>(playlists.playlists()) {

    fun create(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { playlists.create(trimmed) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { playlists.delete(id) }
    }
}
