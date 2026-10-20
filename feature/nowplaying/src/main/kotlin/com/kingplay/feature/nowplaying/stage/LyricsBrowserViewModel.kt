package com.kingplay.feature.nowplaying.stage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.lyrics.lyricsKey
import com.kingplay.core.model.lyrics.parseLyrics
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.LyricsStore
import com.kingplay.core.model.repository.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the in-app browser needs: which song to look up, and a place to save the lyrics the person copied. */
@HiltViewModel
class LyricsBrowserViewModel @Inject constructor(
    private val playback: PlaybackController,
    private val library: LibraryRepository,
    private val store: LyricsStore,
) : ViewModel() {

    /** Search words for the playing song, or an empty string when nothing is playing. */
    suspend fun searchQuery(): String {
        val id = (playback.state.value.current as? MediaRef.Audio)?.id ?: return ""
        val track = library.track(id).first() ?: return ""
        return "${track.artistName} ${track.title} lyrics"
    }

    /** Saves [text] as the lyrics of the playing song. Returns false when it is not usable lyrics. */
    fun save(text: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val id = (playback.state.value.current as? MediaRef.Audio)?.id
            val track = id?.let { library.track(it).first() }
            if (track == null || parseLyrics(text) == null) {
                onDone(false)
                return@launch
            }
            store.save(lyricsKey(track.artistName, track.title), text)
            onDone(true)
        }
    }
}
