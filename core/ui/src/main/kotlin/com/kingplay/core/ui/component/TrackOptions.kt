package com.kingplay.core.ui.component

import androidx.compose.runtime.staticCompositionLocalOf
import com.kingplay.core.model.Track
import com.kingplay.core.model.VideoItem

/** Things a song's menu can do that need the screen around it (dialogs, the share sheet, delete confirmation). */
interface TrackOptions {
    fun addToPlaylist(track: Track)

    fun share(track: Track)

    fun delete(track: Track)

    /** The same three actions for every ticked song at once. */
    fun addToPlaylist(tracks: List<Track>)

    fun share(tracks: List<Track>)

    fun delete(tracks: List<Track>)

    fun shareVideos(videos: List<VideoItem>)

    fun deleteVideos(videos: List<VideoItem>)
}

/** Provided once near the top of the app; song menus show the extra items only when it is there. */
val LocalTrackOptions = staticCompositionLocalOf<TrackOptions?> { null }
