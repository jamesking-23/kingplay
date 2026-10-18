package com.kingplay.feature.nowplaying.logic

import java.util.Locale

/** What the big square on Now Playing shows. */
enum class StageMode { ARTWORK, LYRICS, VISUALIZER }

/** Minutes offered by the sleep timer sheet, besides the "end of song" choices. */
val SleepPresetMinutes = listOf(5, 10, 15, 30, 45, 60, 90)

const val SleepExtendMinutes = 10

/** "4:05", "29:59", "1:02:03". Rounds up, so the display reaches 0:00 only when time is really up. */
fun formatRemaining(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) + 999) / 1_000
    val hours = totalSeconds / 3_600
    val minutes = totalSeconds % 3_600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/** How the bars fall when the bands drop: each frame keeps this share of the previous height. */
const val BarFallOff = 0.88f

/** Next bar heights from the newest analysis [latest]: rise at once, fall smoothly. Writes into [shown]. */
fun smoothBars(shown: FloatArray, latest: FloatArray) {
    for (i in shown.indices) {
        val target = latest.getOrElse(i) { 0f }
        shown[i] = maxOf(target, shown[i] * BarFallOff)
    }
}
