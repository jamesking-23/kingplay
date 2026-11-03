package com.kingplay.desktop.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingplay.core.model.Track

/**
 * The strip shown above a list while rows are ticked: how many, select all, clear, and what to do with them.
 * [content] adds the action buttons for the kind of list (songs or videos).
 */
@Composable
fun SelectionStrip(
    count: Int,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$count selected",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            TextButton(onClick = onSelectAll) { Text("Select all") }
            TextButton(onClick = onClear) { Text("Clear") }
            content()
        }
    }
}

/** Buttons for ticked songs. Each action gets the ticked songs and then the selection is cleared. */
@Composable
fun SongSelectionActions(
    chosen: List<Track>,
    allFavorite: Boolean,
    actions: SongActions,
    onDone: () -> Unit,
) {
    var playlistMenu by remember { mutableStateOf(false) }
    fun run(block: () -> Unit) {
        block()
        onDone()
    }
    OutlinedButton(onClick = { run { actions.playMany(chosen) } }) { Text("Play") }
    OutlinedButton(onClick = { run { actions.playNextMany(chosen) } }) { Text("Play next") }
    OutlinedButton(onClick = { run { actions.addToQueueMany(chosen) } }) { Text("Add to queue") }
    OutlinedButton(onClick = { run { actions.favoriteMany(chosen) } }) {
        Text(if (allFavorite) "Remove from favorites" else "Add to favorites")
    }
    Box {
        OutlinedButton(onClick = { playlistMenu = true }) { Text("Add to playlist") }
        DropdownMenu(expanded = playlistMenu, onDismissRequest = { playlistMenu = false }) {
            actions.playlists.forEach { playlist ->
                DropdownMenuItem(text = { Text(playlist.name) }, onClick = {
                    playlistMenu = false
                    run { actions.addToPlaylistMany(playlist.id, chosen) }
                })
            }
            DropdownMenuItem(text = { Text("New playlist…") }, onClick = {
                playlistMenu = false
                run { actions.newPlaylistWithMany(chosen) }
            })
        }
    }
    OutlinedButton(onClick = { run { actions.deleteMany(chosen) } }) {
        Text("Move to Recycle Bin…", color = MaterialTheme.colorScheme.error)
    }
}
