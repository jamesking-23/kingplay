package com.kingplay.desktop.engine

import kotlinx.coroutines.flow.Flow

sealed interface EngineEvent {
    data class State(val isPlaying: Boolean, val isBuffering: Boolean) : EngineEvent

    data class Position(val ms: Long) : EngineEvent

    data class Duration(val ms: Long) : EngineEvent

    /** The loaded file played to its end. */
    data object Ended : EngineEvent

    data class Failed(val message: String) : EngineEvent
}

/** The sound-making part, so the queue and controller logic can be tested without any native library. */
interface AudioEngine {
    val events: Flow<EngineEvent>

    /** Opens [path] and starts playing it. */
    fun load(path: String)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    /** 0..100. */
    fun setVolume(percent: Int)

    /**
     * Switches the 10-band equalizer. Band [i] of [bandGainsDb] goes to the engine's band [i]; the engine's centre
     * frequencies differ a little from the app's, so this is an approximation. Default does nothing.
     */
    fun setEqualizer(enabled: Boolean, preampDb: Float, bandGainsDb: List<Float>) = Unit

    fun release()
}
