package com.kingplay.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.kingplay.core.model.repository.PlaybackState
import com.kingplay.desktop.cover.CoverRepository
import com.kingplay.desktop.engine.VlcjAudioEngine
import com.kingplay.desktop.input.MediaKeyAction
import com.kingplay.desktop.input.WindowsMediaKeys
import com.kingplay.desktop.launch.LaunchRequest
import com.kingplay.desktop.launch.SingleInstance
import com.kingplay.desktop.launch.parseLaunchFiles
import com.kingplay.desktop.library.JAudioTaggerReader
import com.kingplay.desktop.library.LibraryStore
import com.kingplay.desktop.library.TrackScanner
import com.kingplay.desktop.settings.SettingsStore
import com.kingplay.desktop.ui.App
import com.kingplay.desktop.ui.CloseAction
import com.kingplay.desktop.ui.MiniPlayer
import com.kingplay.desktop.ui.VideoWindow
import com.kingplay.desktop.ui.closeAction
import com.kingplay.desktop.ui.trayIconPainter
import com.kingplay.desktop.ui.trayTooltip
import com.kingplay.desktop.userdata.UserDataStore
import com.kingplay.desktop.video.VideoQueue
import com.kingplay.desktop.video.videoFileFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.SystemTray
import java.io.File

fun main(args: Array<String>) {
    val inbox = Channel<LaunchRequest?>(Channel.UNLIMITED)
    val first = parseLaunchFiles(args.toList())
    val single = SingleInstance(SingleInstance.DEFAULT_PORT)
    val carryOn = single.acquireOrForward(first.all) { paths ->
        // null means "just bring the window forward"
        inbox.trySend(parseLaunchFiles(paths).takeUnless { it.isEmpty })
    }
    if (!carryOn) return
    if (!first.isEmpty) inbox.trySend(first)
    runApp(inbox)
}

private fun runApp(inbox: Channel<LaunchRequest?>) = application {
    val scope = rememberCoroutineScope()
    val engine = remember { VlcjAudioEngine.createOrNull() }
    val covers = remember { CoverRepository() }
    val app = remember {
        DesktopAppState(
            scope = scope,
            store = LibraryStore(File(DesktopAppState.dataDirectory(), "library.json")),
            userStore = UserDataStore(File(DesktopAppState.dataDirectory(), "user.json")),
            settingsStore = SettingsStore(File(DesktopAppState.dataDirectory(), "settings.json")),
            scanner = TrackScanner(JAudioTaggerReader()),
            engine = engine,
        ).also { it.start() }
    }

    var mainVisible by remember { mutableStateOf(true) }
    var miniVisible by remember { mutableStateOf(false) }
    var videoQueue by remember { mutableStateOf<VideoQueue?>(null) }
    var firstStartMs by remember { mutableStateOf(0L) }
    val trayAvailable = remember { SystemTray.isSupported() }
    val settings by app.settings.collectAsState()
    val idle = remember { MutableStateFlow(PlaybackState()) }
    val playback by (app.controller?.state ?: idle).collectAsState()
    LaunchedEffect(app) {
        for (request in inbox) {
            mainVisible = true
            request?.let {
                app.openAudioFiles(it.audio)
                it.videos.firstNotNullOfOrNull { path -> videoFileFor(File(path)) }?.let { video ->
                    app.pauseMusicForVideo()
                    videoQueue = VideoQueue(listOf(video))
                    firstStartMs = 0L
                }
            }
        }
    }
    LaunchedEffect(app) {
        val keysOn = app.settings.value.globalMediaKeys
        if (keysOn) {
            val keys = WindowsMediaKeys { action ->
                // The hot-key thread is not the UI thread: hand the work over.
                scope.launch {
                    val controller = app.controller
                    when (action) {
                        MediaKeyAction.PlayPause -> controller?.togglePlayPause()
                        MediaKeyAction.Next -> controller?.skipToNext()
                        MediaKeyAction.Previous -> controller?.skipToPrevious()
                        MediaKeyAction.Stop -> controller?.pause()
                    }
                }
            }
            withContext(Dispatchers.IO) { keys.start() }
        }
    }
    DisposableEffect(engine) {
        // Released only when the application is really ending, after everything that used it has stopped.
        onDispose { engine?.release() }
    }
    val quit = {
        app.flush()
        exitApplication()
    }
    val showMain = {
        mainVisible = true
        miniVisible = false
    }

    if (trayAvailable) {
        Tray(
            icon = remember { trayIconPainter() },
            tooltip = trayTooltip(playback),
            onAction = showMain,
            menu = {
                Item("Show KingPlay", onClick = showMain)
                Item("Mini player", onClick = { miniVisible = true })
                Separator()
                Item(if (playback.isPlaying) "Pause" else "Play", enabled = playback.hasMedia) {
                    app.controller?.togglePlayPause()
                }
                Item("Next song", enabled = playback.hasMedia) { app.controller?.skipToNext() }
                Item("Previous song", enabled = playback.hasMedia) { app.controller?.skipToPrevious() }
                Separator()
                Item("Quit", onClick = quit)
            },
        )
    }

    if (miniVisible) {
        Window(
            onCloseRequest = { miniVisible = false },
            title = "KingPlay mini player",
            state = rememberWindowState(size = DpSize(380.dp, 190.dp)),
            resizable = false,
            alwaysOnTop = true,
        ) {
            MiniPlayer(app, covers, onOpenMain = showMain)
        }
    }

    videoQueue?.let { queue ->
        val video = queue.current ?: return@let
        // Only the first video of a queue resumes; the ones after it start from the beginning.
        val startMs = firstStartMs
        key(video.id) {
            VideoWindow(
                video = video,
                startMs = startMs,
                settings = settings,
                onProgress = { position, duration -> app.saveVideoProgress(video, position, duration) },
                onStarted = app::addVideoPlay,
                onWatched = app::addVideoTime,
                onClose = { videoQueue = null },
                onNext = queue.next()?.let { next -> { firstStartMs = 0L; videoQueue = next } },
                onPrevious = queue.previous()?.let { prev -> { firstStartMs = 0L; videoQueue = prev } },
            )
        }
    }

    Window(
        visible = mainVisible,
        onCloseRequest = {
            when (closeAction(settings.closeToTray, trayAvailable)) {
                CloseAction.HideToTray -> mainVisible = false
                CloseAction.Quit -> quit()
            }
        },
        title = "KingPlay",
        state = rememberWindowState(size = DpSize(1100.dp, 720.dp)),
        onPreviewKeyEvent = { event ->
            val controller = app.controller
            if (controller == null || event.type != KeyEventType.KeyDown) {
                false
            } else {
                when (event.key) {
                    Key.MediaPlayPause -> controller.togglePlayPause().let { true }
                    Key.MediaNext -> controller.skipToNext().let { true }
                    Key.MediaPrevious -> controller.skipToPrevious().let { true }
                    else -> false
                }
            }
        },
    ) {
        App(
            app,
            covers,
            onOpenMini = { miniVisible = true },
            onPlayVideo = { video, startMs, queueAll ->
                app.pauseMusicForVideo()
                firstStartMs = startMs
                videoQueue = if (queueAll) {
                    VideoQueue.startingAt(app.videos.value, video)
                } else {
                    VideoQueue(listOf(video))
                }
            },
        )
    }
}
