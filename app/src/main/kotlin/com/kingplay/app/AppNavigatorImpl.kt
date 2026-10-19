package com.kingplay.app

import androidx.navigation.NavHostController
import com.kingplay.core.model.navigation.AppNavigator

/** Connects the feature modules' navigation requests to the app's nav graph. */
class AppNavigatorImpl(private val navController: NavHostController) : AppNavigator {

    override fun toNowPlaying() = go(Routes.NOW_PLAYING)

    override fun toAlbum(albumId: Long) = go(Routes.album(albumId))

    override fun toArtist(artistId: Long) = go(Routes.artist(artistId))

    override fun toPlaylist(playlistId: Long) = go(Routes.playlist(playlistId))

    override fun toMostPlayed() = go(Routes.MOST_PLAYED)

    override fun toGenre(genre: String) = go(Routes.genre(genre))

    override fun toFolder(path: String) = go(Routes.folder(path))

    // The video player screen (2g) does not exist yet; its entry point does nothing until then.
    override fun toVideo(videoId: Long) = go(Routes.video(videoId))

    override fun toEqualizer() = go(Routes.EQUALIZER)

    override fun toSettings() = go(Routes.SETTINGS)

    override fun back() {
        navController.popBackStack()
    }

    private fun go(route: String) {
        navController.navigate(route) { launchSingleTop = true }
    }
}
