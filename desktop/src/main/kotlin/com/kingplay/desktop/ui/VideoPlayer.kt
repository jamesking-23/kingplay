package com.kingplay.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.kingplay.desktop.settings.DesktopSettings
import com.kingplay.desktop.video.VideoFile
import kotlinx.coroutines.delay
import uk.co.caprica.vlcj.player.base.State
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

private enum class TrackPanel { Subtitles, Audio }

private const val SKIP_MS = 10_000L
private const val POLL_MS = 500L
private const val SURFACE_POLL_MS = 50L
private const val SAVE_EVERY_POLLS = 10

/** One choice in the subtitle or audio list. */
private data class TrackChoice(val id: Int, val label: String)

/**
 * A window that plays one video through VLC. Subtitle and audio choices open as a strip under the picture, not as a
 * pop-up, because the video surface is a native component that would hide a pop-up drawn over it.
 */
@Composable
fun VideoWindow(
    video: VideoFile,
    startMs: Long,
    settings: DesktopSettings,
    onProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onClose: () -> Unit,
    onNext: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
) {
    // Without 64-bit VLC this throws; close the window instead of crashing the whole app.
    val created = remember(video.id) { runCatching { EmbeddedMediaPlayerComponent() }.getOrNull() }
    if (created == null) {
        LaunchedEffect(video.id) { onClose() }
        return
    }
    val component = created
    val player = component.mediaPlayer()
    val windowState = rememberWindowState(size = DpSize(WINDOW_WIDTH, WINDOW_HEIGHT))
    var position by remember(video.id) { mutableLongStateOf(startMs) }
    var length by remember(video.id) { mutableLongStateOf(0L) }
    var playing by remember(video.id) { mutableStateOf(true) }
    var volume by remember(video.id) { mutableFloatStateOf(settings.volumePercent.toFloat()) }
    var dragging by remember(video.id) { mutableStateOf<Float?>(null) }
    var panel by remember(video.id) { mutableStateOf<TrackPanel?>(null) }
    var tracks by remember(video.id) { mutableStateOf<Pair<List<TrackChoice>, Int>>(emptyList<TrackChoice>() to -1) }
    val fullscreen = windowState.placement == WindowPlacement.Fullscreen

    LaunchedEffect(component) {
        // VLC needs a visible surface when playback starts, or it opens a window of its own.
        while (!component.isShowing) delay(SURFACE_POLL_MS)
        if (startMs > 0) {
            player.media().play(video.path, ":start-time=${startMs / MS_PER_SECOND}")
        } else {
            player.media().play(video.path)
        }
        player.audio().setVolume(settings.volumePercent)
    }

    DisposableEffect(component) {
        onDispose {
            val lastPosition = player.status().time()
            val lastLength = player.status().length()
            if (lastLength > 0) onProgress(lastPosition, lastLength)
            component.release()
        }
    }

    LaunchedEffect(component) {
        var polls = 0
        var finished = false
        while (true) {
            delay(POLL_MS)
            position = player.status().time().coerceAtLeast(0)
            length = player.status().length().coerceAtLeast(0)
            playing = player.status().isPlaying
            if (!finished && player.status().state() == State.ENDED) {
                finished = true
                onNext?.invoke()
            }
            if (++polls >= SAVE_EVERY_POLLS && length > 0) {
                polls = 0
                onProgress(position, length)
            }
        }
    }

    fun togglePlay() {
        val status = player.status()
        when {
            status.isPlaying -> player.controls().setPause(true)
            status.state() == State.PAUSED -> player.controls().setPause(false)
            else -> player.controls().play() // ended or stopped: start again
        }
    }

    fun skip(ms: Long) = player.controls().skipTime(ms)

    fun toggleFullscreen() {
        windowState.placement = if (fullscreen) WindowPlacement.Floating else WindowPlacement.Fullscreen
    }

    fun showTracks(which: TrackPanel) {
        panel = if (panel == which) null else which
        tracks = when (which) {
            TrackPanel.Subtitles ->
                player.subpictures().trackDescriptions().map { TrackChoice(it.id(), it.description()) } to
                    player.subpictures().track()
            TrackPanel.Audio ->
                player.audio().trackDescriptions().map { TrackChoice(it.id(), it.description()) } to
                    player.audio().track()
        }
    }

    Window(
        onCloseRequest = onClose,
        title = video.title,
        state = windowState,
        onPreviewKeyEvent = { event ->
            if (event.type != KeyEventType.KeyDown) {
                false
            } else {
                when (event.key) {
                    Key.Spacebar -> togglePlay().let { true }
                    Key.DirectionRight -> skip(SKIP_MS).let { true }
                    Key.DirectionLeft -> skip(-SKIP_MS).let { true }
                    Key.F -> toggleFullscreen().let { true }
                    Key.Escape -> if (fullscreen) toggleFullscreen().let { true } else false
                    else -> false
                }
            }
        },
    ) {
        KingPlayDesktopTheme(settings) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
                        SwingPanel(factory = { component }, modifier = Modifier.fillMaxSize())
                    }
                    panel?.let { which ->
                        TrackStrip(
                            title = if (which == TrackPanel.Subtitles) "Subtitles" else "Audio track",
                            choices = tracks.first,
                            selectedId = tracks.second,
                            onPick = { id ->
                                if (which == TrackPanel.Subtitles) {
                                    player.subpictures().setTrack(id)
                                } else {
                                    player.audio().setTrack(id)
                                }
                                tracks = tracks.first to id
                            },
                            onAddFile = if (which == TrackPanel.Subtitles) {
                                {
                                    chooseSubtitleFile()?.let { file ->
                                        player.subpictures().setSubTitleFile(file.absolutePath)
                                    }
                                }
                            } else {
                                null
                            },
                        )
                    }
                    Controls(
                        playing = playing,
                        position = dragging?.let { (it * length).toLong() } ?: position,
                        length = length,
                        seekFraction = dragging ?: if (length > 0) position.toFloat() / length else 0f,
                        volume = volume,
                        fullscreen = fullscreen,
                        onToggle = ::togglePlay,
                        onSkip = ::skip,
                        onSeekDrag = { dragging = it },
                        onSeekDone = {
                            dragging?.let { player.controls().setTime((it * length).toLong()) }
                            dragging = null
                        },
                        onVolume = { volume = it; player.audio().setVolume(it.toInt()) },
                        onSubtitles = { showTracks(TrackPanel.Subtitles) },
                        onAudio = { showTracks(TrackPanel.Audio) },
                        onFullscreen = ::toggleFullscreen,
                        onClose = onClose,
                        onNext = onNext,
                        onPrevious = onPrevious,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackStrip(
    title: String,
    choices: List<TrackChoice>,
    selectedId: Int,
    onPick: (Int) -> Unit,
    onAddFile: (() -> Unit)?,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            if (choices.isEmpty()) Text("None found", style = MaterialTheme.typography.bodySmall)
            choices.forEach { choice ->
                FilterChip(
                    selected = choice.id == selectedId,
                    onClick = { onPick(choice.id) },
                    label = { Text(choice.label.ifBlank { "Track ${choice.id}" }) },
                )
            }
            if (onAddFile != null) TextButton(onClick = onAddFile) { Text("Add subtitle file…") }
        }
    }
}

