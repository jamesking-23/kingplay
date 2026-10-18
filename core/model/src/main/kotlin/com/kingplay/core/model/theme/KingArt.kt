package com.kingplay.core.model.theme

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Colours of the "King Art" design (hand-drawn paper), as 0xAARRGGBB so Android and the PC app share one palette.
 * Light is pencil on paper; dark is chalk on charcoal paper.
 */
object KingArtPalette {
    const val PAPER = 0xFFFAF8F5
    const val INK = 0xFF2B2B2B
    const val RED = 0xFFE57373
    const val YELLOW = 0xFFFFF176

    /** Slightly darker paper, used for cards and fields. */
    const val PAPER_SHADE = 0xFFEFEBE3
    const val PAPER_SHADE_HIGH = 0xFFE6E1D8
    const val INK_SOFT = 0xFF6B6B6B

    const val DARK_PAPER = 0xFF1F1D1A
    const val DARK_PAPER_SHADE = 0xFF2A2825
    const val DARK_PAPER_SHADE_HIGH = 0xFF34312D
    const val DARK_INK = 0xFFEDE8DF
    const val DARK_INK_SOFT = 0xFFA9A398
    const val DARK_RED = 0xFFF28B8B
    const val DARK_YELLOW_CONTAINER = 0xFF55501F
}

/**
 * A wobbly outline for a rounded rectangle, as a closed loop of points. The same seed always gives the same
 * wobble, so a shape does not shimmer between redraws.
 */
object Sketch {
    /** Small deterministic random numbers in [0, 1). */
    class Random(seed: Int) {
        private var state = seed
        fun next(): Float {
            state += 0x6D2B79F5
            var t = (state xor (state ushr 15)) * (1 or state)
            t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
            return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble().div(4294967296.0).toFloat()
        }

        /** In [-1, 1). */
        fun signed(): Float = next() * 2f - 1f
    }

    /** x0,y0,x1,y1,... around the rectangle, clockwise from the top edge. Needs at least 3 points to be useful. */
    fun roundRect(
        width: Float,
        height: Float,
        corner: Float,
        seed: Int,
        wobble: Float,
        step: Float,
    ): FloatArray {
        val w = max(width, 1f)
        val h = max(height, 1f)
        val c = min(corner, min(w, h) / 2f).coerceAtLeast(0f)
        val random = Random(seed)
        val out = ArrayList<Float>()

        fun point(x: Float, y: Float) {
            out.add((x + random.signed() * wobble).coerceIn(-wobble * 2, w + wobble * 2))
            out.add((y + random.signed() * wobble).coerceIn(-wobble * 2, h + wobble * 2))
        }

        fun edge(x1: Float, y1: Float, x2: Float, y2: Float) {
            val count = max(1, (hypot(x2 - x1, y2 - y1) / step).roundToInt())
            for (i in 0 until count) {
                val t = i.toFloat() / count
                point(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t)
            }
        }

        fun arc(cx: Float, cy: Float, fromDeg: Float) {
            for (i in 0..CORNER_STEPS) {
                val a = (fromDeg + i * (90f / CORNER_STEPS)) * PI.toFloat() / 180f
                point(cx + cos(a) * c, cy + sin(a) * c)
            }
        }

        edge(c, 0f, w - c, 0f)
        arc(w - c, c, -90f)
        edge(w, c, w, h - c)
        arc(w - c, h - c, 0f)
        edge(w - c, h, c, h)
        arc(c, h - c, 90f)
        edge(0f, h - c, 0f, c)
        arc(c, c, 180f)
        return out.toFloatArray()
    }

    /** A pencil line from (x1,y1) to (x2,y2) with a slight waver, as x0,y0,x1,y1,... */
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, seed: Int, wobble: Float, step: Float): FloatArray {
        val dx = x2 - x1
        val dy = y2 - y1
        val d = hypot(dx, dy)
        if (d < 0.5f) return floatArrayOf(x1, y1, x2, y2)
        val px = -dy / d
        val py = dx / d
        val random = Random(seed)
        val n = max(2, (d / step).roundToInt())
        val out = FloatArray((n + 1) * 2)
        for (i in 0..n) {
            val t = i.toFloat() / n
            val o = (if (i == 0 || i == n) wobble * 0.4f else wobble) * random.signed()
            out[i * 2] = x1 + dx * t + px * o
            out[i * 2 + 1] = y1 + dy * t + py * o
        }
        return out
    }

    private const val CORNER_STEPS = 3
}
