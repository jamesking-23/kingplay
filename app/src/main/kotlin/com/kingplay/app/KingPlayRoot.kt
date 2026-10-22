package com.kingplay.app

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kingplay.core.designsystem.theme.Motion
import com.kingplay.feature.equalizer.EqualizerScreen
import com.kingplay.feature.library.detail.AlbumDetailRoute
import com.kingplay.feature.library.detail.ArtistDetailRoute
import com.kingplay.feature.library.detail.DetailArgs
import com.kingplay.feature.library.detail.FolderDetailRoute
import com.kingplay.feature.library.detail.GenreDetailRoute
import com.kingplay.feature.library.detail.MostPlayedRoute
import com.kingplay.feature.library.detail.PlaylistDetailRoute
import com.kingplay.feature.library.report.ReportArgs
import com.kingplay.feature.nowplaying.stage.LyricsBrowserScreen
import com.kingplay.core.designsystem.theme.CoverBackdrop
import com.kingplay.feature.library.options.TrackOptionsHost
import com.kingplay.feature.settings.profile.ProfileScreen
import com.kingplay.feature.library.report.ReportScreen
import com.kingplay.feature.library.home.LibraryScreen
import com.kingplay.feature.nowplaying.MiniPlayer
import com.kingplay.feature.nowplaying.NowPlayingScreen
import com.kingplay.feature.settings.SettingsScreen
import com.kingplay.feature.video.VideoArgs
import com.kingplay.feature.video.ui.VideoPlayerScreen

/**
 * The app's frame: one nav graph, and the mini player pinned above the system bar on every screen except the
 * full-screen ones (Now Playing and the video player).
 */
@Composable
fun KingPlayRoot(modifier: Modifier = Modifier, startRoute: String? = null) {
    TrackOptionsHost { CoverBackdrop(modifier = Modifier.fillMaxSize()) { KingPlayContent(modifier, startRoute) } }
}

@Composable
private fun KingPlayContent(modifier: Modifier, startRoute: String?) {
    val navController = rememberNavController()
    // Debug builds only: lets the screenshot tour open a screen directly.
    androidx.compose.runtime.LaunchedEffect(startRoute) { startRoute?.let { navController.navigate(it) } }
    val navigator = remember(navController) { AppNavigatorImpl(navController) }
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val fullScreen = route == Routes.NOW_PLAYING || route == Routes.VIDEO

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!fullScreen) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    MiniPlayer(onOpen = navigator::toNowPlaying)
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.LIBRARY) { LibraryScreen(navigator) }
            composable(
                route = Routes.NOW_PLAYING,
                enterTransition = { slideInVertically(Motion.effects(Motion.DurationMedium)) { it } },
                popExitTransition = { slideOutVertically(Motion.effects(Motion.DurationMedium)) { it } },
            ) { NowPlayingScreen(navigator) }
            composable(
                route = Routes.ALBUM,
                arguments = listOf(navArgument(DetailArgs.ALBUM_ID) { type = NavType.LongType }),
            ) { AlbumDetailRoute(navigator) }
            composable(
                route = Routes.ARTIST,
                arguments = listOf(navArgument(DetailArgs.ARTIST_ID) { type = NavType.LongType }),
            ) { ArtistDetailRoute(navigator) }
            composable(
                route = Routes.PLAYLIST,
                arguments = listOf(navArgument(DetailArgs.PLAYLIST_ID) { type = NavType.LongType }),
            ) { PlaylistDetailRoute(navigator) }
            composable(
                route = Routes.REPORT,
                arguments = listOf(navArgument(ReportArgs.MONTH) { type = NavType.StringType }),
            ) { ReportScreen(navigator) }
            composable(Routes.LYRICS_BROWSER) { LyricsBrowserScreen(navigator) }
            composable(Routes.PROFILE) { ProfileScreen(navigator) }
            composable(Routes.MOST_PLAYED) { MostPlayedRoute(navigator) }
            composable(
                route = Routes.GENRE,
                arguments = listOf(navArgument(DetailArgs.GENRE) { type = NavType.StringType }),
            ) { GenreDetailRoute(navigator) }
            composable(
                route = Routes.FOLDER,
                arguments = listOf(navArgument(DetailArgs.FOLDER_PATH) { type = NavType.StringType }),
            ) { FolderDetailRoute(navigator) }
            composable(
                route = Routes.VIDEO,
                arguments = listOf(navArgument(VideoArgs.VIDEO_ID) { type = NavType.LongType }),
            ) { VideoPlayerScreen(navigator) }
            composable(Routes.EQUALIZER) { EqualizerScreen(navigator) }
            composable(Routes.SETTINGS) { SettingsScreen(navigator) }
        }
    }
}
