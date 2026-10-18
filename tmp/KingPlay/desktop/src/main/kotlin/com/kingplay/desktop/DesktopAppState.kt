package com.kingplay.desktop

import com.kingplay.desktop.engine.AudioEngine
import com.kingplay.desktop.library.LibraryIndex
import com.kingplay.desktop.library.LibraryStore
import com.kingplay.desktop.library.SavedLibrary
import com.kingplay.desktop.library.TrackScanner
import com.kingplay.desktop.playback.DesktopPlaybackController
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface ScanStatus {
    data object Idle : ScanStatus

    data class Scanning(val found: Int) : ScanStatus
}

/** Everything the window shows that is not playback: the library, its folders and the scan. */
class DesktopAppState(
    private val scope: CoroutineScope,
    private val store: LibraryStore,
    private val scanner: TrackScanner,
    engine: AudioEngine?,
) {
    private val _index = MutableStateFlow(LibraryIndex.Empty)
    val index: StateFlow<LibraryIndex> = _index.asStateFlow()

    private val _folders = MutableStateFlow<List<String>>(emptyList())
    val folders: StateFlow<List<String>> = _folders.asStateFlow()

    private val _scan = MutableStateFlow<ScanStatus>(ScanStatus.Idle)
    val scan: StateFlow<ScanStatus> = _scan.asStateFlow()

    private val scanLock = Mutex()

    /** Null when VLC could not be loaded; the UI then explains what to install. */
    val controller: DesktopPlaybackController? =
        engine?.let { DesktopPlaybackController(it, { id -> _index.value[id] }, scope) }

    val playerMissing: Boolean = engine == null

    fun start() {
        scope.launch {
            val saved = withContext(Dispatchers.IO) { store.load() }
            _folders.value = saved.folders
            _index.value = LibraryIndex(saved.tracks)
            if (saved.folders.isNotEmpty()) rescan()
        }
    }

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
        /** %APPDATA%\KingPlay on Windows; a hidden folder in the home directory elsewhere. */
        fun dataDirectory(): File {
            val appData = System.getenv("APPDATA")
            val home = System.getProperty("user.home")
            return if (appData != null) File(appData, "KingPlay") else File(home, ".kingplay")
        }
    }
}
