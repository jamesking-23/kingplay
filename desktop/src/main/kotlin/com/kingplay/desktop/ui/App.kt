package com.kingplay.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.Track
import com.kingplay.core.model.repository.PlaybackState
import com.kingplay.desktop.DesktopAppState
import com.kingplay.desktop.ScanStatus
import com.kingplay.desktop.library.sortedForList
import kotlinx.coroutines.flow.MutableStateFlow
import javax.swing.JFileChooser

private sealed interface Page {
    data object Songs : Page

    data object Albums : Page

    data object Artists : Page

    data object Folders : Page

    data class AlbumDetail(val id: Long, val title: String) : Page

    data class ArtistDetail(val id: Long, val name: String) : Page
}

@Composable
fun App(app: DesktopAppState) {
    val index by app.index.collectAsState()
    val folders by app.folders.collectAsState()
    val scan by app.scan.collectAsState()
    val idle = remember { MutableStateFlow(PlaybackState()) }
    val playback by (app.controller?.state ?: idle).collectAsState()

    var page by remember { mutableStateOf<Page>(Page.Songs) }
    var query by remember { mutableStateOf("") }
    val currentId = (playback.current as? MediaRef.Audio)?.id

    KingPlayDesktopTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f)) {
                    NavigationRail {
                        RailItem("Songs", Icons.Filled.MusicNote, page is Page.Songs) { page = Page.Songs }
                        RailItem("Albums", Icons.Filled.Album, page is Page.Albums || page is Page.AlbumDetail) {
                            page = Page.Albums
                        }
                        RailItem("Artists", Icons.Filled.Person, page is Page.Artists || page is Page.ArtistDetail) {
                            page = Page.Artists
                        }
                        RailItem("Folders", Icons.Filled.Folder, page is Page.Folders) { page = Page.Folders }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (scan is ScanStatus.Scanning) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                "Reading your music… ${(scan as ScanStatus.Scanning).found} songs found",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        if (index.tracks.isEmpty() && folders.isEmpty() && page !is Page.Folders) {
                            EmptyLibrary(app.playerMissing, onAddFolder = { chooseFolder()?.let(app::addFolder) })
                        } else {
                            when (val p = page) {
                                Page.Songs -> {
                                    OutlinedTextField(
                                        value = query,
                                        onValueChange = { query = it },
                                        label = { Text("Search songs, artists, albums") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    )
                                    SongList(index.search(query).sortedForList(), currentId, app.controller)
                                }
                                Page.Albums -> AlbumList(
                                    index.albums,
                                    onOpen = { page = Page.AlbumDetail(it.id, it.title) },
                                )
                                Page.Artists -> ArtistList(
                                    index.artists,
                                    onOpen = { page = Page.ArtistDetail(it.id, it.name) },
                                )
                                Page.Folders -> FolderPage(
                                    folders = folders,
                                    onAdd = { chooseFolder()?.let(app::addFolder) },
                                    onRemove = app::removeFolder,
                                    onRescan = app::rescan,
                                )
                                is Page.AlbumDetail -> Detail(
                                    title = p.title,
                                    tracks = index.albumTracks(p.id),
                                    currentId = currentId,
                                    app = app,
                                    onBack = { page = Page.Albums },
                                )
                                is Page.ArtistDetail -> Detail(
                                    title = p.name,
                                    tracks = index.artistTracks(p.id),
                                    currentId = currentId,
                                    app = app,
                                    onBack = { page = Page.Artists },
                                )
                            }
                        }
                    }
                }
                app.controller?.let { NowPlayingBar(it, playback) }
            }
        }
    }
}

@Composable
private fun RailItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
    )
}

@Composable
private fun Detail(
    title: String,
    tracks: List<Track>,
    currentId: Long?,
    app: DesktopAppState,
    onBack: () -> Unit,
) {
    SongList(
        tracks = tracks,
        currentId = currentId,
        controller = app.controller,
        header = {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Button(
                    onClick = { app.controller?.playMedia(tracks.map { MediaRef.Audio(it.id) }, 0) },
                    enabled = tracks.isNotEmpty() && app.controller != null,
                ) { Text("Play all") }
            }
        },
    )
}

@Composable
private fun FolderPage(
    folders: List<String>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onRescan: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Music folders", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAdd) { Text("Add folder") }
            OutlinedButton(onClick = onRescan, enabled = folders.isNotEmpty()) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text("Rescan", modifier = Modifier.padding(start = 8.dp))
            }
        }
        LazyColumn {
            items(folders, key = { it }) { path ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folderLabel(path), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            path,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onRemove(path) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove ${folderLabel(path)}")
                    }
                }
            }
        }
    }
}

/** Opens the system folder picker. Returns null when the person cancels. */
private fun chooseFolder(): String? {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Choose your music folder"
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile.absolutePath else null
}
