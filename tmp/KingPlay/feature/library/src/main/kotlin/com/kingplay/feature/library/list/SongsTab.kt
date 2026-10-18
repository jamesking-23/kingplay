package com.kingplay.feature.library.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.kingplay.core.designsystem.component.KpButton
import com.kingplay.core.designsystem.component.KpButtonStyle
import com.kingplay.core.designsystem.component.KpEmptyState
import com.kingplay.core.designsystem.component.KpErrorState
import com.kingplay.core.designsystem.component.KpSkeleton
import com.kingplay.core.designsystem.component.KpSkeletonList
import com.kingplay.core.designsystem.theme.KpIcons
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.SongSort
import com.kingplay.core.model.SongSortField
import com.kingplay.core.ui.component.MediaRow
import com.kingplay.core.ui.component.TrackMenuItems
import com.kingplay.core.ui.displayName
import com.kingplay.core.ui.format.formatDuration
import com.kingplay.core.ui.joinWithDot
import com.kingplay.feature.library.CollectEffects
import com.kingplay.feature.library.R

@Composable
fun SongsTab(
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: SongsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val songs = viewModel.songs.collectAsLazyPagingItems()
    CollectEffects(viewModel.effects, snackbar)

    val refresh = songs.loadState.refresh
    val showSkeleton = songs.itemCount == 0 && refresh is LoadState.Loading
    val showEmpty = songs.itemCount == 0 && refresh is LoadState.NotLoading && refresh.endOfPaginationReached
    val showError = songs.itemCount == 0 && refresh is LoadState.Error

    Column(modifier = modifier.fillMaxSize()) {
        SongsToolbar(
            sort = state.sort,
            onSort = { viewModel.onEvent(SongsEvent.SortBy(it)) },
            onShuffle = { viewModel.onEvent(SongsEvent.ShuffleAll) },
            shuffleEnabled = songs.itemCount > 0,
        )
        when {
            showSkeleton -> KpSkeletonList(modifier = Modifier.fillMaxWidth())
            showError -> KpErrorState(onRetry = { songs.retry() })
            showEmpty -> KpEmptyState(
                icon = KpIcons.MusicNote,
                title = stringResource(R.string.songs_empty_title),
                message = stringResource(R.string.songs_empty_message),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(count = songs.itemCount, key = songs.itemKey { it.id }) { index ->
                    val track = songs[index]
                    if (track == null) {
                        PlaceholderRow()
                    } else {
                        MediaRow(
                            title = track.title,
                            subtitle = joinWithDot(displayName(track.artistName), displayName(track.albumTitle)),
                            trailingText = formatDuration(track.durationMs),
                            artworkUri = track.uri,
                            artworkKey = "album:${track.albumId}",
                            highlighted = track.id == state.currentTrackId,
                            onClick = { viewModel.onEvent(SongsEvent.Play(track.id)) },
                            menu = { dismiss ->
                                TrackMenuItems(
                                    track = track,
                                    dismiss = dismiss,
                                    onPlayNext = { viewModel.onEvent(SongsEvent.PlayNext(it)) },
                                    onAddToQueue = { viewModel.onEvent(SongsEvent.AddToQueue(it)) },
                                    onToggleFavorite = { viewModel.onEvent(SongsEvent.ToggleFavorite(it)) },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KpTheme.spacing.lg, vertical = KpTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.md),
    ) {
        KpSkeleton(modifier = Modifier.size(52.dp))
        KpSkeleton(modifier = Modifier.weight(1f).height(14.dp))
    }
}

@Composable
private fun SongsToolbar(
    sort: SongSort,
    onSort: (SongSortField) -> Unit,
    onShuffle: () -> Unit,
    shuffleEnabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KpTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SortButton(sort = sort, onSort = onSort)
        KpButton(
            text = stringResource(R.string.library_shuffle_all),
            onClick = onShuffle,
            style = KpButtonStyle.Tonal,
            enabled = shuffleEnabled,
            leadingIcon = KpIcons.Shuffle,
        )
    }
}

@Composable
private fun SortButton(sort: SongSort, onSort: (SongSortField) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val fieldName = stringResource(sort.field.labelRes())
    val direction = stringResource(if (sort.ascending) R.string.sort_ascending else R.string.sort_descending)
    val description = stringResource(R.string.sort_button_description, fieldName, direction)
    Box {
        KpButton(
            text = "$fieldName ${if (sort.ascending) "↑" else "↓"}",
            onClick = { expanded = true },
            style = KpButtonStyle.Text,
            modifier = Modifier.semantics { contentDescription = description },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SongSortField.entries.forEach { field ->
                DropdownMenuItem(
                    text = { Text(stringResource(field.labelRes())) },
                    onClick = {
                        expanded = false
                        onSort(field)
                    },
                )
            }
        }
    }
}

private fun SongSortField.labelRes(): Int = when (this) {
    SongSortField.TITLE -> R.string.sort_title
    SongSortField.ARTIST -> R.string.sort_artist
    SongSortField.ALBUM -> R.string.sort_album
    SongSortField.DATE_ADDED -> R.string.sort_date_added
    SongSortField.DURATION -> R.string.sort_duration
    SongSortField.PLAY_COUNT -> R.string.sort_play_count
}
