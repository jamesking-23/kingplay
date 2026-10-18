package com.kingplay.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.Track
import com.kingplay.core.model.repository.PlaybackState
import com.kingplay.desktop.DesktopAppState
import com.kingplay.desktop.ScanStatus
import com.kingplay.desktop.cover.CoverRepository
import com.kingplay.desktop.library.LibraryIndex
import com.kingplay.desktop.library.sortedForList
import com.kingplay.desktop.settings.DesktopSettings
import com.kingplay.desktop.userdata.UserData
import com.kingplay.desktop.userdata.favoriteTracks
import com.kingplay.desktop.userdata.mostPlayed
import com.kingplay.desktop.userdata.playlistTracks
import com.kingplay.desktop.userdata.recentlyAdded
import com.kingplay.desktop.userdata.recentlyPlayed
import com.kingplay.desktop.video.VideoFile
import kotlinx.coroutines.flow.MutableStateFlow

private enum class SmartKind(val title: String) {
    Favorites("Favorites"),
    MostPlayed("Most played"),
    RecentlyPlayed("Recently played"),
    RecentlyAdded("Recently added"),
}

private sealed interface Page {
    data object Songs : Page

    data object Albums : Page

    data object Artists : Page

    data object Playlists : Page

    data object Queue : Page

    data object Lyrics : Page

    data object Equalizer : Page

    data object Videos : Page

    data object Library : Page

    data object Settings : Page

    data class AlbumDetail(val id: Long, val title: String) : Page

    data class ArtistDetail(val id: Long, val name: String) : Page

    data class PlaylistDetail(val id: Long) : Page

    data class Smart(val kind: SmartKind) : Page
}

private fun smartTracks(kind: SmartKind, index: LibraryIndex, user: UserData): List<Track> = when (kind) {
    SmartKind.Favorites -> favoriteTracks(index, user)
    SmartKind.MostPlayed -> mostPlayed(index, user)
    SmartKind.RecentlyPlayed -> recentlyPlayed(index, user)
    SmartKind.RecentlyAdded -> recentlyAdded(index)
}

@Composable
fun App(
    app: DesktopAppState,
    covers: CoverRepository,
    onOpenMini: () -> Unit,
    onPlayVideo: (VideoFile, startMs: Long) -> Unit,
) {
    val index by app.index.collectAsState()
    val folders by app.folders.collectAsState()
    val scan by app.scan.collectAsState()
    val user by app.userData.collectAsState()
    val settings by app.settings.collectAsState()
    val videos by app.videos.collectAsState()
    val idle = remember { MutableStateFlow(PlaybackState()) }
    val playback by (app.controller?.state ?: idle).collectAsState()

    var page by remember { mutableStateOf<Page>(Page.Songs) }
    var query by remember { mutableStateOf("") }
    var nameDialog by remember { mutableStateOf<NameDialogState?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val currentId = (playback.current as? MediaRef.Audio)?.id

    val actions = SongActions(
        favorites = user.favorites,
        playlists = user.playlists,
        toggleFavorite = app::toggleFavorite,
        playNext = { app.controller?.playNext(listOf(MediaRef.Audio(it.id))) },
        addToQueue = { app.controller?.addToQueue(listOf(MediaRef.Audio(it.id))) },
        addToPlaylist = { playlistId, track -> app.updateUser { it.addToPlaylist(playlistId, listOf(track.id)) } },
        newPlaylistWith = { track ->
            nameDialog = NameDialogState("New playlist", "", "Create") { app.createPlaylist(it, listOf(track)) }
        },
    )

    KingPlayDesktopTheme(settings) {
        CompositionLocalProvider(LocalCovers provides covers) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        Rail(page) { page = it }
                        Column(modifier = Modifier.weight(1f)) {
                            if (scan is ScanStatus.Scanning) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Text(
                                    "Reading your music… ${(scan as ScanStatus.Scanning).found} songs found",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                )
                            }
                            notice?.let { Notice(it) { notice = null } }
                            if (index.tracks.isEmpty() && folders.isEmpty() && page !is Page.Library) {
                                EmptyLibrary(app.playerMissing, onAddFolder = { chooseFolder()?.let(app::addFolder) })
                            } else {
                                PageContent(
                                    page = page,
                                    onPage = { page = it },
                                    app = app,
                                    index = index,
                                    folders = folders,
                                    user = user,
                                    playback = playback,
                                    settings = settings,
                                    currentId = currentId,
                                    actions = actions,
                                    query = query,
                                    onQuery = { query = it },
                                    onDialog = { nameDialog = it },
                                    onNotice = { notice = it },
                                    onOpenMini = onOpenMini,
                                    videos = videos,
                                    onPlayVideo = onPlayVideo,
                                )
                            }
                        }
                    }
                    app.controller?.let {
                        NowPlayingBar(
                            controller = it,
                            state = playback,
                            currentTrack = currentId?.let(index::get),
                            initialVolumePercent = settings.volumePercent,
                            onVolumeChange = app::setVolume,
                        )
                    }
                }
            }
            nameDialog?.let { NameDialog(it) { nameDialog = null } }
        }
    }
}

