package com.kingplay.app

import android.net.Uri
import com.kingplay.feature.library.detail.DetailArgs

/** Navigation routes. Argument names come from the feature module so the view models can read them. */
object Routes {
    const val LIBRARY = "library"
    const val NOW_PLAYING = "nowplaying"
    const val SETTINGS = "settings"
    const val EQUALIZER = "equalizer"

    const val ALBUM = "album/{${DetailArgs.ALBUM_ID}}"
    const val ARTIST = "artist/{${DetailArgs.ARTIST_ID}}"
    const val PLAYLIST = "playlist/{${DetailArgs.PLAYLIST_ID}}"
    const val GENRE = "genre/{${DetailArgs.GENRE}}"
    const val FOLDER = "folder/{${DetailArgs.FOLDER_PATH}}"

    fun album(id: Long) = "album/$id"

    fun artist(id: Long) = "artist/$id"

    fun playlist(id: Long) = "playlist/$id"

    // Text arguments can contain slashes and spaces, so they are encoded into one path segment.
    fun genre(name: String) = "genre/${Uri.encode(name)}"

    fun folder(path: String) = "folder/${Uri.encode(path)}"
}
