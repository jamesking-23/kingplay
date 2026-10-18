package com.kingplay.feature.library.detail

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.theme.KpIcons
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.navigation.AppNavigator
import com.kingplay.core.ui.component.MediaRow
import com.kingplay.core.ui.displayName
import com.kingplay.core.ui.joinWithDot
import com.kingplay.core.ui.quantityString
import com.kingplay.feature.library.CollectEffects
import com.kingplay.feature.library.R

/*
 * One thin route per kind of detail screen. Each builds its view model (Hilt reads the navigation arguments into the
 * SavedStateHandle) and hands the state to the shared DetailScreen.
 */

@Composable
fun AlbumDetailRoute(navigator: AppNavigator, modifier: Modifier = Modifier) {
    val viewModel: AlbumDetailViewModel = hiltViewModel()
    SimpleDetailRoute(viewModel, navigator, modifier)
}

@Composable
fun GenreDetailRoute(navigator: AppNavigator, modifier: Modifier = Modifier) {
    val viewModel: GenreDetailViewModel = hiltViewModel()
    SimpleDetailRoute(viewModel, navigator, modifier)
}

@Composable
fun FolderDetailRoute(navigator: AppNavigator, modifier: Modifier = Modifier) {
    val viewModel: FolderDetailViewModel = hiltViewModel()
    SimpleDetailRoute(viewModel, navigator, modifier)
}

@Composable
private fun SimpleDetailRoute(viewModel: DetailViewModel, navigator: AppNavigator, modifier: Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    CollectEffects(viewModel.effects, snackbar)
    DetailScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = navigator::back,
        snackbar = snackbar,
        modifier = modifier,
    )
}

@Composable
fun ArtistDetailRoute(navigator: AppNavigator, modifier: Modifier = Modifier) {
    val viewModel: ArtistDetailViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    CollectEffects(viewModel.effects, snackbar)
    DetailScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = navigator::back,
        snackbar = snackbar,
        modifier = modifier,
        extraContent = {
            if (albums.isNotEmpty()) {
                item(key = "albums-title") {
                    Text(
                        text = stringResource(R.string.albums_section),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(horizontal = KpTheme.spacing.lg, vertical = KpTheme.spacing.sm)
                            .semantics { heading() },
                    )
                }
                items(albums, key = { "album-${it.id}" }) { album ->
                    MediaRow(
                        title = displayName(album.title),
                        subtitle = joinWithDot(
                            quantityString(R.plurals.count_songs, album.trackCount),
                            album.year?.toString(),
                        ),
                        artworkUri = album.artworkUri,
                        artworkKey = "album:${album.id}",
                        leadingIcon = KpIcons.Album,
                        onClick = { navigator.toAlbum(album.id) },
                    )
                }
                item(key = "songs-title") {
                    Text(
                        text = stringResource(R.string.songs_section),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(horizontal = KpTheme.spacing.lg, vertical = KpTheme.spacing.sm)
                            .semantics { heading() },
                    )
                }
            }
        },
    )
}

@Composable
fun PlaylistDetailRoute(navigator: AppNavigator, modifier: Modifier = Modifier) {
    val viewModel: PlaylistDetailViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    CollectEffects(viewModel.effects, snackbar)
    DetailScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = navigator::back,
        snackbar = snackbar,
        modifier = modifier,
        extraTrackMenu = { index, dismiss ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.playlist_remove_track)) },
                onClick = {
                    dismiss()
                    viewModel.removeRow(index)
                },
            )
        },
    )
}
