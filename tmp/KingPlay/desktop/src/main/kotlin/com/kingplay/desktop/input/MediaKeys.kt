package com.kingplay.desktop.input

/** What a hardware media key should do. */
enum class MediaKeyAction { PlayPause, Next, Previous, Stop }

/** Windows virtual-key codes for the media keys (winuser.h). */
object MediaKeyCodes {
    const val VK_MEDIA_NEXT_TRACK = 0xB0
    const val VK_MEDIA_PREV_TRACK = 0xB1
    const val VK_MEDIA_STOP = 0xB2
    const val VK_MEDIA_PLAY_PAUSE = 0xB3

    val ALL: List<Int> = listOf(VK_MEDIA_NEXT_TRACK, VK_MEDIA_PREV_TRACK, VK_MEDIA_STOP, VK_MEDIA_PLAY_PAUSE)
}

fun mediaKeyAction(virtualKey: Int): MediaKeyAction? = when (virtualKey) {
    MediaKeyCodes.VK_MEDIA_PLAY_PAUSE -> MediaKeyAction.PlayPause
    MediaKeyCodes.VK_MEDIA_NEXT_TRACK -> MediaKeyAction.Next
    MediaKeyCodes.VK_MEDIA_PREV_TRACK -> MediaKeyAction.Previous
    MediaKeyCodes.VK_MEDIA_STOP -> MediaKeyAction.Stop
    else -> null
}

/**
 * A hot key registration comes back as a message carrying the id we registered it under.
 * Ids are the virtual-key codes themselves, so the mapping needs no table.
 */
fun actionForHotKeyId(id: Long): MediaKeyAction? = if (id in 0..Int.MAX_VALUE) mediaKeyAction(id.toInt()) else null
