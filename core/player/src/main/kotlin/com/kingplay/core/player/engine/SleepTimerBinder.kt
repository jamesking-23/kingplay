package com.kingplay.core.player.engine

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.kingplay.core.model.sleep.SleepStep
import com.kingplay.core.model.sleep.SleepTimerState
import com.kingplay.core.model.sleep.sleepTick
import com.kingplay.core.model.sleep.sleepTrackFinished
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Carries out the sleep timer: pauses the player when the countdown ends or the chosen number of songs has played.
 * The fade-out near the end is applied by [AudioEngineBinder], which owns the player's volume.
 */
@OptIn(UnstableApi::class)
@ServiceScoped
class SleepTimerBinder @Inject constructor(
    private val player: Player,
    private val timer: DefaultSleepTimer,
) {
    private var jobs = emptyList<Job>()
    private var tickJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val finished = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            if (finished) {
                apply(sleepTrackFinished(timer.state.value))
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            // The queue ran out before the timer did; nothing is left to stop.
            if (playbackState == Player.STATE_ENDED && timer.state.value is SleepTimerState.AfterTracks) {
                timer.set(SleepTimerState.Off)
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM &&
                timer.state.value is SleepTimerState.AfterTracks
            ) {
                apply(sleepTrackFinished(timer.state.value))
            }
        }
    }

    fun start(scope: CoroutineScope) {
        stop()
        player.addListener(listener)
        jobs = listOf(
            scope.launch {
                timer.state.collect { state ->
                    // Stop exactly at the end of the song when it is the last one, instead of a moment into the next.
                    (player as? ExoPlayer)?.pauseAtEndOfMediaItems =
                        state is SleepTimerState.AfterTracks && state.remaining == 1
                    tickJob?.cancel()
                    tickJob = if (state is SleepTimerState.Timed) launch { countdown() } else null
                }
            },
        )
    }

    fun stop() {
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        tickJob?.cancel()
        tickJob = null
        player.removeListener(listener)
        (player as? ExoPlayer)?.pauseAtEndOfMediaItems = false
    }

    private suspend fun countdown() {
        while (true) {
            apply(sleepTick(timer.state.value, SystemClock.elapsedRealtime()))
            delay(TICK_MS)
        }
    }

    private fun apply(step: SleepStep) {
        if (step.state != timer.state.value) timer.set(step.state)
        if (step.pause) player.pause()
    }

    private companion object {
        const val TICK_MS = 500L
    }
}
