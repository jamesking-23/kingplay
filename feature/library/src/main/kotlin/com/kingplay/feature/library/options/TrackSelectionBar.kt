package com.kingplay.feature.library.options

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kingplay.core.model.Track
import com.kingplay.core.model.VideoItem
import com.kingplay.core.ui.R as UiR
import com.kingplay.core.ui.component.LocalTrackOptions
import com.kingplay.core.ui.component.SelectionBar
import com.kingplay.core.ui.component.SelectionState

/**
 * The bar for ticked songs: play next, add to queue, favorite, add to playlist, share and delete, all applied to
 * every ticked song. [resolve] hands over the ticked songs (they may have to be loaded first).
 */
@Composable
fun TrackSelectionBar(
    selection: SelectionState,
    onSelectAll: () -> Unit,
    resolve: ((List<Track>) -> Unit) -> Unit,
    onPlayNext: (List<Track>) -> Unit,
    onAddToQueue: (List<Track>) -> Unit,
    onToggleFavorite: (List<Track>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = LocalTrackOptions.current
    // Run the action, then leave selection mode (the delete screens keep their own state).
    fun act(then: (List<Track>) -> Unit) = resolve { tracks ->
        then(tracks)
        selection.clear()
    }
    SelectionBar(
        count = selection.count,
        onClose = selection::clear,
        onSelectAll = onSelectAll,
        modifier = modifier,
    ) { dismiss ->
        DropdownMenuItem(
            text = { Text(stringResource(UiR.string.ui_play_next)) },
            onClick = { dismiss(); act(onPlayNext) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(UiR.string.ui_add_to_queue)) },
            onClick = { dismiss(); act(onAddToQueue) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(UiR.string.ui_add_favorite)) },
            onClick = { dismiss(); act(onToggleFavorite) },
        )
        if (options != null) {
            DropdownMenuItem(
                text = { Text(stringResource(UiR.string.ui_add_to_playlist)) },
                onClick = { dismiss(); act(options::addToPlaylist) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(UiR.string.ui_share)) },
                onClick = { dismiss(); act(options::share) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(UiR.string.ui_delete_song), color = MaterialTheme.colorScheme.error) },
                onClick = { dismiss(); act(options::delete) },
            )
        }
    }
}

/** The bar for ticked videos: share and delete (videos do not go into playlists). */
@Composable
fun VideoSelectionBar(
    selection: SelectionState,
    videos: List<VideoItem>,
    modifier: Modifier = Modifier,
) {
    val options = LocalTrackOptions.current
    fun chosen() = videos.filter { it.id in selection }
    SelectionBar(
        count = selection.count,
        onClose = selection::clear,
        onSelectAll = { selection.selectAll(videos.map { it.id }) },
        modifier = modifier,
    ) { dismiss ->
        if (options != null) {
            DropdownMenuItem(
                text = { Text(stringResource(UiR.string.ui_share)) },
                onClick = {
                    dismiss()
                    options.shareVideos(chosen())
                    selection.clear()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(UiR.string.ui_delete_song), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    dismiss()
                    options.deleteVideos(chosen())
                    selection.clear()
                },
            )
        }
    }
}
