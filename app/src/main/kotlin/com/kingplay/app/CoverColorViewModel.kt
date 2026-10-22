package com.kingplay.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.PlaybackController
import com.kingplay.core.ui.artwork.coverColor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The main colour of the playing song's cover. It stays until the next song's colour is known. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CoverColorViewModel @Inject constructor(
    playback: PlaybackController,
    library: LibraryRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val color: StateFlow<Int?> = playback.state
        .map { (it.current as? MediaRef.Audio)?.id }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(null) else library.track(id) }
        .distinctUntilChanged { a, b -> a?.id == b?.id }
        .map { track -> track?.let { coverColor(context, it.uri, "album:${it.albumId}") } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
