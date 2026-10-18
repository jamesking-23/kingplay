package com.kingplay.feature.nowplaying.stage

import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.lyrics.LyricsState
import com.kingplay.core.model.repository.LyricsRepository
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class StageState(val lyrics: LyricsState = LyricsState.Loading) : UiState

sealed interface StageEvent : UiEvent {
    data class Seek(val positionMs: Long) : StageEvent
}

/** Feeds the lyrics view and the visualizer. */
@HiltViewModel
class StageViewModel @Inject constructor(
    lyrics: LyricsRepository,
    val spectrum: SpectrumSource,
    private val playback: PlaybackController,
) : MviViewModel<StageState, StageEvent, NoEffect>(StageState()) {

    init {
        lyrics.lyrics.onEach { next -> setState { copy(lyrics = next) } }.launchIn(viewModelScope)
    }

    override fun onEvent(event: StageEvent) {
        when (event) {
            is StageEvent.Seek -> playback.seekTo(event.positionMs)
        }
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
