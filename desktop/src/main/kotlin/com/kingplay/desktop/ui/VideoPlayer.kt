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
import androidx.compose.material.icons.filled.VolumeOff
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
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
import java.io.File
import com.kingplay.desktop.video.AbLoop
import com.kingplay.desktop.video.AspectRatios
import com.kingplay.desktop.video.CropShapes
import com.kingplay.desktop.video.MAX_VOLUME_PERCENT
import com.kingplay.desktop.video.PlaybackSpeeds
import com.kingplay.desktop.video.VideoCommand
import com.kingplay.desktop.video.VideoFile
import com.kingplay.desktop.video.clampVolume
import com.kingplay.desktop.video.commandForKey
import com.kingplay.desktop.video.formatDelay
import com.kingplay.desktop.video.formatRate
import com.kingplay.desktop.video.nextInCycle
import com.kingplay.desktop.video.steppedSpeed
import kotlinx.coroutines.delay
import uk.co.caprica.vlcj.player.base.State
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

private enum class TrackPanel { Subtitles, Audio, Picture, Speed, Sync, Tools }

private const val SKIP_MS = 10_000L
private const val POLL_MS = 500L
private const val SURFACE_POLL_MS = 50L
private const val SAVE_EVERY_POLLS = 10
private const val ACTIVITY_POLL_MS = 150L
private const val HIDE_AFTER_MS = 2_000L
private const val VOLUME_WHEEL = 5
private const val AB_POLL_MS = 100L
private const val OSD_MS = 1_800L
private const val MICROS_PER_MS = 1_000L
private const val SNAPSHOT_NAME_MAX = 60
private const val MS_PER_SECOND_INT = 1_000L
private const val SPEED_EPSILON = 0.01f

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
    onStarted: () -> Unit = {},
    onWatched: (ms: Long) -> Unit = {},
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
    var muted by remember(video.id) { mutableStateOf(false) }
    var rate by remember(video.id) { mutableFloatStateOf(1f) }
    var aspect by remember(video.id) { mutableIntStateOf(0) }
    var crop by remember(video.id) { mutableIntStateOf(0) }
    var subDelayMs by remember(video.id) { mutableIntStateOf(0) }
    var audioDelayMs by remember(video.id) { mutableIntStateOf(0) }
    var looping by remember(video.id) { mutableStateOf(false) }
    var abLoop by remember(video.id) { mutableStateOf(AbLoop()) }
    var osd by remember(video.id) { mutableStateOf<String?>(null) }
    val fullscreen = windowState.placement == WindowPlacement.Fullscreen
    // In full screen the controls fade away after a short idle time and come back on any mouse or key activity.
    var controlsVisible by remember(video.id) { mutableStateOf(true) }
    var controlsHeightDp by remember(video.id) { mutableFloatStateOf(0f) }
    val wake = remember(video.id) { java.util.concurrent.atomic.AtomicBoolean(false) }

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

    // What a command does lives further down; the AWT listeners below call it through this holder.
    val runner = remember(video.id) { arrayOf<(VideoCommand) -> Unit>({}) }

    DisposableEffect(component) {
        // Key presses and clicks on the native video surface never reach Compose, so listen on it directly.
        val surface = component.videoSurfaceComponent()
        val mouse = object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent) {
                wake.set(true)
                surface.requestFocusInWindow()
            }
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return
                // One click plays or pauses; a quick second click goes full screen (the two pauses cancel out).
                runner[0](if (e.clickCount == 2) VideoCommand.ToggleFullscreen else VideoCommand.TogglePlay)
            }
            override fun mouseMoved(e: java.awt.event.MouseEvent) = wake.set(true)
            override fun mouseWheelMoved(e: java.awt.event.MouseWheelEvent) {
                wake.set(true)
                runner[0](VideoCommand.VolumeBy(if (e.wheelRotation < 0) VOLUME_WHEEL else -VOLUME_WHEEL))
            }
        }
        surface.addMouseListener(mouse)
        surface.addMouseMotionListener(mouse)
        surface.addMouseWheelListener(mouse)
        onDispose {
            surface.removeMouseListener(mouse)
            surface.removeMouseMotionListener(mouse)
            surface.removeMouseWheelListener(mouse)
            val lastPosition = player.status().time()
            val lastLength = player.status().length()
            if (lastLength > 0) onProgress(lastPosition, lastLength)
            component.release()
        }
    }

    val nextNow by rememberUpdatedState(onNext)
    val progressNow by rememberUpdatedState(onProgress)
    val startedNow by rememberUpdatedState(onStarted)
    val watchedNow by rememberUpdatedState(onWatched)
    LaunchedEffect(component) {
        var polls = 0
        var finished = false
        var counted = false
        while (true) {
            delay(POLL_MS)
            position = player.status().time().coerceAtLeast(0)
            length = player.status().length().coerceAtLeast(0)
            playing = player.status().isPlaying
            if (playing) {
                watchedNow(POLL_MS)
                if (!counted) {
                    counted = true
                    startedNow()
                }
            }
            if (!finished && player.status().state() == State.ENDED) {
                finished = true
                nextNow?.invoke()
            }
            if (++polls >= SAVE_EVERY_POLLS && length > 0) {
                polls = 0
                progressNow(position, length)
            }
        }
    }

    LaunchedEffect(abLoop) {
        while (abLoop.active) {
            delay(AB_POLL_MS)
            abLoop.jumpFor(player.status().time())?.let { player.controls().setTime(it) }
        }
    }
    LaunchedEffect(osd) {
        if (osd != null) {
            delay(OSD_MS)
            osd = null
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
        val now = windowState.placement == WindowPlacement.Fullscreen
        windowState.placement = if (now) WindowPlacement.Floating else WindowPlacement.Fullscreen
    }

    fun setVolume(percent: Int) {
        val clamped = clampVolume(percent)
        volume = clamped.toFloat()
        player.audio().setVolume(clamped)
    }

    fun setRate(value: Float) {
        rate = value
        player.controls().setRate(value)
    }

    fun setAspect(index: Int) {
        aspect = index
        player.video().setAspectRatio(AspectRatios[index].vlc)
    }

    fun setCrop(index: Int) {
        crop = index
        player.video().setCropGeometry(CropShapes[index].vlc)
    }

    fun setSubDelay(ms: Int) {
        subDelayMs = ms
        player.subpictures().setDelay(ms * MICROS_PER_MS)
    }

    fun setAudioDelay(ms: Int) {
        audioDelayMs = ms
        player.audio().setDelay(ms * MICROS_PER_MS)
    }

    fun setMuted(value: Boolean) {
        muted = value
        player.audio().setMute(value)
    }

    fun setLooping(value: Boolean) {
        looping = value
        player.controls().setRepeat(value)
    }

    fun snapshot() {
        val folder = File(System.getProperty("user.home"), "Pictures/KingPlay").apply { mkdirs() }
        val name = video.title.replace(Regex("[^A-Za-z0-9 ._-]"), "_").take(SNAPSHOT_NAME_MAX)
        val file = File(folder, "$name ${formatTime(player.status().time()).replace(':', '-')}.png")
        osd = if (player.snapshots().save(file)) "Saved screenshot to Pictures/KingPlay" else "Could not save a screenshot"
    }

    fun cycleTrack(subtitles: Boolean) {
        val ids = if (subtitles) {
            player.subpictures().trackDescriptions().map { it.id() }
        } else {
            player.audio().trackDescriptions().map { it.id() }
        }
        val next = nextInCycle(ids, if (subtitles) player.subpictures().track() else player.audio().track()) ?: return
        if (subtitles) player.subpictures().setTrack(next) else player.audio().setTrack(next)
        val names = if (subtitles) player.subpictures().trackDescriptions() else player.audio().trackDescriptions()
        val label = names.firstOrNull { it.id() == next }?.description().orEmpty().ifBlank { "Off" }
        osd = (if (subtitles) "Subtitles: " else "Audio: ") + label
    }

    fun execute(command: VideoCommand) {
        when (command) {
            VideoCommand.TogglePlay -> togglePlay()
            is VideoCommand.Seek -> {
                skip(command.ms)
                osd = if (command.ms > 0) "Forward ${command.ms / MS_PER_SECOND_INT} s" else "Back ${-command.ms / MS_PER_SECOND_INT} s"
            }
            is VideoCommand.VolumeBy -> {
                setVolume(volume.toInt() + command.delta)
                osd = "Volume ${volume.toInt()}%"
            }
            VideoCommand.ToggleMute -> {
                setMuted(!muted)
                osd = if (muted) "Muted" else "Sound on"
            }
            VideoCommand.ToggleFullscreen -> toggleFullscreen()
            VideoCommand.ExitFullscreen -> if (windowState.placement == WindowPlacement.Fullscreen) toggleFullscreen()
            VideoCommand.Next -> onNext?.invoke()
            VideoCommand.Previous -> onPrevious?.invoke()
            VideoCommand.SpeedUp, VideoCommand.SpeedDown -> {
                setRate(steppedSpeed(rate, command == VideoCommand.SpeedUp))
                osd = "Speed ${formatRate(rate)}"
            }
            VideoCommand.SpeedNormal -> {
                setRate(1f)
                osd = "Speed 1x"
            }
            VideoCommand.CycleAspect -> {
                setAspect((aspect + 1) % AspectRatios.size)
                osd = "Aspect ratio: ${AspectRatios[aspect].label}"
            }
            VideoCommand.CycleCrop -> {
                setCrop((crop + 1) % CropShapes.size)
                osd = "Crop: ${CropShapes[crop].label}"
            }
            VideoCommand.CycleSubtitles -> cycleTrack(subtitles = true)
            VideoCommand.CycleAudio -> cycleTrack(subtitles = false)
            is VideoCommand.SubtitleDelay -> {
                setSubDelay(subDelayMs + command.deltaMs)
                osd = "Subtitle delay ${formatDelay(subDelayMs)}"
            }
            is VideoCommand.AudioDelay -> {
                setAudioDelay(audioDelayMs + command.deltaMs)
                osd = "Audio delay ${formatDelay(audioDelayMs)}"
            }
            VideoCommand.NextFrame -> {
                player.controls().setPause(true)
                player.controls().nextFrame()
            }
            VideoCommand.Snapshot -> snapshot()
            VideoCommand.ToggleLoop -> {
                setLooping(!looping)
                osd = if (looping) "Repeat on" else "Repeat off"
            }
            VideoCommand.Restart -> player.controls().setTime(0L)
        }
    }
    runner[0] = ::execute

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
        icon = trayIconPainter(),
        state = windowState,
        onPreviewKeyEvent = { _ ->
            wake.set(true)
            false
        },
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        DisposableEffect(window) {
            // Catch shortcuts wherever the keyboard focus is, including on the native video surface.
            val manager = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
            val dispatcher = java.awt.KeyEventDispatcher { e ->
                if (e.id != java.awt.event.KeyEvent.KEY_PRESSED || manager.activeWindow !== window) {
                    false
                } else {
                    wake.set(true)
                    val command = commandForKey(e.keyCode, e.isShiftDown, e.isControlDown)
                    if (command == null) {
                        false
                    } else {
                        runner[0](command)
                        true
                    }
                }
            }
            manager.addKeyEventDispatcher(dispatcher)
            onDispose { manager.removeKeyEventDispatcher(dispatcher) }
        }
        LaunchedEffect(fullscreen, component) {
            val blank = java.awt.Toolkit.getDefaultToolkit().createCustomCursor(
                java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB),
                java.awt.Point(0, 0),
                "hidden",
            )
            fun cursorHidden(hidden: Boolean) {
                val cursor = if (hidden) blank else java.awt.Cursor.getDefaultCursor()
                runCatching {
                    component.cursor = cursor
                    component.videoSurfaceComponent().cursor = cursor
                    window.cursor = cursor
                }
            }
            controlsVisible = true
            cursorHidden(false)
            var last = runCatching { java.awt.MouseInfo.getPointerInfo()?.location }.getOrNull()
            var idleSince = System.currentTimeMillis()
            try {
                while (fullscreen) {
                    delay(ACTIVITY_POLL_MS)
                    val now = System.currentTimeMillis()
                    val pointer = runCatching { java.awt.MouseInfo.getPointerInfo()?.location }.getOrNull()
                    val moved = pointer != null && last != null && (pointer.x != last.x || pointer.y != last.y)
                    last = pointer
                    val overControls = pointer != null && controlsVisible &&
                        pointer.y >= window.y + window.height - controlsHeightDp
                    if (moved || overControls || wake.getAndSet(false)) {
                        idleSince = now
                        if (!controlsVisible) {
                            controlsVisible = true
                            cursorHidden(false)
                        }
                    } else if (controlsVisible && now - idleSince >= HIDE_AFTER_MS) {
                        controlsVisible = false
                        cursorHidden(true)
                    }
                }
            } finally {
                controlsVisible = true
                cursorHidden(false)
            }
        }
        KingPlayDesktopTheme(settings) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
                        SwingPanel(factory = { component }, modifier = Modifier.fillMaxSize())
                    }
                    if (!fullscreen || controlsVisible) Column(
                        modifier = Modifier.onSizeChanged { controlsHeightDp = it.height / density },
                    ) {
                    osd?.let { text ->
                        Surface(color = MaterialTheme.colorScheme.inverseSurface, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                            )
                        }
                    }
                    panel?.takeIf { it == TrackPanel.Subtitles || it == TrackPanel.Audio }?.let { which ->
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
                    when (panel) {
                        TrackPanel.Picture -> ChoiceStrip("Aspect ratio", AspectRatios.map { it.label }, aspect, ::setAspect) {
                            ChoiceRow("Crop", CropShapes.map { it.label }, crop, ::setCrop)
                        }
                        TrackPanel.Speed -> ChoiceStrip(
                            "Speed",
                            PlaybackSpeeds.map { formatRate(it) },
                            PlaybackSpeeds.indexOfFirst { kotlin.math.abs(it - rate) < SPEED_EPSILON },
                            { setRate(PlaybackSpeeds[it]) },
                        )
                        TrackPanel.Sync -> SyncStrip(
                            subDelayMs = subDelayMs,
                            audioDelayMs = audioDelayMs,
                            onSubtitles = ::setSubDelay,
                            onAudio = ::setAudioDelay,
                        )
                        TrackPanel.Tools -> ToolsStrip(
                            looping = looping,
                            abLoop = abLoop,
                            onLoop = { setLooping(it) },
                            onMarkAb = { abLoop = abLoop.mark(player.status().time()) },
                            onClearAb = { abLoop = AbLoop() },
                            onSnapshot = ::snapshot,
                            onFrame = { execute(VideoCommand.NextFrame) },
                        )
                        else -> Unit
                    }
                    Controls(
                        muted = muted,
                        onMute = { execute(VideoCommand.ToggleMute) },
                        onPicture = { panel = if (panel == TrackPanel.Picture) null else TrackPanel.Picture },
                        onSpeedPanel = { panel = if (panel == TrackPanel.Speed) null else TrackPanel.Speed },
                        onSync = { panel = if (panel == TrackPanel.Sync) null else TrackPanel.Sync },
                        onTools = { panel = if (panel == TrackPanel.Tools) null else TrackPanel.Tools },
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
                        onVolume = { setVolume(it.toInt()) },
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
private fun ChoiceRow(title: String, labels: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(CHOICE_TITLE_WIDTH))
        labels.forEachIndexed { index, label ->
            FilterChip(selected = index == selected, onClick = { onPick(index) }, label = { Text(label) })
        }
    }
}

