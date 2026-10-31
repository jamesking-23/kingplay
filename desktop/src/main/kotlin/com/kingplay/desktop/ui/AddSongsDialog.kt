package com.kingplay.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingplay.core.model.Track

/** Tick any number of songs (with a search box) and add them to a playlist in one go. */
@Composable
fun AddSongsDialog(tracks: List<Track>, onAdd: (List<Long>) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf(emptySet<Long>()) }
    val shown = remember(tracks, query) {
        val q = query.trim()
        if (q.isEmpty()) {
            tracks
        } else {
            tracks.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.artistName.contains(q, ignoreCase = true) ||
                    it.albumTitle.contains(q, ignoreCase = true)
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.width(560.dp),
        title = { Text("Add songs") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search songs, artists, albums") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp).padding(top = 8.dp)) {
                    items(shown, key = { it.id }) { track ->
                        val on = track.id in picked
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Checkbox) {
                                    picked = if (on) picked - track.id else picked + track.id
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = on, onCheckedChange = null, modifier = Modifier.padding(end = 12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${track.artistName} · ${track.albumTitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = picked.isNotEmpty(),
                onClick = {
                    onAdd(tracks.filter { it.id in picked }.map { it.id })
                    onDismiss()
                },
            ) { Text("Add (${picked.size})") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** After an update, lists what is new once. A fresh install records the version silently and shows nothing. */
@Composable
fun WhatsNewDialog(lastSeen: String, installed: String, onSeen: (String) -> Unit) {
    var open by remember { mutableStateOf(true) }
    LaunchedEffect(lastSeen, installed) {
        if (lastSeen.isBlank() && installed.isNotBlank() && installed != "dev") onSeen(installed)
    }
    if (!open || !com.kingplay.core.model.update.WhatsNew.shouldShow(lastSeen, installed)) return
    AlertDialog(
        onDismissRequest = {
            open = false
            onSeen(installed)
        },
        title = { Text("What's new in $installed") },
        text = {
            Column {
                com.kingplay.core.model.update.WhatsNew.highlights.forEach { line ->
                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                        Text("•  ")
                        Text(line)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                open = false
                onSeen(installed)
            }) { Text("Got it") }
        },
    )
}
