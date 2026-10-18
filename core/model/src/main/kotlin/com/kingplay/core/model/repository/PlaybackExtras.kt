package com.kingplay.core.model.repository

import com.kingplay.core.model.lyrics.LyricsState
import com.kingplay.core.model.sleep.SleepTimerSpec
import com.kingplay.core.model.sleep.SleepTimerState
import kotlinx.coroutines.flow.StateFlow

/** Lyrics of the song that is playing now. Read from the song's own tags. */
interface LyricsRepository {
    val lyrics: StateFlow<LyricsState>
}

interface SleepTimer {
    val state: StateFlow<SleepTimerState>

    fun start(spec: SleepTimerSpec)

    fun extend(minutes: Int)

    fun cancel()
}

/** Live loudness per frequency band of what is playing, for the visualizer. */
interface SpectrumSource {
    val bandCount: Int

    /** Latest levels, 0..1, [bandCount] values, low to high pitch. Cheap enough to read every frame. */
    fun latest(): FloatArray

    /** The analysis only runs while something is showing it. */
    fun setActive(active: Boolean)
}
