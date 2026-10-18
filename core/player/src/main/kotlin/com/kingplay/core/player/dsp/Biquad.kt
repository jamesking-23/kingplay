package com.kingplay.core.player.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Normalised second-order filter: y = b0 x + z1; z1 = b1 x - a1 y + z2; z2 = b2 x - a2 y. */
class BiquadCoefficients(
    val b0: Double,
    val b1: Double,
    val b2: Double,
    val a1: Double,
    val a2: Double,
) {
    /** Filter gain in dB at [freqHz] for sample rate [sampleRate]. */
    fun magnitudeDb(freqHz: Double, sampleRate: Double): Double {
        val w = 2 * PI * freqHz / sampleRate
        val cosW = cos(w)
        val sinW = sin(w)
        val cos2W = cos(2 * w)
        val sin2W = sin(2 * w)
        val numRe = b0 + b1 * cosW + b2 * cos2W
        val numIm = -(b1 * sinW + b2 * sin2W)
        val denRe = 1 + a1 * cosW + a2 * cos2W
        val denIm = -(a1 * sinW + a2 * sin2W)
        val num = numRe * numRe + numIm * numIm
        val den = denRe * denRe + denIm * denIm
        return 10 * log10(num / den)
    }

    val isIdentity: Boolean get() = b0 == 1.0 && b1 == 0.0 && b2 == 0.0 && a1 == 0.0 && a2 == 0.0

    companion object {
        val Identity = BiquadCoefficients(1.0, 0.0, 0.0, 0.0, 0.0)
    }
}

/** Filter designs from the RBJ audio EQ cookbook. */
object BiquadDesign {
    /** Bell filter centred on [centerHz]. A gain of exactly 0 dB returns the identity filter. */
    fun peaking(sampleRate: Double, centerHz: Double, q: Double, gainDb: Double): BiquadCoefficients {
        if (gainDb == 0.0) return BiquadCoefficients.Identity
        val a = 10.0.pow(gainDb / 40)
        val w0 = 2 * PI * centerHz / sampleRate
        val alpha = sin(w0) / (2 * q)
        val cosW = cos(w0)
        val a0 = 1 + alpha / a
        return BiquadCoefficients(
            b0 = (1 + alpha * a) / a0,
            b1 = (-2 * cosW) / a0,
            b2 = (1 - alpha * a) / a0,
            a1 = (-2 * cosW) / a0,
            a2 = (1 - alpha / a) / a0,
        )
    }

    /** Shelf that boosts everything below [cornerHz] by [gainDb]. Shelf slope 1 (no overshoot). */
    fun lowShelf(sampleRate: Double, cornerHz: Double, gainDb: Double): BiquadCoefficients {
        if (gainDb == 0.0) return BiquadCoefficients.Identity
        val a = 10.0.pow(gainDb / 40)
        val w0 = 2 * PI * cornerHz / sampleRate
        val cosW = cos(w0)
        val sqrtA = sqrt(a)
        // With slope S = 1 the (1/S - 1) term vanishes and alpha = sin(w0) / sqrt(2).
        val alpha = sin(w0) / sqrt(2.0)
        val a0 = (a + 1) + (a - 1) * cosW + 2 * sqrtA * alpha
        return BiquadCoefficients(
            b0 = a * ((a + 1) - (a - 1) * cosW + 2 * sqrtA * alpha) / a0,
            b1 = 2 * a * ((a - 1) - (a + 1) * cosW) / a0,
            b2 = a * ((a + 1) - (a - 1) * cosW - 2 * sqrtA * alpha) / a0,
            a1 = -2 * ((a - 1) + (a + 1) * cosW) / a0,
            a2 = ((a + 1) + (a - 1) * cosW - 2 * sqrtA * alpha) / a0,
        )
    }
}

/** Running state of one filter on one channel (transposed direct form II). */
class BiquadState {
    private var z1 = 0.0
    private var z2 = 0.0

    fun process(x: Double, c: BiquadCoefficients): Double {
        val y = c.b0 * x + z1
        z1 = c.b1 * x - c.a1 * y + z2
        z2 = c.b2 * x - c.a2 * y
        return y
    }

    fun reset() {
        z1 = 0.0
        z2 = 0.0
    }
}
