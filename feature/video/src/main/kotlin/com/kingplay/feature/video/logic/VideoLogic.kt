package com.kingplay.feature.video.logic

import java.util.Locale
import kotlin.math.abs

/** Where a tap landed, as a third of the screen width. */
enum class TapZone { LEFT, CENTER, RIGHT }

enum class DragAxis { HORIZONTAL, VERTICAL }

/** How the picture fills the screen. */
enum class VideoResize { FIT, ZOOM, FILL }

const val DOUBLE_TAP_SEEK_MS = 10_000L
const val SEEK_SPAN_MS = 120_000L
const val CONTROLS_HIDE_MS = 3_500L
const val RESUME_MIN_MS = 5_000L
const val RESUME_END_MARGIN_MS = 10_000L
private const val SIDE_ZONE = 0.35f
private const val LEVEL_RANGE = 1.2f

val SpeedSteps = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f)

/** Subtitle text sizes as a fraction of the picture height, smallest first. */
val SubtitleSizes = listOf(0.04f, 0.0533f, 0.07f, 0.09f)
const val DEFAULT_SUBTITLE_SIZE_INDEX = 1

fun tapZone(x: Float, width: Float): TapZone {
    if (width <= 0f) return TapZone.CENTER
    val fraction = x / width
    return when {
        fraction < SIDE_ZONE -> TapZone.LEFT
        fraction > 1f - SIDE_ZONE -> TapZone.RIGHT
        else -> TapZone.CENTER
    }
}

/** Milliseconds to jump for a double tap: back on the left, forward on the right, none in the middle. */
fun doubleTapSeekMs(zone: TapZone): Long = when (zone) {
    TapZone.LEFT -> -DOUBLE_TAP_SEEK_MS
    TapZone.RIGHT -> DOUBLE_TAP_SEEK_MS
    TapZone.CENTER -> 0L
}

/** Picks the direction of a drag once it has moved past [slopPx]; null while it is still too small to tell. */
fun dragAxis(dx: Float, dy: Float, slopPx: Float): DragAxis? = when {
    abs(dx) < slopPx && abs(dy) < slopPx -> null
    abs(dx) >= abs(dy) -> DragAxis.HORIZONTAL
    else -> DragAxis.VERTICAL
}

/** Position after dragging [dx] pixels across a [width]-pixel screen. A full-width drag moves about two minutes. */
fun swipeSeekTarget(startMs: Long, dx: Float, width: Float, durationMs: Long): Long {
    if (width <= 0f) return startMs
    val span = if (durationMs in 1 until SEEK_SPAN_MS) durationMs else SEEK_SPAN_MS
    val target = startMs + (dx / width * span).toLong()
    return if (durationMs > 0) target.coerceIn(0L, durationMs) else target.coerceAtLeast(0L)
}

/** Brightness or volume (0..1) after dragging [dy] pixels down a [height]-pixel screen; dragging up raises it. */
fun swipeLevel(start: Float, dy: Float, height: Float): Float {
    if (height <= 0f) return start
    return (start - dy / height * LEVEL_RANGE).coerceIn(0f, 1f)
}

/** Where to start playing a video that was stopped at [resumeMs]: not at all near the very start or the very end. */
fun resumeStartMs(resumeMs: Long, durationMs: Long): Long = when {
    resumeMs < RESUME_MIN_MS -> 0L
    durationMs > 0 && resumeMs > durationMs - RESUME_END_MARGIN_MS -> 0L
    else -> resumeMs
}

fun nextSpeed(current: Float): Float {
    val index = SpeedSteps.indexOfFirst { abs(it - current) < 0.01f }
    return SpeedSteps[(index + 1) % SpeedSteps.size].takeIf { index >= 0 } ?: 1f
}

fun formatSpeed(speed: Float): String =
    String.format(Locale.ROOT, "%.2f", speed).trimEnd('0').trimEnd('.') + "x"

fun nextResize(current: VideoResize): VideoResize =
    VideoResize.entries[(current.ordinal + 1) % VideoResize.entries.size]

/** A wide picture is best watched with the phone turned sideways. */
fun prefersLandscape(width: Int, height: Int): Boolean = width > 0 && height > 0 && width >= height

fun channelLabel(channels: Int): String = when (channels) {
    1 -> "Mono"
    2 -> "Stereo"
    6 -> "5.1"
    8 -> "7.1"
    else -> if (channels > 0) "$channels ch" else ""
}

/**
 * Name for an audio or subtitle track. Uses the file's own label, then the language in the user's language, then
 * "Track N". Audio tracks add their channel layout.
 */
fun trackLabel(label: String?, language: String?, channels: Int, index: Int, locale: Locale, fallback: String): String {
    val languageName = language
        ?.takeIf { it.isNotBlank() && it != "und" }
        ?.let { Locale.forLanguageTag(it).getDisplayLanguage(locale).takeIf { name -> name.isNotBlank() } }
        ?.replaceFirstChar { it.titlecase(locale) }
    val name = label?.takeIf { it.isNotBlank() } ?: languageName ?: "$fallback ${index + 1}"
    val layout = channelLabel(channels)
    return if (layout.isEmpty()) name else "$name · $layout"
}

/** Mime type for a subtitle file the user picked, or null for formats the player cannot read. */
fun subtitleMimeType(fileName: String?, reportedMime: String?): String? {
    val byName = when (fileName?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)) {
        "srt" -> "application/x-subrip"
        "vtt" -> "text/vtt"
        "ass", "ssa" -> "text/x-ssa"
        "ttml", "dfxp", "xml" -> "application/ttml+xml"
        else -> null
    }
    if (byName != null) return byName
    return reportedMime?.takeIf {
        it in setOf("application/x-subrip", "text/vtt", "text/x-ssa", "application/ttml+xml")
    }
}

/**
 * Orders names the way people expect: "Episode 2" before "Episode 10", ignoring case. Used to line up the other
 * videos in a folder as "next episode".
 */
fun naturalCompare(a: String, b: String): Int {
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        if (a[i].isDigit() && b[j].isDigit()) {
            val startA = i
            val startB = j
            while (i < a.length && a[i].isDigit()) i++
            while (j < b.length && b[j].isDigit()) j++
            val numA = a.substring(startA, i).trimStart('0')
            val numB = b.substring(startB, j).trimStart('0')
            val byLength = numA.length.compareTo(numB.length)
            if (byLength != 0) return byLength
            val byValue = numA.compareTo(numB)
            if (byValue != 0) return byValue
        } else {
            val byChar = a[i].lowercaseChar().compareTo(b[j].lowercaseChar())
            if (byChar != 0) return byChar
            i++
            j++
        }
    }
    return (a.length - i).compareTo(b.length - j)
}