@Composable
private fun Controls(
    playing: Boolean,
    position: Long,
    length: Long,
    seekFraction: Float,
    volume: Float,
    fullscreen: Boolean,
    onToggle: () -> Unit,
    onSkip: (Long) -> Unit,
    onSeekDrag: (Float) -> Unit,
    onSeekDone: () -> Unit,
    onVolume: (Float) -> Unit,
    onSubtitles: () -> Unit,
    onAudio: () -> Unit,
    onFullscreen: () -> Unit,
    onClose: () -> Unit,
    onNext: (() -> Unit)?,
    onPrevious: (() -> Unit)?,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(position), style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = seekFraction.coerceIn(0f, 1f),
                    onValueChange = onSeekDrag,
                    onValueChangeFinished = onSeekDone,
                    enabled = length > 0,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .semantics { contentDescription = "Seek" },
                )
                Text(formatTime(length), style = MaterialTheme.typography.labelMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onPrevious != null) {
                    IconButton(onClick = onPrevious) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous video")
                    }
                }
                IconButton(onClick = { onSkip(-SKIP_MS) }) {
                    Icon(Icons.Filled.Replay10, contentDescription = "Back 10 seconds")
                }
                IconButton(onClick = onToggle, colors = IconButtonDefaults.filledIconButtonColors()) {
                    Icon(
                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                    )
                }
                IconButton(onClick = { onSkip(SKIP_MS) }) {
                    Icon(Icons.Filled.Forward10, contentDescription = "Forward 10 seconds")
                }
                if (onNext != null) {
                    IconButton(onClick = onNext) { Icon(Icons.Filled.SkipNext, contentDescription = "Next video") }
                }
                TextButton(onClick = onSubtitles) { Text("Subtitles") }
                TextButton(onClick = onAudio) { Text("Audio") }
                Box(modifier = Modifier.weight(1f))
                Icon(Icons.Filled.VolumeUp, contentDescription = null)
                Slider(
                    value = volume,
                    onValueChange = onVolume,
                    valueRange = 0f..MAX_VOLUME,
                    modifier = Modifier.width(VOLUME_WIDTH).semantics { contentDescription = "Volume" },
                )
                IconButton(onClick = onFullscreen) {
                    Icon(
                        imageVector = if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = if (fullscreen) "Leave full screen" else "Full screen",
                    )
                }
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close video") }
            }
        }
    }
}

private val WINDOW_WIDTH = 960.dp
private val WINDOW_HEIGHT = 600.dp
private val VOLUME_WIDTH = 120.dp
private const val MAX_VOLUME = 100f
private const val MS_PER_SECOND = 1000.0
