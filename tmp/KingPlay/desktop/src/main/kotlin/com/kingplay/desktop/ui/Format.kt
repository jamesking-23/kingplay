package com.kingplay.desktop.ui

import com.kingplay.core.model.RepeatMode

/** 65_000 -> "1:05", 3_725_000 -> "1:02:05", negative or unknown -> "0:00". */
fun formatTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0)) / 1000
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

fun nextRepeatMode(mode: RepeatMode): RepeatMode = when (mode) {
    RepeatMode.OFF -> RepeatMode.ALL
    RepeatMode.ALL -> RepeatMode.ONE
    RepeatMode.ONE -> RepeatMode.OFF
}

/** The folder shown in the folders list: the last path segment ("C:" for a drive root, the whole path if empty). */
fun folderLabel(path: String): String {
    val trimmed = path.trimEnd('/', '\\')
    val name = trimmed.substringAfterLast('/').substringAfterLast('\\')
    return name.ifEmpty { path }
}
