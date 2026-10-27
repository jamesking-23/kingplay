package com.kingplay.feature.library.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.PlayArrow
import com.kingplay.core.designsystem.component.KpButton
import com.kingplay.core.designsystem.component.KpButtonStyle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.component.KpEmptyState
import com.kingplay.core.designsystem.component.KpIconButton
import com.kingplay.core.designsystem.theme.KpIcons
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.navigation.AppNavigator
import com.kingplay.core.ui.component.MediaRow
import com.kingplay.core.ui.displayName
import com.kingplay.core.ui.format.formatDuration
import com.kingplay.core.ui.joinWithDot
import com.kingplay.core.ui.quantityString
import com.kingplay.feature.library.R

/** Full-screen search over songs, albums, artists and videos. Shown in place of the tabs. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchPanel(
    navigator: AppNavigator,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val focus = FocusRequester()
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KpTheme.spacing.lg, vertical = KpTheme.spacing.xs)
                .focusRequester(focus),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            leadingIcon = {
                KpIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.search_close),
                    onClick = onClose,
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    KpIconButton(
                        icon = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.search_clear),
                        onClick = { viewModel.onQueryChange("") },
                    )
                }
            },
        )
        when {
            query.isBlank() -> KpEmptyState(
                icon = KpIcons.MusicNote,
                title = stringResource(R.string.search_prompt),
                message = "",
            )
            results.isEmpty -> KpEmptyState(
                icon = KpIcons.MusicNote,
                title = stringResource(R.string.search_empty),
                message = "",
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (results.tracks.isNotEmpty()) {
                    item(key = "play-all") {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = KpTheme.spacing.lg, vertical = KpTheme.spacing.xs),
                            horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm),
                        ) {
                            KpButton(
                                text = stringResource(R.string.search_play_all, results.tracks.size),
                                onClick = { viewModel.playAll(results.tracks) },
                                leadingIcon = Icons.Default.PlayArrow,
                            )
                            KpButton(
                                text = stringResource(R.string.search_shuffle),
                                onClick = { viewModel.shuffleAll(results.tracks) },
                                style = KpButtonStyle.Tonal,
                                leadingIcon = KpIcons.Shuffle,
                            )
                        }
                    }
                    item(key = "h-songs") { SectionTitle(stringResource(R.string.songs_section)) }
                    items(results.tracks, key = { "t${it.id}" }) { track ->
                        MediaRow(
                            title = track.title,
                            subtitle = joinWithDot(displayName(track.artistName), displayName(track.albumTitle)),
                            trailingText = formatDuration(track.durationMs),
                            artworkUri = track.uri,
                            artworkKey = "album:${track.albumId}",
                            onClick = { viewModel.play(track, results.tracks) },
                        )
                    }
                }
                if (results.albums.isNotEmpty()) {
                    item(key = "h-albums") { SectionTitle(stringResource(R.string.albums_section)) }
                    items(results.albums, key = { "a${it.id}" }) { album ->
                        MediaRow(
                            title = displayName(album.title),
                            subtitle = joinWithDot(
                                displayName(album.artistName),
                                quantityString(R.plurals.count_songs, album.trackCount),
                            ),
                            artworkUri = album.artworkUri,
                            artworkKey = "album:${album.id}",
                            leadingIcon = KpIcons.Album,
                            onClick = { navigator.toAlbum(album.id) },
                        )
                    }
                }
                if (results.artists.isNotEmpty()) {
                    item(key = "h-artists") { SectionTitle(stringResource(R.string.tab_artists)) }
                    items(results.artists, key = { "r${it.id}" }) { artist ->
                        MediaRow(
                            title = displayName(artist.name),
                            subtitle = quantityString(R.plurals.count_songs, artist.trackCount),
                            leadingIcon = Icons.Default.Person,
                            onClick = { navigator.toArtist(artist.id) },
                        )
                    }
                }
                if (results.videos.isNotEmpty()) {
                    item(key = "h-videos") { SectionTitle(stringResource(R.string.tab_videos)) }
                    items(results.videos, key = { "v${it.id}" }) { video ->
                        MediaRow(
                            title = displayName(video.title),
                            subtitle = formatDuration(video.durationMs),
                            artworkUri = video.uri,
                            artworkKey = "video:${video.id}",
                            leadingIcon = KpIcons.Movie,
                            onClick = { navigator.toVideo(video.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(horizontal = KpTheme.spacing.lg, vertical = KpTheme.spacing.sm)
            .semantics { heading() },
    )
}
