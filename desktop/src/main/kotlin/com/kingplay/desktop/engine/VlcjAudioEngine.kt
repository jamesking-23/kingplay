package com.kingplay.desktop.engine

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

/**
 * Plays through VLC's engine (libVLC), which must be installed (64-bit VLC for 64-bit Windows).
 * vlcj event callbacks run on a native thread and must never call back into the player, so they only publish events.
 */
class VlcjAudioEngine private constructor(
    private val factory: MediaPlayerFactory,
    private val player: MediaPlayer,
) : AudioEngine {
    private val bus = MutableSharedFlow<EngineEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: Flow<EngineEvent> = bus

    init {
        player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                bus.tryEmit(EngineEvent.State(isPlaying = true, isBuffering = false))
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                bus.tryEmit(EngineEvent.State(isPlaying = false, isBuffering = false))
            }

            override fun buffering(mediaPlayer: MediaPlayer, newCache: Float) {
                val playing = mediaPlayer.status().isPlaying
                bus.tryEmit(EngineEvent.State(isPlaying = playing, isBuffering = newCache < FULL))
            }

            override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
                bus.tryEmit(EngineEvent.Position(newTime))
            }

            override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
                bus.tryEmit(EngineEvent.Duration(newLength))
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                bus.tryEmit(EngineEvent.Ended)
            }

            override fun error(mediaPlayer: MediaPlayer) {
                bus.tryEmit(EngineEvent.Failed("This file could not be played"))
            }
        })
    }

    override fun load(path: String) {
        player.media().play(path)
    }

    override fun play() = player.controls().play()

    override fun pause() = player.controls().setPause(true)

    override fun seekTo(positionMs: Long) = player.controls().setTime(positionMs.coerceAtLeast(0))

    override fun setVolume(percent: Int) {
        player.audio().setVolume(percent.coerceIn(0, 100))
    }

    override fun release() {
        player.release()
        factory.release()
    }

    companion object {
        private const val FULL = 100f

        /** Null when VLC is not installed, so the app can tell the person instead of crashing. */
        fun createOrNull(): VlcjAudioEngine? = runCatching {
            val factory = MediaPlayerFactory(NativeDiscovery(), "--no-video", "--quiet")
            VlcjAudioEngine(factory, factory.mediaPlayers().newMediaPlayer())
        }.getOrNull()
    }
}
