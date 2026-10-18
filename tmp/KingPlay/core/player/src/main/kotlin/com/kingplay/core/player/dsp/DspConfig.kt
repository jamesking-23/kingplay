package com.kingplay.core.player.dsp

import com.kingplay.core.model.AudioSettings
import com.kingplay.core.model.EqPreset
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.tanh

/** Largest low-shelf boost the bass slider can ask for. */
const val MAX_BASS_BOOST_DB = 9.0
const val BASS_SHELF_HZ = 100.0

/** Graphic EQ bandwidth: one octave per band. */
const val BAND_Q = 1.414

/**
 * What the audio thread works from: plain numbers, built once per settings change.
 * [outputGain] is linear and already includes preamp, loudness, ReplayGain and the headroom reduction.
 */
class DspConfig(
    val bandGainsDb: DoubleArray,
    val bassShelfDb: Double,
    val outputGain: Float,
) {
    /** True when processing would change nothing, so the processor can pass audio through untouched. */
    val bypass: Boolean
        get() = bandGainsDb.all { it == 0.0 } && bassShelfDb == 0.0 && abs(outputGain - 1f) < UNITY_TOLERANCE

    companion object {
        private const val UNITY_TOLERANCE = 1e-5f
        val Bypass = DspConfig(DoubleArray(EqPreset.BAND_COUNT), 0.0, 1f)
    }
}

/**
 * Turns user settings plus the current song's ReplayGain adjustment into [DspConfig].
 * The EQ switch covers the bands and the preamp; bass boost, loudness and ReplayGain work on their own.
 */
fun buildDspConfig(settings: AudioSettings, replayGainDb: Float, referenceSampleRate: Double = 48_000.0): DspConfig {
    val bands = DoubleArray(EqPreset.BAND_COUNT) { index ->
        if (settings.eqEnabled) settings.bandGainsDb[index].toDouble() else 0.0
    }
    val preamp = if (settings.eqEnabled) settings.preampDb.toDouble() else 0.0
    val bass = settings.effects.bassBoostStrength.toDouble() / AudioSettings.MAX_BASS_BOOST * MAX_BASS_BOOST_DB
    val loudness = settings.effects.loudnessGainMb / MILLIBELS_PER_DB
    val headroom = if (settings.autoHeadroom) peakBoostDb(bands, bass, referenceSampleRate) else 0.0
    val totalDb = preamp + loudness + replayGainDb - headroom
    return DspConfig(bands, bass, dbToLinear(totalDb).toFloat())
}

private const val MILLIBELS_PER_DB = 100.0

fun dbToLinear(db: Double): Double = 10.0.pow(db / 20)

fun linearToDb(linear: Double): Double = 20 * log10(linear)

/** Highest gain of the whole curve, measured on a log grid, never below 0. The EQ is lowered by this much. */
fun peakBoostDb(bandGainsDb: DoubleArray, bassShelfDb: Double, sampleRate: Double): Double {
    if (bandGainsDb.all { it <= 0.0 } && bassShelfDb <= 0.0) return 0.0
    val filters = designFilters(bandGainsDb, bassShelfDb, sampleRate)
    var peak = 0.0
    var freq = GRID_START_HZ
    while (freq <= GRID_END_HZ) {
        var total = 0.0
        for (filter in filters) total += filter.magnitudeDb(freq, sampleRate)
        peak = max(peak, total)
        freq *= GRID_STEP
    }
    return peak
}

private const val GRID_START_HZ = 20.0
private const val GRID_END_HZ = 20_000.0
private const val GRID_STEP = 1.05

/** The active filters for a curve: ten bells and the bass shelf. Bands too close to the Nyquist limit are skipped. */
fun designFilters(bandGainsDb: DoubleArray, bassShelfDb: Double, sampleRate: Double): List<BiquadCoefficients> {
    val filters = ArrayList<BiquadCoefficients>(EqPreset.BAND_COUNT + 1)
    if (bassShelfDb != 0.0) filters += BiquadDesign.lowShelf(sampleRate, BASS_SHELF_HZ, bassShelfDb)
    for (index in 0 until EqPreset.BAND_COUNT) {
        val gain = bandGainsDb[index]
        val center = EqPreset.CENTER_FREQUENCIES_HZ[index].toDouble()
        if (gain != 0.0 && center < sampleRate * NYQUIST_GUARD) {
            filters += BiquadDesign.peaking(sampleRate, center, BAND_Q, gain)
        }
    }
    return filters
}

private const val NYQUIST_GUARD = 0.45

/** Combined response of the curve at [freqHz]. Used to draw the EQ graph. */
fun responseDb(bandGainsDb: DoubleArray, bassShelfDb: Double, freqHz: Double, sampleRate: Double = 48_000.0): Double =
    designFilters(bandGainsDb, bassShelfDb, sampleRate).sumOf { it.magnitudeDb(freqHz, sampleRate) }

/**
 * Soft clipper: untouched up to [LIMIT_KNEE], then bends smoothly towards 1.0 so boosted peaks never wrap around or
 * hard-clip. It is a safety net, not a mastering limiter.
 */
fun softLimit(x: Float): Float {
    val magnitude = abs(x)
    if (magnitude <= LIMIT_KNEE) return x
    val over = (magnitude - LIMIT_KNEE) / (1f - LIMIT_KNEE)
    val limited = LIMIT_KNEE + (1f - LIMIT_KNEE) * tanh(over)
    return if (x < 0) -limited else limited
}

private const val LIMIT_KNEE = 0.9f
