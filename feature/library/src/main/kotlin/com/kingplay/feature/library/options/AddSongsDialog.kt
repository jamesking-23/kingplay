package com.kingplay.feature.library.options

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.kingplay.core.model.Track
import com.kingplay.core.ui.displayName
import com.kingplay.core.ui.joinWithDot
import com.kingplay.feature.library.R

/** Pick any number of songs from the library (or from search matches) and add them to one playlist. */
@Composable
fun AddSongsDialog(
    playlistId: Long,
    onDismiss: () -> Unit,
    viewModel: AddSongsViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val found by viewModel.found.collectAsStateWithLifecycle()
    val paged = viewModel.all.collectAsLazyPagingItems()
    var picked by remember { mutableStateOf(emptySet<Long>()) }
    fun toggle(track: Track) {
        picked = if (track.id in picked) picked - track.id else picked + track.id
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_songs_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.query.value = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp).padding(top = 8.dp)) {
                    val matches = found
                    if (matches != null) {
                        items(matches, key = { it.id }) { SongPickRow(it, it.id in picked) { toggle(it) } }
                    } else {
                        items(paged.itemCount) { index ->
                            paged[index]?.let { SongPickRow(it, it.id in picked) { toggle(it) } }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = picked.isNotEmpty(),
                onClick = {
                    viewModel.add(playlistId, picked)
                    onDismiss()
                },
            ) { Text(stringResource(R.string.add_songs_confirm, picked.size)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
private fun SongPickRow(track: Track, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(end = 12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                joinWithDot(displayName(track.artistName), displayName(track.albumTitle)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
