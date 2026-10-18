package com.kingplay.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingplay.core.model.Album
import com.kingplay.core.model.Artist
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.Track
import com.kingplay.desktop.playback.DesktopPlaybackController

@Composable
fun SongList(
    tracks: List<Track>,
    currentId: Long?,
    controller: DesktopPlaybackController?,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (header != null) item { header() }
        itemsIndexed(tracks, key = { _, t -> t.id }) { position, track ->
            val playing = track.id == currentId
            val description = "${track.title}, ${track.artistName}, ${track.albumTitle}"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clickable(enabled = controller != null, role = Role.Button) {
                        controller?.playMedia(tracks.map { MediaRef.Audio(it.id) }, position)
                    }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = description
                        selected = playing
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${track.artistName} · ${track.albumTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(formatTime(track.durationMs), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun AlbumList(albums: List<Album>, onOpen: (Album) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(albums, key = { _, a -> a.id }) { _, album ->
            ListRow(
                title = album.title,
                subtitle = "${album.artistName} · ${album.trackCount} songs",
                onClick = { onOpen(album) },
            )
        }
    }
}

@Composable
fun ArtistList(artists: List<Artist>, onOpen: (Artist) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(artists, key = { _, a -> a.id }) { _, artist ->
            ListRow(
                title = artist.name,
                subtitle = "${artist.albumCount} albums · ${artist.trackCount} songs",
                onClick = { onOpen(artist) },
            )
        }
    }
}

@Composable
private fun ListRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$title, $subtitle" },
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun EmptyLibrary(playerMissing: Boolean, onAddFolder: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Your library is empty", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Choose the folder where your music is kept. KingPlay reads it and never changes your files.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Button(onClick = onAddFolder) { Text("Add music folder") }
        if (playerMissing) {
            Text(
                "Playback needs VLC (64-bit) installed. Get it from videolan.org, then restart KingPlay.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
