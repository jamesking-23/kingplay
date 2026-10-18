package com.kingplay.core.ui.format

import java.util.Locale

/** m:ss, or h:mm:ss from one hour up. Negative values show as 0:00. */
fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / MS_PER_SECOND).coerceAtLeast(0)
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/** Long totals such as an album length: "42 min" or "3 h 05 min". */
fun formatTotalDuration(ms: Long): String {
    val totalMinutes = (ms / MS_PER_SECOND).coerceAtLeast(0) / SECONDS_PER_MINUTE
    val hours = totalMinutes / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d h %02d min", hours, minutes)
    } else {
        "$minutes min"
    }
}

/** "FLAC · 44.1 kHz · 920 kbps", skipping parts that are unknown (0). */
fun formatQuality(format: String, sampleRateHz: Int, bitrateKbps: Int): String {
    val parts = buildList {
        if (format.isNotBlank()) add(format.uppercase(Locale.ROOT))
        if (sampleRateHz > 0) add(formatSampleRate(sampleRateHz))
        if (bitrateKbps > 0) add("$bitrateKbps kbps")
    }
    return parts.joinToString(" · ")
}

/** 44100 -> "44.1 kHz", 48000 -> "48 kHz", 96000 -> "96 kHz". */
fun formatSampleRate(hz: Int): String {
    val khz = hz / HZ_PER_KHZ.toDouble()
    val text = if (khz % 1.0 == 0.0) khz.toInt().toString() else String.format(Locale.ROOT, "%.1f", khz)
    return "$text kHz"
}

/** 0.0..1.0 progress, safe for unknown (0) durations and positions past the end. */
fun progressFraction(positionMs: Long, durationMs: Long): Float =
    if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

private const val MS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
private const val MINUTES_PER_HOUR = 60L
private const val HZ_PER_KHZ = 1_000
