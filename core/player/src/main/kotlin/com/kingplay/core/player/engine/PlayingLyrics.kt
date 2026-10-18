package com.kingplay.core.player.engine

import com.kingplay.core.model.lyrics.LyricsState
import com.kingplay.core.model.repository.LyricsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds the lyrics of the current song. The service writes it; the screen reads it. */
@Singleton
class PlayingLyrics @Inject constructor() : LyricsRepository {
    private val state = MutableStateFlow<LyricsState>(LyricsState.None)

    override val lyrics: StateFlow<LyricsState> = state.asStateFlow()

    fun publish(newState: LyricsState) {
        state.value = newState
    }
}
