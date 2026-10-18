package com.kingplay.feature.equalizer.logic

import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

/** "31", "250", "1k", "16k": short enough to sit under a slider. */
fun bandLabel(hz: Int): String = if (hz >= 1_000) "${hz / 1_000}k" else hz.toString()

/** "+3.5 dB", "-2 dB", "0 dB". Whole numbers drop the decimal. */
fun formatDb(db: Float): String {
    val rounded = (db * 2).roundToInt() / 2f
    val whole = rounded == rounded.toInt().toFloat()
    val text = if (whole) rounded.toInt().toString() else String.format(Locale.ROOT, "%.1f", rounded)
    return if (rounded > 0f) "+$text dB" else "$text dB"
}

/** "1.25x" style. */
fun formatTempo(tempo: Float): String =
    String.format(Locale.ROOT, "%.2fx", tempo).replace(".00x", "x").replace(Regex("0x$"), "x")

fun formatSemitones(semitones: Float): String {
    val whole = semitones.roundToInt()
    return if (whole > 0) "+$whole" else whole.toString()
}

/** Snaps a slider value to the nearest 0.5 dB so that the stored curve stays tidy. */
fun snapDb(value: Float): Float = (value * 2).roundToInt() / 2f

/** Position 0..1 of [hz] on a log scale between [minHz] and [maxHz], for drawing the response curve. */
fun logPosition(hz: Double, minHz: Double = 20.0, maxHz: Double = 20_000.0): Double =
    ((ln(hz) - ln(minHz)) / (ln(maxHz) - ln(minHz))).coerceIn(0.0, 1.0)

/** Frequencies to sample for the curve: evenly spaced on the log axis. */
fun curveFrequencies(points: Int = 96, minHz: Double = 20.0, maxHz: Double = 20_000.0): List<Double> =
    List(points) { i -> minHz * Math.pow(maxHz / minHz, i / (points - 1).toDouble()) }

/** True when every band sits at 0 dB, which makes "reset" pointless. */
fun isFlat(bands: List<Float>, preampDb: Float): Boolean = abs(preampDb) < 0.01f && bands.all { abs(it) < 0.01f }
