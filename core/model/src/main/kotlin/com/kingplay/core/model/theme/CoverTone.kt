package com.kingplay.core.model.theme

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Picks the colour a cover is "mostly" and tunes it so it reads well as an app colour. Pure maths on ARGB ints, so
 * the PC app can share it and it can be tested without a screen.
 */
object CoverTone {
    private const val HUE_BUCKETS = 12
    private const val MIN_ALPHA = 128
    private const val MIN_WEIGHT = 0.15f
    private const val MIN_SATURATION = 0.45f
    private const val MAX_SATURATION = 0.9f
    private const val DARK_LIGHTNESS = 0.68f
    private const val LIGHT_LIGHTNESS = 0.38f
    private const val FULL_CIRCLE = 360f

    /** The most vivid hue in [pixels] (ARGB), or null when the picture is grey, black or white. */
    fun dominant(pixels: IntArray): Int? {
        val weight = FloatArray(HUE_BUCKETS)
        val r = FloatArray(HUE_BUCKETS)
        val g = FloatArray(HUE_BUCKETS)
        val b = FloatArray(HUE_BUCKETS)
        for (p in pixels) {
            if ((p ushr 24) < MIN_ALPHA) continue
            val red = (p shr 16) and 0xFF
            val green = (p shr 8) and 0xFF
            val blue = p and 0xFF
            val hsl = toHsl(red, green, blue)
            val vivid = hsl[1] * (1f - abs(2f * hsl[2] - 1f))
            if (vivid <= 0f) continue
            val bucket = ((hsl[0] / FULL_CIRCLE) * HUE_BUCKETS).toInt().coerceIn(0, HUE_BUCKETS - 1)
            weight[bucket] += vivid
            r[bucket] += red * vivid
            g[bucket] += green * vivid
            b[bucket] += blue * vivid
        }
        val best = weight.indices.maxByOrNull { weight[it] } ?: return null
        if (weight[best] / pixels.size.coerceAtLeast(1) < MIN_WEIGHT * MIN_WEIGHT) return null
        val w = weight[best]
        return (0xFF shl 24) or ((r[best] / w).toInt() shl 16) or ((g[best] / w).toInt() shl 8) or (b[best] / w).toInt()
    }

    /** Keeps the hue of [argb] but moves saturation and lightness to a range that has contrast on the background. */
    fun tune(argb: Int, dark: Boolean): Int {
        val hsl = toHsl((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
        val s = hsl[1].coerceIn(MIN_SATURATION, MAX_SATURATION)
        val l = if (dark) DARK_LIGHTNESS else LIGHT_LIGHTNESS
        return fromHsl(hsl[0], s, l)
    }

    internal fun toHsl(r: Int, g: Int, b: Int): FloatArray {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val mx = max(rf, max(gf, bf))
        val mn = min(rf, min(gf, bf))
        val l = (mx + mn) / 2f
        val d = mx - mn
        if (d == 0f) return floatArrayOf(0f, 0f, l)
        val s = d / (1f - abs(2f * l - 1f))
        val h = when (mx) {
            rf -> ((gf - bf) / d) % 6f
            gf -> (bf - rf) / d + 2f
            else -> (rf - gf) / d + 4f
        } * 60f
        return floatArrayOf(if (h < 0f) h + FULL_CIRCLE else h, s, l)
    }

    internal fun fromHsl(h: Float, s: Float, l: Float): Int {
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r, g, b) = when ((h / 60f).toInt().coerceIn(0, 5)) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun ch(v: Float) = ((v + m) * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }
}