@Composable
private fun ChoiceStrip(
    title: String,
    labels: List<String>,
    selected: Int,
    onPick: (Int) -> Unit,
    extra: @Composable () -> Unit = {},
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Column {
            ChoiceRow(title, labels, selected, onPick)
            extra()
        }
    }
}

@Composable
private fun DelayRow(title: String, delayMs: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(CHOICE_TITLE_WIDTH))
        TextButton(onClick = { onChange(delayMs - com.kingplay.desktop.video.DELAY_STEP_MS) }) { Text("Earlier  −0.05 s") }
        Text(formatDelay(delayMs), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { onChange(delayMs + com.kingplay.desktop.video.DELAY_STEP_MS) }) { Text("Later  +0.05 s") }
        TextButton(onClick = { onChange(0) }, enabled = delayMs != 0) { Text("Reset") }
    }
}

@Composable
private fun SyncStrip(
    subDelayMs: Int,
    audioDelayMs: Int,
    onSubtitles: (Int) -> Unit,
    onAudio: (Int) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Column {
            DelayRow("Subtitles (H / J)", subDelayMs, onSubtitles)
            DelayRow("Audio (G / L)", audioDelayMs, onAudio)
        }
    }
}

@Composable
private fun ToolsStrip(
    looping: Boolean,
    abLoop: AbLoop,
    onLoop: (Boolean) -> Unit,
    onMarkAb: () -> Unit,
    onClearAb: () -> Unit,
    onSnapshot: () -> Unit,
    onFrame: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(selected = looping, onClick = { onLoop(!looping) }, label = { Text("Repeat this video (R)") })
            TextButton(onClick = onMarkAb) {
                Text(
                    when {
                        abLoop.active -> "A–B ${formatTime(abLoop.start ?: 0L)} to ${formatTime(abLoop.end ?: 0L)}"
                        abLoop.start != null -> "Mark end of A–B (A at ${formatTime(abLoop.start)})"
                        else -> "Mark start of A–B"
                    },
                )
            }
            TextButton(onClick = onClearAb, enabled = abLoop.start != null) { Text("Clear A–B") }
            TextButton(onClick = onFrame) { Text("Next frame (E)") }
            TextButton(onClick = onSnapshot) { Text("Screenshot (S)") }
        }
    }
}

private val CHOICE_TITLE_WIDTH = 140.dp

@Composable
private fun Controls(
    muted: Boolean,
    onMute: () -> Unit,
    onPicture: () -> Unit,
    onSpeedPanel: () -> Unit,
    onSync: () -> Unit,
    onTools: () -> Unit,
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
                Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                    TextButton(onClick = onSubtitles) { Text("Subtitles") }
                    TextButton(onClick = onAudio) { Text("Audio") }
                    TextButton(onClick = onPicture) { Text("Picture") }
                    TextButton(onClick = onSpeedPanel) { Text("Speed") }
                    TextButton(onClick = onSync) { Text("Sync") }
                    TextButton(onClick = onTools) { Text("Tools") }
                }
                IconButton(onClick = onMute) {
                    Icon(
                        imageVector = if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = if (muted) "Unmute" else "Mute",
                    )
                }
                Slider(
                    value = volume,
                    onValueChange = onVolume,
                    valueRange = 0f..MAX_VOLUME_PERCENT.toFloat(),
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
private const val MS_PER_SECOND = 1000.0
