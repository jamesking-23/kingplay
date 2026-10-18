package com.kingplay.core.player.engine

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import com.kingplay.core.model.AudioSettings
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.ReplayGain
import com.kingplay.core.model.repository.AudioSettingsRepository
import com.kingplay.core.model.repository.SleepTimer
import com.kingplay.core.model.sleep.SleepTimerState
import com.kingplay.core.model.sleep.sleepVolume
import com.kingplay.core.player.dsp.buildDspConfig
import com.kingplay.core.player.dsp.fadeVolume
import com.kingplay.core.player.dsp.replayGainDb
import com.kingplay.core.player.dsp.replayGainFromTags
import com.kingplay.core.player.logic.MediaIds
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Inject
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Connects the live [AudioSettings] to the player. Everything here runs on the main thread (the service scope), which
 * is the thread the player must be used from.
 *
 * - EQ, bass, loudness and ReplayGain: turned into a config for [KingPlayAudioProcessor].
 * - ReplayGain: read from the tags of the song that is playing.
 * - Fade: song-start and song-end volume ramp. It is a fade, not an overlap; overlapping two songs needs a second
 *   player and is not part of this step.
 * - Tempo and pitch: handed to the player as playback parameters.
 */
@OptIn(UnstableApi::class)
@ServiceScoped
class AudioEngineBinder @Inject constructor(
    private val player: Player,
    private val processor: KingPlayAudioProcessor,
    private val settingsRepository: AudioSettingsRepository,
    private val sleepTimer: SleepTimer,
) {
    private var settings: AudioSettings = settingsRepository.settings.value
    private var replayGain: ReplayGain? = null
    private var fadeScope: CoroutineScope? = null
    private var jobs = emptyList<Job>()
    private var fadeJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onTracksChanged(tracks: Tracks) = refreshReplayGain(tracks)

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            applyPlaybackParameters()
            refreshReplayGain(player.currentTracks)
            updateVolume()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) = updateVolume()
    }

    fun start(scope: CoroutineScope) {
        stop()
        fadeScope = scope
        player.addListener(listener)
        refreshReplayGain(player.currentTracks)
        jobs = listOf(
            scope.launch {
                settingsRepository.settings.collect {
                    settings = it
                    pushConfig()
                    updateVolume()
                }
            },
            scope.launch {
                settingsRepository.settings
                    .map { it.effects.tempo to it.effects.pitchSemitones }
                    .distinctUntilChanged()
                    .collect { applyPlaybackParameters() }
            },
            scope.launch { sleepTimer.state.collect { updateVolume() } },
        )
        updateVolume()
    }

    fun stop() {
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        fadeJob?.cancel()
        fadeJob = null
        player.removeListener(listener)
    }

    /**
     * Music uses the tempo and pitch from the equalizer. Video always starts at normal speed and pitch; the video
     * screen then changes speed itself, so the two never fight over it.
     */
    private fun applyPlaybackParameters() {
        val effects = settingsRepository.settings.value.effects
        val isAudio = MediaIds.parse(player.currentMediaItem?.mediaId) is MediaRef.Audio
        val wanted = if (isAudio) {
            PlaybackParameters(effects.tempo, semitonesToPitch(effects.pitchSemitones))
        } else {
            PlaybackParameters.DEFAULT
        }
        if (player.playbackParameters != wanted) player.playbackParameters = wanted
    }

    private fun pushConfig() {
        val gain = replayGainDb(settings.replayGainMode, replayGain, settings.replayGainPreampDb)
        processor.setConfig(buildDspConfig(settings, gain))
    }

    private fun refreshReplayGain(tracks: Tracks) {
        replayGain = readReplayGain(tracks)
        pushConfig()
    }

    /**
     * Owns the player's volume: the per-song fade and the sleep timer's fade-out multiply together. The ticker runs
     * only while something needs it; otherwise the volume stays at full.
     */
    private fun updateVolume() {
        fadeJob?.cancel()
        fadeJob = null
        val songFade = settings.fadeMs > 0 && MediaIds.parse(player.currentMediaItem?.mediaId) is MediaRef.Audio
        val sleeping = sleepTimer.state.value is SleepTimerState.Timed
        if (!songFade && !sleeping) {
            player.volume = 1f
            return
        }
        if (!player.isPlaying) return
        fadeJob = fadeScope?.launch {
            while (isActive) {
                player.volume = volumeNow(songFade)
                delay(FADE_TICK_MS)
            }
        }
    }

    private fun volumeNow(songFade: Boolean): Float {
        val duration = player.duration
        val fade = if (songFade && duration != C.TIME_UNSET) {
            fadeVolume(player.currentPosition, duration, settings.fadeMs)
        } else {
            1f
        }
        return fade * sleepVolume(sleepTimer.state.value, SystemClock.elapsedRealtime())
    }

    private fun readReplayGain(tracks: Tracks) = replayGainFromTags(
        buildList {
            for (group in tracks.groups) {
                if (group.type != C.TRACK_TYPE_AUDIO || !group.isSelected) continue
                for (index in 0 until group.length) {
                    group.getTrackFormat(index).metadata?.let { addAll(it.toNamedValues()) }
                }
            }
        },
    )

    private companion object {
        const val FADE_TICK_MS = 100L
        const val SEMITONES_PER_OCTAVE = 12.0

        fun semitonesToPitch(semitones: Float): Float = 2.0.pow(semitones / SEMITONES_PER_OCTAVE).toFloat()
    }
}

/**
 * The entries of this [Metadata] as a list. Written outside any `buildList` block on purpose: inside one, `get(index)`
 * resolves to the list being built and crashes with IndexOutOfBounds.
 */
@OptIn(UnstableApi::class)
internal fun Metadata.entryList(): List<Metadata.Entry> {
    val out = ArrayList<Metadata.Entry>(length())
    for (index in 0 until length()) out.add(get(index))
    return out
}

/** ID3, Vorbis and MP4 tags, flattened to name/value pairs. Only the kinds that can carry ReplayGain are read. */
@OptIn(UnstableApi::class)
internal fun Metadata.toNamedValues(): List<Pair<String, String>> {
    val out = ArrayList<Pair<String, String>>()
    for (entry in entryList()) {
        when (entry) {
            is TextInformationFrame -> {
                val name = entry.description
                val value = entry.values.firstOrNull()
                if (name != null && value != null) out.add(name to value)
            }
            is VorbisComment -> out.add(entry.key to entry.value)
            is InternalFrame -> out.add(entry.description to entry.text)
        }
    }
    return out
}
