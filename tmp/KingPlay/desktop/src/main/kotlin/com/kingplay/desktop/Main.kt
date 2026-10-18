package com.kingplay.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.kingplay.desktop.library.JAudioTaggerReader
import com.kingplay.desktop.library.LibraryStore
import com.kingplay.desktop.library.TrackScanner
import com.kingplay.desktop.settings.SettingsStore
import com.kingplay.desktop.ui.App
import com.kingplay.desktop.ui.CloseAction
import com.kingplay.desktop.ui.MiniPlayer
import com.kingplay.desktop.ui.closeAction
import com.kingplay.desktop.ui.trayIconPainter
import com.kingplay.desktop.ui.trayTooltip
import com.kingplay.desktop.userdata.UserDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.SystemTray
import java.io.File

fun main() = application {
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
    val trayAvailable = remember { SystemTray.isSupported() }
    val settings by app.settings.collectAsState()
    val idle = remember { MutableStateFlow(PlaybackState()) }
    val playback by (app.controller?.state ?: idle).collectAsState()
    val quit = {
        app.flush()
        engine?.release()
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
                    Key.MediaNextTrack -> controller.skipToNext().let { true }
                    Key.MediaPreviousTrack -> controller.skipToPrevious().let { true }
                    else -> false
                }
            }
        },
    ) {
        App(app, covers, onOpenMini = { miniVisible = true })
    }
}
