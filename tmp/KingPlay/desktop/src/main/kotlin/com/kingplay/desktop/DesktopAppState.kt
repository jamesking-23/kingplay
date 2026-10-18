package com.kingplay.desktop

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.Track
import com.kingplay.core.model.playlist.M3uEntry
import com.kingplay.core.model.playlist.M3uParser
import com.kingplay.core.model.playlist.M3uWriter
import com.kingplay.core.model.playlist.PathRow
import com.kingplay.core.model.playlist.TrackPathIndex
import com.kingplay.desktop.engine.AudioEngine
import com.kingplay.desktop.library.LibraryIndex
import com.kingplay.desktop.library.LibraryStore
import com.kingplay.desktop.library.SavedLibrary
import com.kingplay.desktop.library.TrackScanner
import com.kingplay.desktop.playback.DesktopPlaybackController
import com.kingplay.desktop.userdata.BackupImport
import com.kingplay.desktop.userdata.ImportOutcome
import com.kingplay.desktop.userdata.SavedQueue
import com.kingplay.desktop.userdata.UserData
import com.kingplay.desktop.userdata.UserDataStore
import com.kingplay.desktop.userdata.restorableQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ScanStatus {
    data object Idle : ScanStatus

    data class Scanning(val found: Int) : ScanStatus
}

/** Everything the window shows that is not playback itself: the library, the person's own data and the scan. */
class DesktopAppState(
    private val scope: CoroutineScope,
    private val store: LibraryStore,
    private val userStore: UserDataStore,
    private val scanner: TrackScanner,
    engine: AudioEngine?,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val _index = MutableStateFlow(LibraryIndex.Empty)
    val index: StateFlow<LibraryIndex> = _index.asStateFlow()

    private val _folders = MutableStateFlow<List<String>>(emptyList())
    val folders: StateFlow<List<String>> = _folders.asStateFlow()

    private val _scan = MutableStateFlow<ScanStatus>(ScanStatus.Idle)
    val scan: StateFlow<ScanStatus> = _scan.asStateFlow()

    private val _user = MutableStateFlow(UserData())
    val userData: StateFlow<UserData> = _user.asStateFlow()

    private val scanLock = Mutex()
    private val dirty = Channel<Unit>(Channel.CONFLATED)

    /** Null when VLC could not be loaded; the UI then explains what to install. */
    val controller: DesktopPlaybackController? = engine?.let {
        DesktopPlaybackController(
            engine = it,
            lookup = { id -> _index.value[id] },
            scope = scope,
            onCounted = { track -> updateUser { data -> data.recordPlay(track.id, now()) } },
        )
    }

    val playerMissing: Boolean = engine == null

    fun start() {
        scope.launch {
            val (saved, user) = withContext(Dispatchers.IO) { store.load() to userStore.load() }
            _folders.value = saved.folders
            _index.value = LibraryIndex(saved.tracks)
            _user.value = user
            restoreQueue(user)
            launch { writeUserDataLoop() }
            launch { saveQueueLoop() }
            if (saved.folders.isNotEmpty()) rescan()
        }
    }

    private fun restoreQueue(user: UserData) {
        val saved = restorableQueue(user.queue, _index.value) ?: return
        controller?.restore(
            refs = saved.trackIds.map { MediaRef.Audio(it) },
            index = saved.index,
            positionMs = saved.positionMs,
            shuffle = saved.shuffle,
            repeat = saved.repeat,
        )
    }

    // ---- the person's own data -------------------------------------------------------------------------

    fun updateUser(change: (UserData) -> UserData) {
        _user.update(change)
        dirty.trySend(Unit)
    }

    /** Waits a moment after the last change so a burst of edits is written once. */
    private suspend fun writeUserDataLoop() {
        for (signal in dirty) {
            delay(SAVE_DELAY_MS)
            withContext(Dispatchers.IO) { runCatching { userStore.save(_user.value) } }
        }
    }

    private suspend fun saveQueueLoop() {
        while (scope.isActive) {
            delay(QUEUE_SAVE_INTERVAL_MS)
            rememberQueue()
        }
    }

    private fun rememberQueue() {
        val snap = controller?.snapshot()
        val saved = snap?.let { SavedQueue(it.trackIds, it.index, it.positionMs, it.shuffle, it.repeat) }
        if (saved != _user.value.queue) updateUser { it.withQueue(saved) }
    }

    /** Writes everything now. Called when the window closes. */
    fun flush() {
        rememberQueue()
        runCatching { userStore.save(_user.value) }
    }

    fun toggleFavorite(trackId: Long) = updateUser { it.toggleFavorite(trackId) }

    fun createPlaylist(name: String, tracks: List<Track> = emptyList()): Long {
        var id = 0L
        updateUser { data ->
            val (next, newId) = data.createPlaylist(name, now(), tracks.map { it.id })
            id = newId
            next
        }
        return id
    }

    fun importBackup(text: String): ImportOutcome {
        val outcome = BackupImport.import(text, _index.value, _user.value, now())
        if (outcome is ImportOutcome.Done) updateUser { outcome.data }
        return outcome
    }

    /** Creates a playlist from an M3U file. Returns (songs found, songs listed). */
    fun importM3u(name: String, text: String): Pair<Int, Int> {
        val paths = M3uParser.parse(text)
        val rows = _index.value.tracks.map {
            PathRow(it.id, it.folderPath.replace('\\', '/'), it.uri.replace('\\', '/').substringAfterLast('/'))
        }
        val matcher = TrackPathIndex(rows)
        val ids = paths.mapNotNull { matcher.match(it) }
        updateUser { it.createPlaylist(name, now(), ids).first }
        return ids.size to paths.size
    }

    fun exportM3u(playlistId: Long): String? {
        val playlist = _user.value.playlist(playlistId) ?: return null
        val entries = playlist.trackIds.mapNotNull { _index.value[it] }
            .map { M3uEntry(it.uri.replace('\\', '/'), (it.durationMs / MS_PER_SECOND).toInt(), it.title) }
        return M3uWriter.write(entries)
    }

    // ---- library ---------------------------------------------------------------------------------------

    fun addFolder(path: String) {
        if (path in _folders.value) return
        _folders.value = _folders.value + path
        rescan()
    }

    fun removeFolder(path: String) {
        _folders.value = _folders.value - path
        rescan()
    }

    fun rescan() {
        scope.launch {
            scanLock.withLock {
                val roots = _folders.value
                val previous = _index.value.tracks.associateBy { it.uri }
                _scan.value = ScanStatus.Scanning(0)
                val tracks = withContext(Dispatchers.IO) {
                    scanner.scan(roots, previous) { found -> _scan.value = ScanStatus.Scanning(found) }
                }
                _index.value = LibraryIndex(tracks)
                withContext(Dispatchers.IO) { runCatching { store.save(SavedLibrary(roots, tracks)) } }
                _scan.value = ScanStatus.Idle
            }
        }
    }

    companion object {
        private const val SAVE_DELAY_MS = 400L
        private const val QUEUE_SAVE_INTERVAL_MS = 5_000L
        private const val MS_PER_SECOND = 1000

        /** %APPDATA%\KingPlay on Windows; a hidden folder in the home directory elsewhere. */
        fun dataDirectory(): File {
            val appData = System.getenv("APPDATA")
            val home = System.getProperty("user.home")
            return if (appData != null) File(appData, "KingPlay") else File(home, ".kingplay")
        }
    }
}
