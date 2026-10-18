package com.kingplay.feature.library.logic

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.PlaylistEntry

/**
 * The playlist screen lists tracks, but the playlist also holds entries whose file is missing, so a row index is
 * not an entry position. This finds the real position: the Nth entry for that track, where N is how many rows with
 * the same track come before [rowIndex].
 */
fun entryPositionFor(entries: List<PlaylistEntry>, displayedTrackIds: List<Long>, rowIndex: Int): Int? {
    val trackId = displayedTrackIds.getOrNull(rowIndex) ?: return null
    val occurrence = displayedTrackIds.take(rowIndex).count { it == trackId }
    return entries
        .filter { it.ref == MediaRef.Audio(trackId) }
        .getOrNull(occurrence)
        ?.position
}
