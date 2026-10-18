package com.kingplay.desktop.userdata

import com.kingplay.core.model.Track
import com.kingplay.desktop.library.LibraryIndex

const val SMART_LIST_LIMIT = 100

/** Tracks of a playlist in order, leaving out files that are no longer in the library. */
fun playlistTracks(playlist: DesktopPlaylist, index: LibraryIndex): List<Track> =
    playlist.trackIds.mapNotNull { index[it] }

fun favoriteTracks(index: LibraryIndex, data: UserData): List<Track> =
    index.tracks.filter { it.id in data.favorites }.sortedBy { it.title.lowercase() }

fun mostPlayed(index: LibraryIndex, data: UserData, limit: Int = SMART_LIST_LIMIT): List<Track> =
    index.tracks
        .filter { (data.stats[it.id]?.count ?: 0) > 0 }
        .sortedWith(compareByDescending<Track> { data.stats.getValue(it.id).count }.thenBy { it.title.lowercase() })
        .take(limit)

fun recentlyPlayed(index: LibraryIndex, data: UserData, limit: Int = SMART_LIST_LIMIT): List<Track> =
    index.tracks
        .filter { it.id in data.stats }
        .sortedByDescending { data.stats.getValue(it.id).lastPlayedMs }
        .take(limit)

fun recentlyAdded(index: LibraryIndex, limit: Int = SMART_LIST_LIMIT): List<Track> =
    index.tracks.sortedByDescending { it.dateAddedMs }.take(limit)
