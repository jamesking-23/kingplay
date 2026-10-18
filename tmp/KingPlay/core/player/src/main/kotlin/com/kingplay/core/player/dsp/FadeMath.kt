package com.kingplay.core.player.dsp

import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Player volume (0..1) for a song at [positionMs] when [fadeMs] of fade in and fade out are wanted.
 * Short songs fade for at most half their length. Equal-power (sine) curve, so the fade sounds even.
 */
fun fadeVolume(positionMs: Long, durationMs: Long, fadeMs: Int): Float {
    if (fadeMs <= 0 || durationMs <= 0) return 1f
    val fade = min(fadeMs.toLong(), durationMs / 2).coerceAtLeast(1)
    val position = positionMs.coerceIn(0, durationMs)
    val fadeIn = if (position < fade) curve(position.toDouble() / fade) else 1.0
    val remaining = durationMs - position
    val fadeOut = if (remaining < fade) curve(remaining.toDouble() / fade) else 1.0
    return min(fadeIn, fadeOut).toFloat().coerceIn(0f, 1f)
}

private fun curve(fraction: Double): Double = sin(fraction * PI / 2)
