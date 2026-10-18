package com.kingplay.core.model.sleep

import kotlin.math.PI
import kotlin.math.sin

/** What the user asked for. "End of this song" is [Tracks] with a count of 1. */
sealed interface SleepTimerSpec {
    data class Minutes(val minutes: Int) : SleepTimerSpec

    data class Tracks(val count: Int) : SleepTimerSpec
}

sealed interface SleepTimerState {
    data object Off : SleepTimerState

    /** Stops at [endsAtMs] on the monotonic clock (elapsed realtime). */
    data class Timed(val endsAtMs: Long, val totalMs: Long) : SleepTimerState

    /** Stops when [remaining] more songs have finished. */
    data class AfterTracks(val remaining: Int) : SleepTimerState
}

/** What to do after a clock tick or a finished song. */
data class SleepStep(val state: SleepTimerState, val pause: Boolean)

const val SLEEP_FADE_MS = 30_000L
const val MAX_SLEEP_MINUTES = 720
const val MAX_SLEEP_TRACKS = 99
private const val MS_PER_MINUTE = 60_000L

fun startSleepTimer(spec: SleepTimerSpec, nowMs: Long): SleepTimerState = when (spec) {
    is SleepTimerSpec.Minutes -> {
        val total = spec.minutes.coerceIn(1, MAX_SLEEP_MINUTES) * MS_PER_MINUTE
        SleepTimerState.Timed(endsAtMs = nowMs + total, totalMs = total)
    }
    is SleepTimerSpec.Tracks -> SleepTimerState.AfterTracks(spec.count.coerceIn(1, MAX_SLEEP_TRACKS))
}

fun remainingMs(state: SleepTimerState, nowMs: Long): Long? =
    (state as? SleepTimerState.Timed)?.let { (it.endsAtMs - nowMs).coerceAtLeast(0L) }

/** Adds time to a running countdown. Does nothing for the other states. */
fun extendSleepTimer(state: SleepTimerState, minutes: Int): SleepTimerState {
    if (state !is SleepTimerState.Timed) return state
    val added = minutes.coerceIn(1, MAX_SLEEP_MINUTES) * MS_PER_MINUTE
    return state.copy(endsAtMs = state.endsAtMs + added, totalMs = state.totalMs + added)
}

fun sleepTick(state: SleepTimerState, nowMs: Long): SleepStep {
    val left = remainingMs(state, nowMs) ?: return SleepStep(state, pause = false)
    return if (left <= 0L) SleepStep(SleepTimerState.Off, pause = true) else SleepStep(state, pause = false)
}

/** Call once each time a song finishes. */
fun sleepTrackFinished(state: SleepTimerState): SleepStep {
    if (state !is SleepTimerState.AfterTracks) return SleepStep(state, pause = false)
    val left = state.remaining - 1
    return if (left <= 0) {
        SleepStep(SleepTimerState.Off, pause = true)
    } else {
        SleepStep(SleepTimerState.AfterTracks(left), pause = false)
    }
}

/**
 * Player volume (0..1) while a countdown runs: full until the last [SLEEP_FADE_MS], then a smooth fade to silence.
 * Short timers fade over at most half their length.
 */
fun sleepVolume(state: SleepTimerState, nowMs: Long): Float {
    val timed = state as? SleepTimerState.Timed ?: return 1f
    val fade = minOf(SLEEP_FADE_MS, timed.totalMs / 2).coerceAtLeast(1L)
    val left = (timed.endsAtMs - nowMs).coerceAtLeast(0L)
    if (left >= fade) return 1f
    return sin(left.toDouble() / fade * PI / 2).toFloat().coerceIn(0f, 1f)
}
