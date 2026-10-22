package com.kingplay.core.ui.component

import androidx.compose.runtime.staticCompositionLocalOf
import com.kingplay.core.model.Track

/** Things a song's menu can do that need the screen around it (dialogs, the share sheet, delete confirmation). */
interface TrackOptions {
    fun addToPlaylist(track: Track)

    fun share(track: Track)

    fun delete(track: Track)
}

/** Provided once near the top of the app; song menus show the extra items only when it is there. */
val LocalTrackOptions = staticCompositionLocalOf<TrackOptions?> { null }