@Composable
private fun Rail(page: Page, onPage: (Page) -> Unit) {
    // A plain scrolling column rather than NavigationRail, so every item stays reachable in a short window.
    Column(
        modifier = Modifier
            .width(RAIL_WIDTH)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RailItem("Songs", Icons.Filled.MusicNote, page is Page.Songs) { onPage(Page.Songs) }
        RailItem("Albums", Icons.Filled.Album, page is Page.Albums || page is Page.AlbumDetail) { onPage(Page.Albums) }
        RailItem("Artists", Icons.Filled.Person, page is Page.Artists || page is Page.ArtistDetail) {
            onPage(Page.Artists)
        }
        RailItem(
            "Playlists",
            Icons.Filled.LibraryMusic,
            page is Page.Playlists || page is Page.PlaylistDetail || page is Page.Smart,
        ) { onPage(Page.Playlists) }
        RailItem("Queue", Icons.AutoMirrored.Filled.QueueMusic, page is Page.Queue) { onPage(Page.Queue) }
        RailItem("Lyrics", Icons.Filled.Lyrics, page is Page.Lyrics) { onPage(Page.Lyrics) }
        RailItem("Equalizer", Icons.Filled.Equalizer, page is Page.Equalizer) { onPage(Page.Equalizer) }
        RailItem("Videos", Icons.Filled.Movie, page is Page.Videos) { onPage(Page.Videos) }
        RailItem("Library", Icons.Filled.Folder, page is Page.Library) { onPage(Page.Library) }
        RailItem("Settings", Icons.Filled.Settings, page is Page.Settings) { onPage(Page.Settings) }
    }
}

private val RAIL_WIDTH = 88.dp

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
private fun Notice(message: String, onDismiss: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Dismiss message") }
        }
    }
}

