package com.kingplay.feature.library.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingplay.core.designsystem.component.KpButton
import com.kingplay.core.designsystem.component.KpButtonStyle
import com.kingplay.core.designsystem.component.KpEmptyState
import com.kingplay.core.designsystem.component.KpErrorState
import com.kingplay.core.designsystem.component.KpIconButton
import com.kingplay.core.designsystem.component.KpSkeletonList
import com.kingplay.core.designsystem.theme.KpIcons
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.Track
import com.kingplay.core.ui.Async
import com.kingplay.core.ui.artwork.ArtworkImage
import com.kingplay.core.ui.artwork.ArtworkSize
import com.kingplay.core.ui.component.MediaRow
import com.kingplay.core.ui.component.TrackMenuItems
import com.kingplay.core.ui.displayName
import com.kingplay.core.ui.format.formatDuration
import com.kingplay.core.ui.format.formatTotalDuration
import com.kingplay.core.ui.joinWithDot
import com.kingplay.core.ui.quantityString
import com.kingplay.core.ui.R as UiR
import com.kingplay.feature.library.R

private val HeaderArtwork = 128.dp

/**
 * Screen for any list of songs with a header. Route composables pass the state of their view model and may add
 * [extraContent] (the artist's albums) and [extraTrackMenu] (remove from playlist).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: DetailState,
    onEvent: (DetailEvent) -> Unit,
    onBack: () -> Unit,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
    extraContent: LazyListScope.() -> Unit = {},
    extraTrackMenu: (@Composable ColumnScope.(index: Int, dismiss: () -> Unit) -> Unit)? = null,
    showPlayCounts: Boolean = false,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(state.header?.title?.let { displayName(it) }.orEmpty(), maxLines = 1) },
                navigationIcon = {
                    KpIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(UiR.string.ui_back),
                        onClick = onBack,
                    )
                },
            )
        },
    ) { padding ->
        val tracks = state.tracks
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (tracks) {
                Async.Loading -> {
                    Header(state, trackList = emptyList(), onEvent = onEvent)
                    KpSkeletonList()
                }
                Async.Empty -> KpEmptyState(
                    icon = KpIcons.MusicNote,
                    title = stringResource(R.string.detail_empty_title),
                    message = stringResource(R.string.detail_empty_message),
                )
                is Async.Error -> KpErrorState(onRetry = { onEvent(DetailEvent.Retry) }, message = tracks.message)
                is Async.Content -> TrackList(
                    state = state,
                    tracks = tracks.data,
                    onEvent = onEvent,
                    extraContent = extraContent,
                    extraTrackMenu = extraTrackMenu,
                    showPlayCounts = showPlayCounts,
                )
            }
        }
    }
}

@Composable
private fun TrackList(
    state: DetailState,
    tracks: List<Track>,
    onEvent: (DetailEvent) -> Unit,
    extraContent: LazyListScope.() -> Unit,
    extraTrackMenu: (@Composable ColumnScope.(index: Int, dismiss: () -> Unit) -> Unit)?,
    showPlayCounts: Boolean,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "header") { Header(state, tracks, onEvent) }
        extraContent()
        itemsIndexed(tracks, key = { index, track -> "${track.id}#$index" }) { index, track ->
            MediaRow(
                title = track.title,
                subtitle = if (showPlayCounts) {
                    joinWithDot("#${index + 1}", displayName(track.artistName))
                } else {
                    joinWithDot(displayName(track.artistName), formatDuration(track.durationMs))
                },
                trailingText = if (showPlayCounts) {
                    quantityString(R.plurals.count_plays, track.playCount)
                } else {
                    null
                },
                artworkUri = track.uri,
                artworkKey = "album:${track.albumId}",
                highlighted = track.id == state.currentTrackId,
                onClick = { onEvent(DetailEvent.Play(track.id)) },
                menu = { dismiss ->
                    TrackMenuItems(
                        track = track,
                        dismiss = dismiss,
                        onPlayNext = { onEvent(DetailEvent.PlayNext(it)) },
                        onAddToQueue = { onEvent(DetailEvent.AddToQueue(it)) },
                        onToggleFavorite = { onEvent(DetailEvent.ToggleFavorite(it)) },
                    )
                    extraTrackMenu?.invoke(this, index, dismiss)
                },
            )
        }
    }
}

@Composable
private fun Header(state: DetailState, trackList: List<Track>, onEvent: (DetailEvent) -> Unit) {
    val header = state.header
    val spacing = KpTheme.spacing
    Column(
        modifier = Modifier.fillMaxWidth().padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            ArtworkImage(
                audioUri = header?.artworkUri,
                cacheKey = header?.artworkKey.orEmpty(),
                sizePx = ArtworkSize.CARD,
                modifier = Modifier.size(HeaderArtwork),
            )
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = header?.title?.let { displayName(it) }.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                header?.secondary?.let { secondary ->
                    Text(
                        text = displayName(secondary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (trackList.isNotEmpty()) {
                    val count = quantityString(R.plurals.count_songs, trackList.size)
                    val total = formatTotalDuration(trackList.sumOf { it.durationMs })
                    Text(
                        text = joinWithDot(count, total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            KpButton(
                text = stringResource(R.string.library_play_all),
                onClick = { onEvent(DetailEvent.PlayAll) },
                enabled = trackList.isNotEmpty(),
                leadingIcon = Icons.Default.PlayArrow,
            )
            KpButton(
                text = stringResource(R.string.library_shuffle_all),
                onClick = { onEvent(DetailEvent.ShuffleAll) },
                enabled = trackList.isNotEmpty(),
                style = KpButtonStyle.Tonal,
                leadingIcon = KpIcons.Shuffle,
            )
        }
    }
}
