package com.kingplay.feature.nowplaying.stage

import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.lyrics.LyricsState
import com.kingplay.core.model.lyrics.lyricsKey
import com.kingplay.core.model.lyrics.parseLyrics
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.LyricsRepository
import com.kingplay.core.model.repository.LyricsStore
import com.kingplay.core.model.repository.PlaybackController
import com.kingplay.core.model.repository.SleepTimer
import com.kingplay.core.model.repository.SpectrumSource
import com.kingplay.core.model.sleep.SleepTimerSpec
import com.kingplay.core.model.sleep.SleepTimerState
import com.kingplay.core.ui.MviViewModel
import com.kingplay.core.ui.NoEffect
import com.kingplay.core.ui.UiEvent
import com.kingplay.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StageState(
    val lyrics: LyricsState = LyricsState.Loading,
    /** The lyrics shown came from a .lrc file the user imported, not from the song's tags. */
    val lyricsImported: Boolean = false,
    /** The last file the user picked could not be used as lyrics. */
    val importFailed: Boolean = false,
) : UiState

sealed interface StageEvent : UiEvent {
    data class Seek(val positionMs: Long) : StageEvent

    /** Text of a picked .lrc file, or null when it could not be read. */
    data class ImportLrc(val text: String?) : StageEvent

    data object RemoveImported : StageEvent
}

/** Feeds the lyrics view and the visualizer. Lyrics the user imported win over the ones in the song's tags. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StageViewModel @Inject constructor(
    lyrics: LyricsRepository,
    library: LibraryRepository,
    private val store: LyricsStore,
    val spectrum: SpectrumSource,
    private val playback: PlaybackController,
) : MviViewModel<StageState, StageEvent, NoEffect>(StageState()) {

    private val currentTrack = playback.state
        .map { (it.current as? MediaRef.Audio)?.id }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(null) else library.track(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        val imported = currentTrack.flatMapLatest { track ->
            if (track == null) flowOf(null) else store.observe(lyricsKey(track.artistName, track.title))
        }
        combine(lyrics.lyrics, currentTrack, imported) { embedded, track, lrc ->
            val parsed = if (track != null && lrc != null) parseLyrics(lrc) else null
            if (track != null && parsed != null) {
                LyricsState.Available("a:${track.id}", parsed) to true
            } else {
                embedded to false
            }
        }
            .onEach { (next, fromFile) -> setState { copy(lyrics = next, lyricsImported = fromFile) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: StageEvent) {
        when (event) {
            is StageEvent.Seek -> playback.seekTo(event.positionMs)
            is StageEvent.ImportLrc -> importLrc(event.text)
            StageEvent.RemoveImported -> currentTrack.value?.let { track ->
                viewModelScope.launch { store.remove(lyricsKey(track.artistName, track.title)) }
            }
        }
    }

    private fun importLrc(text: String?) {
        val track = currentTrack.value
        if (track == null || text == null || parseLyrics(text) == null) {
            setState { copy(importFailed = true) }
            return
        }
        setState { copy(importFailed = false) }
        viewModelScope.launch { store.save(lyricsKey(track.artistName, track.title), text) }
    }
}

data class SleepState(val timer: SleepTimerState = SleepTimerState.Off) : UiState

sealed interface SleepEvent : UiEvent {
    data class Start(val spec: SleepTimerSpec) : SleepEvent

    data class Extend(val minutes: Int) : SleepEvent

    data object Cancel : SleepEvent
}

@HiltViewModel
class SleepTimerViewModel @Inject constructor(
    private val timer: SleepTimer,
) : MviViewModel<SleepState, SleepEvent, NoEffect>(SleepState()) {

    init {
        timer.state.onEach { next -> setState { copy(timer = next) } }.launchIn(viewModelScope)
    }

    override fun onEvent(event: SleepEvent) {
        when (event) {
            is SleepEvent.Start -> timer.start(event.spec)
            is SleepEvent.Extend -> timer.extend(event.minutes)
            SleepEvent.Cancel -> timer.cancel()
        }
    }
}