@Composable
private fun PageContent(
    page: Page,
    onPage: (Page) -> Unit,
    app: DesktopAppState,
    index: LibraryIndex,
    folders: List<String>,
    user: UserData,
    playback: PlaybackState,
    settings: DesktopSettings,
    currentId: Long?,
    actions: SongActions,
    query: String,
    onQuery: (String) -> Unit,
    onDialog: (NameDialogState) -> Unit,
    onNotice: (String) -> Unit,
    onOpenMini: () -> Unit,
    videos: List<VideoFile>,
    onPlayVideo: (VideoFile, startMs: Long) -> Unit,
) {
    when (page) {
        Page.Songs -> {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                label = { Text("Search songs, artists, albums") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            SongList(index.search(query).sortedForList(), currentId, app.controller, actions)
        }
        Page.Albums -> AlbumList(
            index.albums,
            coverOf = { index.firstTrackOfAlbum[it.id] },
            onOpen = { onPage(Page.AlbumDetail(it.id, it.title)) },
        )
        Page.Artists -> ArtistList(index.artists, onOpen = { onPage(Page.ArtistDetail(it.id, it.name)) })
        Page.Playlists -> PlaylistsPage(
            app = app,
            index = index,
            user = user,
            onOpen = onPage,
            onDialog = onDialog,
            onNotice = onNotice,
        )
        Page.Queue -> QueuePage(app, index, playback, onDialog)
        Page.Lyrics -> LyricsPage(
            app = app,
            track = currentId?.let(index::get),
            positionMs = playback.positionMs,
            importedKeys = user.lyrics.keys,
            onNotice = onNotice,
        )
        Page.Equalizer -> EqualizerPage(
            settings = settings,
            savedPresets = user.eqPresets,
            onChange = app::updateEqualizer,
            onSavePreset = { preset -> app.updateUser { it.withEqPreset(preset) } },
            onDeletePreset = { name -> app.updateUser { it.withoutEqPreset(name) } },
            onDialog = onDialog,
        )
        Page.Videos -> VideosPage(videos, user.videoProgress, onPlayVideo)
        Page.Library -> LibraryPage(
            folders = folders,
            onAdd = { chooseFolder()?.let(app::addFolder) },
            onRemove = app::removeFolder,
            onRescan = app::rescan,
        )
        Page.Settings -> SettingsPage(
            settings = settings,
            onChange = app::updateSettings,
            onImportBackup = { importBackup(app, onNotice) },
            onExportBackup = { exportBackup(app, onNotice) },
            onOpenMini = onOpenMini,
        )
        is Page.AlbumDetail -> Detail(
            title = page.title,
            tracks = index.albumTracks(page.id),
            currentId = currentId,
            app = app,
            actions = actions,
            onBack = { onPage(Page.Albums) },
        )
        is Page.ArtistDetail -> Detail(
            title = page.name,
            tracks = index.artistTracks(page.id),
            currentId = currentId,
            app = app,
            actions = actions,
            onBack = { onPage(Page.Artists) },
        )
        is Page.Smart -> Detail(
            title = page.kind.title,
            tracks = smartTracks(page.kind, index, user),
            currentId = currentId,
            app = app,
            actions = actions,
            onBack = { onPage(Page.Playlists) },
        )
        is Page.PlaylistDetail -> PlaylistDetail(
            id = page.id,
            app = app,
            index = index,
            user = user,
            currentId = currentId,
            actions = actions,
            onBack = { onPage(Page.Playlists) },
            onDialog = onDialog,
            onNotice = onNotice,
        )
    }
}

@Composable
private fun DetailHeader(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun PlayAllButton(app: DesktopAppState, tracks: List<Track>) {
    Button(
        onClick = { app.controller?.playMedia(tracks.map { MediaRef.Audio(it.id) }, 0) },
        enabled = tracks.isNotEmpty() && app.controller != null,
    ) { Text("Play all") }
}

@Composable
private fun Detail(
    title: String,
    tracks: List<Track>,
    currentId: Long?,
    app: DesktopAppState,
    actions: SongActions,
    onBack: () -> Unit,
) {
    SongList(
        tracks = tracks,
        currentId = currentId,
        controller = app.controller,
        actions = actions,
        header = { DetailHeader(title, onBack) { PlayAllButton(app, tracks) } },
    )
}

@Composable
private fun PlaylistsPage(
    app: DesktopAppState,
    index: LibraryIndex,
    user: UserData,
    onOpen: (Page) -> Unit,
    onDialog: (NameDialogState) -> Unit,
    onNotice: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = {
                    onDialog(NameDialogState("New playlist", "", "Create") { app.createPlaylist(it) })
                }) { Text("New playlist") }
                OutlinedButton(onClick = { importM3u(app, onNotice) }) { Text("Import .m3u") }
            }
        }
        items(SmartKind.entries, key = { it.name }) { kind ->
            val count = smartTracks(kind, index, user).size
            NavRow(kind.title, "$count songs") { onOpen(Page.Smart(kind)) }
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
        if (user.playlists.isEmpty()) {
            item {
                Text(
                    "No playlists yet. Make one here, or add songs from the ⋮ menu on any song.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        items(user.playlists, key = { it.id }) { playlist ->
            val shown = playlistTracks(playlist, index).size
            val missing = playlist.trackIds.size - shown
            val subtitle = "$shown songs" + if (missing > 0) " · $missing not in library" else ""
            NavRow(playlist.name, subtitle) { onOpen(Page.PlaylistDetail(playlist.id)) }
        }
    }
}

@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlaylistDetail(
    id: Long,
    app: DesktopAppState,
    index: LibraryIndex,
    user: UserData,
    currentId: Long?,
    actions: SongActions,
    onBack: () -> Unit,
    onDialog: (NameDialogState) -> Unit,
    onNotice: (String) -> Unit,
) {
    val playlist = user.playlist(id)
    var confirmDelete by remember { mutableStateOf(false) }
    if (playlist == null) {
        // Deleted (here or elsewhere): leave the page instead of showing an empty one.
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val positions = playlist.trackIds.indices.filter { index[playlist.trackIds[it]] != null }
    val tracks = playlistTracks(playlist, index)
    SongList(
        tracks = tracks,
        currentId = currentId,
        controller = app.controller,
        actions = actions,
        onRemoveAt = { shown -> app.updateUser { it.removeFromPlaylist(id, positions[shown]) } },
        onMove = { from, to -> app.updateUser { it.moveInPlaylist(id, positions[from], positions[to]) } },
        header = {
            DetailHeader(playlist.name, onBack) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = {
                        onDialog(NameDialogState("Rename playlist", playlist.name, "Rename") { name ->
                            app.updateUser { it.renamePlaylist(id, name) }
                        })
                    }) { Text("Rename") }
                    TextButton(onClick = {
                        val text = app.exportM3u(id)
                        val saved = text?.let { saveTextFile("Export playlist", "${playlist.name}.m3u", it) }
                        if (saved != null) onNotice("Saved $saved")
                    }) { Text("Export") }
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
                    PlayAllButton(app, tracks)
                }
            }
        },
    )
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete playlist?",
            message = "\"${playlist.name}\" will be removed. Your songs are not deleted.",
            confirmLabel = "Delete",
            onConfirm = { app.updateUser { it.deletePlaylist(id) } },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun QueuePage(
    app: DesktopAppState,
    index: LibraryIndex,
    playback: PlaybackState,
    onDialog: (NameDialogState) -> Unit,
) {
    val controller = app.controller
    val items = playback.queue.mapNotNull { (it as? MediaRef.Audio)?.id }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Queue", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            OutlinedButton(
                enabled = items.isNotEmpty(),
                onClick = {
                    onDialog(NameDialogState("Save queue as playlist", "", "Save") { name ->
                        app.createPlaylist(name, items.mapNotNull { index[it] })
                    })
                },
            ) { Text("Save as playlist") }
            OutlinedButton(enabled = items.isNotEmpty(), onClick = { controller?.clearQueue() }) { Text("Clear") }
        }
        if (items.isEmpty()) {
            Text(
                "The queue is empty. Play a song, or use ⋮ → Add to queue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(items, key = { position, id -> "$position-$id" }) { position, id ->
                val track = index[id]
                val current = position == playback.currentIndex
                val spec = ReorderSpec(position, items.size) { from, to -> controller?.moveInQueue(from, to) }
                val drag = rememberDragState(spec)
                var rowMenu by remember { mutableStateOf(false) }
                Row(
                    modifier = drag.rowModifier
                        .fillMaxWidth()
                        .height(REORDER_ROW_HEIGHT)
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .clickable(role = Role.Button) { controller?.skipToQueueIndex(position) }
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            track?.title ?: "Missing song",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (current) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            track?.artistName.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    Box {
                        IconButton(onClick = { rowMenu = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "More actions for ${track?.title ?: "song"}",
                            )
                        }
                        DropdownMenu(expanded = rowMenu, onDismissRequest = { rowMenu = false }) {
                            stepTarget(position, -1, items.size)?.let { to ->
                                DropdownMenuItem(text = { Text("Move up") }, onClick = {
                                    rowMenu = false
                                    controller?.moveInQueue(position, to)
                                })
                            }
                            stepTarget(position, 1, items.size)?.let { to ->
                                DropdownMenuItem(text = { Text("Move down") }, onClick = {
                                    rowMenu = false
                                    controller?.moveInQueue(position, to)
                                })
                            }
                        }
                    }
                    IconButton(onClick = { controller?.removeFromQueue(position) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove ${track?.title ?: "song"} from queue")
                    }
                    ReorderHandle(drag, "Drag to move ${track?.title ?: "song"}")
                }
            }
        }
    }
}

@Composable
private fun LibraryPage(
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
        LazyColumn(modifier = Modifier.weight(1f)) {
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

private fun importM3u(app: DesktopAppState, onNotice: (String) -> Unit) {
    val (name, text) = chooseTextFile("Choose a playlist", "Playlist (.m3u)", "m3u", "m3u8") ?: return
    val (found, total) = app.importM3u(name, text)
    onNotice("Created \"$name\" with $found of $total songs.")
}
