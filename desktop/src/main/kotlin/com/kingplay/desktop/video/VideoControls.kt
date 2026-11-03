package com.kingplay.desktop.video

import java.awt.event.KeyEvent

/** Something the person asked the video window to do, from a key press, the mouse or a button. */
sealed interface VideoCommand {
    data object TogglePlay : VideoCommand
    data class Seek(val ms: Long) : VideoCommand
    data class VolumeBy(val delta: Int) : VideoCommand
    data object ToggleMute : VideoCommand
    data object ToggleFullscreen : VideoCommand
    data object ExitFullscreen : VideoCommand
    data object Next : VideoCommand
    data object Previous : VideoCommand
    data object SpeedUp : VideoCommand
    data object SpeedDown : VideoCommand
    data object SpeedNormal : VideoCommand
    data object CycleAspect : VideoCommand
    data object CycleCrop : VideoCommand
    data object CycleSubtitles : VideoCommand
    data object CycleAudio : VideoCommand
    data class SubtitleDelay(val deltaMs: Int) : VideoCommand
    data class AudioDelay(val deltaMs: Int) : VideoCommand
    data object NextFrame : VideoCommand
    data object Snapshot : VideoCommand
    data object ToggleLoop : VideoCommand
    data object Restart : VideoCommand
}

const val SEEK_SHORT_MS = 3_000L
const val SEEK_NORMAL_MS = 10_000L
const val SEEK_LONG_MS = 60_000L
const val VOLUME_STEP = 5
const val DELAY_STEP_MS = 50

/** The shortcut for a key, using VLC's keys where it has them. Null for keys the player ignores. */
fun commandForKey(keyCode: Int, shift: Boolean, ctrl: Boolean): VideoCommand? {
    val seekMs = when {
        ctrl -> SEEK_LONG_MS
        shift -> SEEK_SHORT_MS
        else -> SEEK_NORMAL_MS
    }
    return when (keyCode) {
        KeyEvent.VK_SPACE, KeyEvent.VK_K -> VideoCommand.TogglePlay
        KeyEvent.VK_RIGHT -> VideoCommand.Seek(seekMs)
        KeyEvent.VK_LEFT -> VideoCommand.Seek(-seekMs)
        KeyEvent.VK_UP -> VideoCommand.VolumeBy(VOLUME_STEP)
        KeyEvent.VK_DOWN -> VideoCommand.VolumeBy(-VOLUME_STEP)
        KeyEvent.VK_M -> VideoCommand.ToggleMute
        KeyEvent.VK_F, KeyEvent.VK_F11 -> VideoCommand.ToggleFullscreen
        KeyEvent.VK_ESCAPE -> VideoCommand.ExitFullscreen
        KeyEvent.VK_N, KeyEvent.VK_PAGE_DOWN -> VideoCommand.Next
        KeyEvent.VK_P, KeyEvent.VK_PAGE_UP -> VideoCommand.Previous
        KeyEvent.VK_CLOSE_BRACKET -> VideoCommand.SpeedUp
        KeyEvent.VK_OPEN_BRACKET -> VideoCommand.SpeedDown
        KeyEvent.VK_EQUALS -> VideoCommand.SpeedNormal
        KeyEvent.VK_A -> VideoCommand.CycleAspect
        KeyEvent.VK_C -> VideoCommand.CycleCrop
        KeyEvent.VK_V -> VideoCommand.CycleSubtitles
        KeyEvent.VK_B -> VideoCommand.CycleAudio
        KeyEvent.VK_H -> VideoCommand.SubtitleDelay(-DELAY_STEP_MS)
        KeyEvent.VK_J -> VideoCommand.SubtitleDelay(DELAY_STEP_MS)
        KeyEvent.VK_G -> VideoCommand.AudioDelay(-DELAY_STEP_MS)
        KeyEvent.VK_L -> VideoCommand.AudioDelay(DELAY_STEP_MS)
        KeyEvent.VK_E -> VideoCommand.NextFrame
        KeyEvent.VK_S -> VideoCommand.Snapshot
        KeyEvent.VK_R -> VideoCommand.ToggleLoop
        KeyEvent.VK_HOME -> VideoCommand.Restart
        else -> null
    }
}

/** A picture shape VLC can force. [vlc] is the text libVLC wants ("16:9"); null means the video's own shape. */
data class PictureShape(val label: String, val vlc: String?)

val AspectRatios = listOf(
    PictureShape("Default", null),
    PictureShape("16:9", "16:9"),
    PictureShape("4:3", "4:3"),
    PictureShape("21:9", "21:9"),
    PictureShape("16:10", "16:10"),
    PictureShape("5:4", "5:4"),
    PictureShape("1:1", "1:1"),
    PictureShape("2.35:1", "235:100"),
)

/** Crops the picture to a shape, cutting off the edges (VLC's "crop"). */
val CropShapes = listOf(
    PictureShape("None", null),
    PictureShape("16:9", "16:9"),
    PictureShape("4:3", "4:3"),
    PictureShape("21:9", "21:9"),
    PictureShape("16:10", "16:10"),
    PictureShape("1:1", "1:1"),
    PictureShape("2.35:1", "235:100"),
)

val PlaybackSpeeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f, 4f)

/** The speed one step up or down from [current]; stays at the ends. */
fun steppedSpeed(current: Float, up: Boolean): Float {
    val nearest = PlaybackSpeeds.indices.minByOrNull { kotlin.math.abs(PlaybackSpeeds[it] - current) } ?: return 1f
    return PlaybackSpeeds[(nearest + if (up) 1 else -1).coerceIn(0, PlaybackSpeeds.lastIndex)]
}

fun formatRate(rate: Float): String =
    String.format(java.util.Locale.ROOT, "%.2f", rate).trimEnd('0').trimEnd('.') + "x"

/** "+0.05 s" or "-0.10 s" for a sync delay. */
fun formatDelay(ms: Int): String =
    String.format(java.util.Locale.ROOT, "%+.2f s", ms / 1000.0)

/** The id after [current] in [ids], wrapping round; the first id when [current] is not in the list. */
fun nextInCycle(ids: List<Int>, current: Int): Int? {
    if (ids.isEmpty()) return null
    val index = ids.indexOf(current)
    return ids[(index + 1) % ids.size]
}

fun clampVolume(value: Int, max: Int = MAX_VOLUME_PERCENT): Int = value.coerceIn(0, max)

const val MAX_VOLUME_PERCENT = 150

/** A–B repeat: playback jumps back to [start] when it reaches [end]. */
data class AbLoop(val start: Long? = null, val end: Long? = null) {
    val active: Boolean get() = start != null && end != null && end > start

    fun jumpFor(positionMs: Long): Long? = if (active && positionMs >= end!!) start else null

    /** First mark is A, the second is B, the third starts again with A. */
    fun mark(positionMs: Long): AbLoop = when {
        start == null || end != null -> AbLoop(start = positionMs)
        positionMs > start -> copy(end = positionMs)
        else -> AbLoop(start = positionMs)
    }
}
