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

    /** Kept from the event callbacks, because the callbacks must not call back into the player. */
    @Volatile
    private var isPlayingNow = false

    init {
        player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                isPlayingNow = true
                bus.tryEmit(EngineEvent.State(isPlaying = true, isBuffering = false))
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                isPlayingNow = false
                bus.tryEmit(EngineEvent.State(isPlaying = false, isBuffering = false))
            }

            override fun buffering(mediaPlayer: MediaPlayer, newCache: Float) {
                bus.tryEmit(EngineEvent.State(isPlaying = isPlayingNow, isBuffering = newCache < FULL))
            }

            override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
                bus.tryEmit(EngineEvent.Position(newTime))
            }

            override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
                bus.tryEmit(EngineEvent.Duration(newLength))
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                isPlayingNow = false
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

    override fun setEqualizer(enabled: Boolean, preampDb: Float, bandGainsDb: List<Float>) {
        // "Off" is a flat equalizer: it passes sound through unchanged and needs no special case in libVLC.
        val equalizer = factory.equalizer().newEqualizer()
        equalizer.setPreamp(if (enabled) preampDb.coerceIn(-MAX_DB, MAX_DB) else 0f)
        for (band in 0 until minOf(equalizer.bandCount(), bandGainsDb.size)) {
            equalizer.setAmp(band, if (enabled) bandGainsDb[band].coerceIn(-MAX_DB, MAX_DB) else 0f)
        }
        player.audio().setEqualizer(equalizer)
        equalizer.release() // libVLC copies the settings, so this one is no longer needed
    }

    override fun release() {
        player.release()
        factory.release()
    }

    companion object {
        private const val FULL = 100f
        private const val MAX_DB = 20f

        /** Null when VLC is not installed, so the app can tell the person instead of crashing. */
        fun createOrNull(): VlcjAudioEngine? = runCatching {
            val factory = MediaPlayerFactory(NativeDiscovery(), "--no-video", "--quiet")
            VlcjAudioEngine(factory, factory.mediaPlayers().newMediaPlayer())
        }.getOrNull()
    }
}
