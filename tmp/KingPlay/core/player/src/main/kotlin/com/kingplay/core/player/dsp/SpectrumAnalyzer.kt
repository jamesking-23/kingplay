package com.kingplay.core.player.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

const val SPECTRUM_BANDS = 32
const val SPECTRUM_FFT_SIZE = 1_024
private const val HOP = SPECTRUM_FFT_SIZE / 2
private const val LOW_HZ = 40.0
private const val HIGH_HZ = 16_000.0
private const val FLOOR_DB = -70.0
private const val RELEASE = 0.82f

/**
 * Turns the audio that is playing into [bandCount] loudness values (0..1, log-spaced from 40 Hz to 16 kHz).
 * Feed it interleaved float audio; [onBands] gets a fresh array about every 10 ms. Levels rise at once and fall
 * smoothly, which is what makes the bars look like music rather than noise. Audio thread only.
 */
class SpectrumAnalyzer(
    private val sampleRate: Int,
    private val channels: Int,
    val bandCount: Int = SPECTRUM_BANDS,
    private val onBands: (FloatArray) -> Unit,
) {
    private val fft = Fft(SPECTRUM_FFT_SIZE)
    private val window = FloatArray(SPECTRUM_FFT_SIZE) {
        (0.5 - 0.5 * cos(2 * PI * it / (SPECTRUM_FFT_SIZE - 1))).toFloat()
    }
    private val ring = FloatArray(SPECTRUM_FFT_SIZE)
    private val re = FloatArray(SPECTRUM_FFT_SIZE)
    private val im = FloatArray(SPECTRUM_FFT_SIZE)
    private val smoothed = FloatArray(bandCount)
    private val edges = bandEdges(bandCount, sampleRate)
    private var filled = 0
    private var sinceLast = 0
    private var head = 0

    /** [frames] frames of interleaved samples in -1..1. */
    fun process(samples: FloatArray, frames: Int) {
        var index = 0
        for (frame in 0 until frames) {
            var sum = 0f
            for (c in 0 until channels) sum += samples[index++]
            ring[head] = sum / channels
            head = (head + 1) % SPECTRUM_FFT_SIZE
            if (filled < SPECTRUM_FFT_SIZE) filled++
            if (++sinceLast >= HOP && filled == SPECTRUM_FFT_SIZE) {
                sinceLast = 0
                analyze()
            }
        }
    }

    private fun analyze() {
        for (i in 0 until SPECTRUM_FFT_SIZE) {
            re[i] = ring[(head + i) % SPECTRUM_FFT_SIZE] * window[i]
            im[i] = 0f
        }
        fft.transform(re, im)
        val out = FloatArray(bandCount)
        for (band in 0 until bandCount) {
            var peak = 0f
            for (bin in edges[band]..edges[band + 1]) {
                peak = max(peak, sqrt(re[bin] * re[bin] + im[bin] * im[bin]))
            }
            val level = levelOf(peak)
            smoothed[band] = max(level, smoothed[band] * RELEASE)
            out[band] = smoothed[band]
        }
        onBands(out)
    }
}

/** Magnitude of a Hann-windowed FFT bin to 0..1. A full-scale sine reads about 1. */
internal fun levelOf(magnitude: Float): Float {
    val normalized = magnitude / (SPECTRUM_FFT_SIZE / 4f)
    val db = 20 * log10(normalized.toDouble() + 1e-9)
    return ((db - FLOOR_DB) / -FLOOR_DB).coerceIn(0.0, 1.0).toFloat()
}

/**
 * First FFT bin of each band plus a final entry, so band `i` covers bins `edges[i]..edges[i + 1]`.
 * Neighbouring bands share a boundary bin; the lowest bands never get fewer than one bin.
 */
internal fun bandEdges(bandCount: Int, sampleRate: Int): IntArray {
    val binHz = sampleRate.toDouble() / SPECTRUM_FFT_SIZE
    val top = minOf(HIGH_HZ, sampleRate * 0.45)
    val maxBin = SPECTRUM_FFT_SIZE / 2 - 1
    val edges = IntArray(bandCount + 1)
    for (i in 0..bandCount) {
        val hz = LOW_HZ * (top / LOW_HZ).pow(i.toDouble() / bandCount)
        edges[i] = (hz / binHz).toInt().coerceIn(1, maxBin)
    }
    for (i in 1..bandCount) if (edges[i] < edges[i - 1]) edges[i] = edges[i - 1]
    return edges
}
