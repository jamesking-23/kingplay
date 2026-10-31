package com.kingplay.feature.library.list

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import com.kingplay.core.designsystem.component.KpErrorState
import com.kingplay.core.ui.Async
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.component.KpButton
import com.kingplay.core.designsystem.component.KpButtonStyle
import com.kingplay.core.designsystem.theme.KpIcons
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.Playlist
import com.kingplay.core.model.navigation.AppNavigator
import com.kingplay.core.ui.component.MediaRow
import com.kingplay.core.ui.displayName
import com.kingplay.core.ui.format.formatDuration
import com.kingplay.core.ui.joinWithDot
import com.kingplay.core.ui.quantityString
import com.kingplay.feature.library.R

@Composable
fun AlbumsTab(navigator: AppNavigator, modifier: Modifier = Modifier, viewModel: AlbumsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ListTab(
        state = state,
        onRetry = viewModel::retry,
        emptyIcon = KpIcons.Album,
        emptyTitle = stringResource(R.string.albums_empty_title),
        emptyMessage = stringResource(R.string.albums_empty_message),
        key = { it.id },
        modifier = modifier,
    ) { album ->
        val songs = quantityString(R.plurals.count_songs, album.trackCount)
        MediaRow(
            title = displayName(album.title),
            subtitle = joinWithDot(displayName(album.artistName), songs),
            artworkUri = album.artworkUri,
            artworkKey = "album:${album.id}",
            leadingIcon = KpIcons.Album,
            onClick = { navigator.toAlbum(album.id) },
        )
    }
}

@Composable
fun ArtistsTab(navigator: AppNavigator, modifier: Modifier = Modifier, viewModel: ArtistsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ListTab(
        state = state,
        onRetry = viewModel::retry,
        emptyIcon = Icons.Default.Person,
        emptyTitle = stringResource(R.string.artists_empty_title),
        emptyMessage = stringResource(R.string.artists_empty_message),
        key = { it.id },
        modifier = modifier,
    ) { artist ->
        val albums = quantityString(R.plurals.count_albums, artist.albumCount)
        val songs = quantityString(R.plurals.count_songs, artist.trackCount)
        MediaRow(
            title = displayName(artist.name),
            subtitle = joinWithDot(albums, songs),
            leadingIcon = Icons.Default.Person,
            onClick = { navigator.toArtist(artist.id) },
        )
    }
}

@Composable
fun GenresTab(navigator: AppNavigator, modifier: Modifier = Modifier, viewModel: GenresViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ListTab(
        state = state,
        onRetry = viewModel::retry,
        emptyIcon = KpIcons.MusicNote,
        emptyTitle = stringResource(R.string.genres_empty_title),
        emptyMessage = stringResource(R.string.genres_empty_message),
        key = { it.name },
        modifier = modifier,
    ) { genre ->
        MediaRow(
            title = genre.name,
            subtitle = quantityString(R.plurals.count_songs, genre.trackCount),
            leadingIcon = KpIcons.MusicNote,
            onClick = { navigator.toGenre(genre.name) },
        )
    }
}

@Composable
fun FoldersTab(navigator: AppNavigator, modifier: Modifier = Modifier, viewModel: FoldersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ListTab(
        state = state,
        onRetry = viewModel::retry,
        emptyIcon = KpIcons.Folder,
        emptyTitle = stringResource(R.string.folders_empty_title),
        emptyMessage = stringResource(R.string.folders_empty_message),
        key = { it.path },
        modifier = modifier,
    ) { folder ->
        MediaRow(
            title = folder.name,
            subtitle = folder.path,
            leadingIcon = KpIcons.Folder,
            onClick = { navigator.toFolder(folder.path) },
        )
    }
}

@Composable
fun PlaylistsTab(
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Playlist?>(null) }

    val newPlaylistButton: @Composable () -> Unit = {
        MediaRow(
            title = stringResource(R.string.most_played_title),
            subtitle = stringResource(R.string.most_played_subtitle),
            leadingIcon = Icons.Default.Star,
            onClick = navigator::toMostPlayed,
        )
        MediaRow(
            title = "Monthly listening report",
            subtitle = "Your month in music, like a Wrapped",
            leadingIcon = Icons.Default.DateRange,
            onClick = { navigator.toReport() },
        )
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = KpTheme.spacing.lg)) {
            KpButton(
                text = stringResource(R.string.playlist_new),
                onClick = { showCreate = true },
                style = KpButtonStyle.Tonal,
            )
        }
    }

    // One list for everything: the shortcuts, then the playlists. (Swapping between an empty screen and a list
    // used to leave a gap under the tab bar after the first playlist was made.)
    androidx.compose.foundation.lazy.LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "header") { Column { newPlaylistButton() } }
        when (val current = state) {
            is Async.Content -> items(current.data, key = { it.id }) { playlist ->
                MediaRow(
                    title = playlist.name,
                    subtitle = quantityString(R.plurals.count_items, playlist.entryCount),
                    leadingIcon = KpIcons.QueueMusic,
                    onClick = { navigator.toPlaylist(playlist.id) },
                    menu = { dismiss ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playlist_delete)) },
                            onClick = {
                                dismiss()
                                pendingDelete = playlist
                            },
                        )
                    },
                )
            }
            Async.Empty -> item(key = "empty") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(KpTheme.spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm),
                ) {
                    androidx.compose.material3.Icon(
                        KpIcons.QueueMusic,
                        contentDescription = null,
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        stringResource(R.string.playlists_empty_title),
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.playlists_empty_message),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            is Async.Error -> item(key = "error") { KpErrorState(onRetry = viewModel::retry, message = current.message) }
            Async.Loading -> Unit
        }
    }

    if (showCreate) {
        CreatePlaylistDialog(
            onDismiss = { showCreate = false },
            onCreate = { name ->
                showCreate = false
                viewModel.create(name) { id -> navigator.toPlaylist(id) }
            },
        )
    }
    pendingDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.playlist_delete_title)) },
            text = { Text(stringResource(R.string.playlist_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(playlist.id)
                        pendingDelete = null
                    },
                ) { Text(stringResource(R.string.playlist_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.playlist_cancel)) }
            },
        )
    }
}

@Composable
internal fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_new)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.playlist_create))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.playlist_cancel)) } },
    )
}

@Composable
fun VideosTab(navigator: AppNavigator, modifier: Modifier = Modifier, viewModel: VideosViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ListTab(
        state = state,
        onRetry = viewModel::retry,
        emptyIcon = KpIcons.Movie,
        emptyTitle = stringResource(R.string.videos_empty_title),
        emptyMessage = stringResource(R.string.videos_empty_message),
        key = { it.id },
        modifier = modifier,
    ) { video ->
        val resume = if (video.resumePositionMs > 0) {
            stringResource(R.string.video_resume_at, formatDuration(video.resumePositionMs))
        } else {
            null
        }
        val details = joinWithDot(formatDuration(video.durationMs), videoQuality(video.width, video.height))
        MediaRow(
            title = displayName(video.title),
            subtitle = joinWithDot(details, resume),
            artworkUri = video.uri,
            artworkKey = "video:${video.id}",
            leadingIcon = KpIcons.Movie,
            onClick = { navigator.toVideo(video.id) },
        )
    }
}

/** "1080p", "4K"; empty when the size is unknown. */
private fun videoQuality(width: Int, height: Int): String = when {
    width <= 0 || height <= 0 -> ""
    maxOf(width, height) >= UHD_LONG_SIDE -> "4K"
    else -> "${minOf(width, height)}p"
}

private const val UHD_LONG_SIDE = 3_800
