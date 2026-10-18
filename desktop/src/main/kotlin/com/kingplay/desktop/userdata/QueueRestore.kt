package com.kingplay.desktop.userdata

import com.kingplay.desktop.library.LibraryIndex

/**
 * Keeps only the songs that are still in the library and moves the current position with them.
 * If the song that was playing is gone, the one that now sits in its place becomes current.
 * Returns null when nothing is left to restore.
 */
fun restorableQueue(saved: SavedQueue?, library: LibraryIndex): SavedQueue? {
    if (saved == null) return null
    val kept = saved.trackIds.filter { library[it] != null }
    if (kept.isEmpty()) return null
    val before = saved.trackIds.take(saved.index.coerceAtLeast(0)).count { library[it] != null }
    val currentSurvived = saved.trackIds.getOrNull(saved.index)?.let { library[it] != null } == true
    return saved.copy(
        trackIds = kept,
        index = before.coerceAtMost(kept.lastIndex),
        positionMs = if (currentSurvived) saved.positionMs else 0,
    )
}
