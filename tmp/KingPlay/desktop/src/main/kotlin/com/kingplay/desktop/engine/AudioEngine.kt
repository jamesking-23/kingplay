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

    fun release()
}
