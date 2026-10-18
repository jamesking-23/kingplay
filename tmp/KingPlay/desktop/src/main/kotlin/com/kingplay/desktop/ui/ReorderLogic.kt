package com.kingplay.desktop.ui

import kotlin.math.roundToInt

/** Where a row dragged [dragPx] pixels ends up: whole rows passed, kept inside the list. */
fun dragTarget(from: Int, dragPx: Float, rowPx: Float, count: Int): Int {
    if (count <= 0) return from
    if (rowPx <= 0f) return from.coerceIn(0, count - 1)
    return (from + (dragPx / rowPx).roundToInt()).coerceIn(0, count - 1)
}

/** The neighbour a "Move up" (-1) or "Move down" (+1) menu item swaps with, or null at the ends of the list. */
fun stepTarget(position: Int, step: Int, count: Int): Int? =
    (position + step).takeIf { position in 0 until count && it in 0 until count }
