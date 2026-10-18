package com.kingplay.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.kingplay.desktop.userdata.DesktopPlaylist

/** Everything a song row can do besides play. Built once in [App] and shared by every list. */
class SongActions(
    val favorites: Set<Long>,
    val playlists: List<DesktopPlaylist>,
    val toggleFavorite: (Long) -> Unit,
    val playNext: (Track) -> Unit,
    val addToQueue: (Track) -> Unit,
    val addToPlaylist: (playlistId: Long, track: Track) -> Unit,
    val newPlaylistWith: (Track) -> Unit,
)

@Composable
fun SongList(
    tracks: List<Track>,
    currentId: Long?,
    controller: DesktopPlaybackController?,
    actions: SongActions,
    modifier: Modifier = Modifier,
    /** When set, each row's menu offers "Remove from this list" and calls this with the row's position. */
    onRemoveAt: ((Int) -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (header != null) item { header() }
        itemsIndexed(tracks, key = { position, t -> "$position-${t.id}" }) { position, track ->
            SongRow(
                track = track,
                playing = track.id == currentId,
                favorite = track.id in actions.favorites,
                actions = actions,
                onPlay = { controller?.playMedia(tracks.map { MediaRef.Audio(it.id) }, position) },
                canPlay = controller != null,
                onRemove = onRemoveAt?.let { remove -> { remove(position) } },
            )
        }
    }
}

@Composable
private fun SongRow(
    track: Track,
    playing: Boolean,
    favorite: Boolean,
    actions: SongActions,
    onPlay: () -> Unit,
    canPlay: Boolean,
    onRemove: (() -> Unit)?,
) {
    val description = "${track.title}, ${track.artistName}, ${track.albumTitle}"
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .clickable(enabled = canPlay, role = Role.Button, onClick = onPlay)
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    selected = playing
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverImage(track, COVER_ROW_SIZE, modifier = Modifier.padding(end = 12.dp))
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
            Text(
                formatTime(track.durationMs),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        IconButton(onClick = { actions.toggleFavorite(track.id) }) {
            Icon(
                imageVector = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (favorite) {
                    "Remove ${track.title} from favorites"
                } else {
                    "Add ${track.title} to favorites"
                },
                tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More actions for ${track.title}")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Play next") }, onClick = {
                    menuOpen = false
                    actions.playNext(track)
                })
                DropdownMenuItem(text = { Text("Add to queue") }, onClick = {
                    menuOpen = false
                    actions.addToQueue(track)
                })
                HorizontalDivider()
                actions.playlists.forEach { playlist ->
                    DropdownMenuItem(text = { Text("Add to ${playlist.name}") }, onClick = {
                        menuOpen = false
                        actions.addToPlaylist(playlist.id, track)
                    })
                }
                DropdownMenuItem(text = { Text("Add to new playlist…") }, onClick = {
                    menuOpen = false
                    actions.newPlaylistWith(track)
                })
                if (onRemove != null) {
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Remove from this list") }, onClick = {
                        menuOpen = false
                        onRemove()
                    })
                }
            }
        }
    }
}

@Composable
fun AlbumList(
    albums: List<Album>,
    coverOf: (Album) -> Track?,
    onOpen: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(albums, key = { _, a -> a.id }) { _, album ->
            ListRow(
                title = album.title,
                subtitle = "${album.artistName} · ${album.trackCount} songs",
                cover = coverOf(album),
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
private fun ListRow(title: String, subtitle: String, onClick: () -> Unit, cover: Track? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$title, $subtitle" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cover != null) CoverImage(cover, COVER_ROW_SIZE, modifier = Modifier.padding(end = 12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
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
}

private val COVER_ROW_SIZE = 44.dp

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
