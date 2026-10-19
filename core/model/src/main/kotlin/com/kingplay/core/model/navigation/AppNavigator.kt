package com.kingplay.core.model.navigation

/**
 * Navigation contract. Feature modules receive this interface and never depend on each other.
 * The :app module provides the implementation.
 */
interface AppNavigator {
    fun toNowPlaying()

    fun toAlbum(albumId: Long)

    fun toArtist(artistId: Long)

    fun toPlaylist(playlistId: Long)

    fun toMostPlayed()

    fun toGenre(genre: String)

    fun toFolder(path: String)

    fun toVideo(videoId: Long)

    fun toEqualizer()

    fun toSettings()

    fun back()
}
