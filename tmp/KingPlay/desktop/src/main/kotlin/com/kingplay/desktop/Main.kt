package com.kingplay.desktop

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.kingplay.desktop.engine.VlcjAudioEngine
import com.kingplay.desktop.library.JAudioTaggerReader
import com.kingplay.desktop.library.LibraryStore
import com.kingplay.desktop.library.TrackScanner
import com.kingplay.desktop.ui.App
import java.io.File

fun main() = application {
    val scope = rememberCoroutineScope()
    val engine = remember { VlcjAudioEngine.createOrNull() }
    val app = remember {
        DesktopAppState(
            scope = scope,
            store = LibraryStore(File(DesktopAppState.dataDirectory(), "library.json")),
            scanner = TrackScanner(JAudioTaggerReader()),
            engine = engine,
        ).also { it.start() }
    }

    Window(
        onCloseRequest = {
            engine?.release()
            exitApplication()
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
        App(app)
    }
}
