package com.kingplay.feature.video.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.kingplay.core.designsystem.component.KpEmptyState
import com.kingplay.core.designsystem.theme.KpIcons
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.navigation.AppNavigator
import com.kingplay.core.player.video.ExternalSubtitle
import com.kingplay.core.ui.format.formatDuration
import com.kingplay.feature.video.R
import com.kingplay.feature.video.VideoEvent
import com.kingplay.feature.video.VideoLoad
import com.kingplay.feature.video.VideoViewModel
import com.kingplay.feature.video.logic.CONTROLS_HIDE_MS
import com.kingplay.feature.video.logic.DEFAULT_SUBTITLE_SIZE_INDEX
import com.kingplay.feature.video.logic.SubtitleSizes
import com.kingplay.feature.video.logic.TapZone
import com.kingplay.feature.video.logic.VideoResize
import com.kingplay.feature.video.logic.doubleTapSeekMs
import com.kingplay.feature.video.logic.nextResize
import com.kingplay.feature.video.logic.prefersLandscape
import com.kingplay.feature.video.logic.subtitleMimeType
import com.kingplay.feature.video.logic.swipeLevel
import com.kingplay.feature.video.logic.swipeSeekTarget
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private const val HOLD_SPEED = 2f
private const val INDICATOR_MS = 900L
private const val PERCENT = 100

@Composable
fun VideoPlayerScreen(navigator: AppNavigator, viewModel: VideoViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    when (state.load) {
        VideoLoad.Loading -> Box(Modifier.fillMaxSize().background(Color.Black))
        VideoLoad.Missing -> Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            KpEmptyState(
                icon = KpIcons.Movie,
                title = stringResource(R.string.video_missing_title),
                message = stringResource(R.string.video_missing_message),
            )
        }
        is VideoLoad.Ready -> {
            val current = player
            if (current == null) {
                Box(Modifier.fillMaxSize().background(Color.Black))
            } else {
                VideoPlayerContent(
                    player = current,
                    onBack = navigator::back,
                    onSavePosition = { viewModel.onEvent(VideoEvent.SavePosition) },
                    onAddSubtitle = { viewModel.onEvent(VideoEvent.AddSubtitle(it)) },
                )
            }
        }
    }
}

private enum class VideoSheet { Speed, Audio, Subtitles }

@Composable
private fun VideoPlayerContent(
    player: Player,
    onBack: () -> Unit,
    onSavePosition: () -> Unit,
    onAddSubtitle: (ExternalSubtitle) -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val state = rememberVideoPlayerState(player) ?: return
    val inPip = rememberInPictureInPicture(activity)
    val brightness = rememberBrightnessControl(activity)
    val volume = remember(context) { VolumeControl(context) }
    val pipSupported = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var interactions by remember { mutableIntStateOf(0) }
    var locked by rememberSaveable { mutableStateOf(false) }
    var resize by rememberSaveable { mutableStateOf(VideoResize.FIT) }
    var sheet by remember { mutableStateOf<VideoSheet?>(null) }
    var subtitleSize by rememberSaveable { mutableIntStateOf(DEFAULT_SUBTITLE_SIZE_INDEX) }
    var indicator by remember { mutableStateOf<String?>(null) }
    var indicatorSticky by remember { mutableStateOf(false) }

    // Orientation: follow the picture until the user rotates by hand.
    var userRotated by rememberSaveable { mutableStateOf(false) }
    var orientation by rememberSaveable { mutableIntStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    LaunchedEffect(state.videoWidth, state.videoHeight) {
        if (!userRotated && state.videoWidth > 0) {
            orientation = if (prefersLandscape(state.videoWidth, state.videoHeight)) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    ImmersiveMode(activity)
    if (!inPip) ScreenOrientation(activity, orientation)
    AutoEnterPictureInPicture(activity, pipSupported && state.isPlaying, state.videoWidth, state.videoHeight)

    // Controls hide themselves a few seconds after the last touch while the video plays.
    LaunchedEffect(controlsVisible, interactions, state.isPlaying, sheet) {
        if (controlsVisible && state.isPlaying && sheet == null) {
            delay(CONTROLS_HIDE_MS)
            controlsVisible = false
        }
    }
    LaunchedEffect(indicator, indicatorSticky) {
        if (indicator != null && !indicatorSticky) {
            delay(INDICATOR_MS)
            indicator = null
        }
    }

    // Leaving the screen stores the position and stops playback; the picture-in-picture window keeps playing.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        onSavePosition()
        if (activity?.isInPictureInPictureMode != true) player.pause()
    }
    DisposableEffect(player) {
        onDispose {
            onSavePosition()
            player.pause()
        }
    }

    val skipBack = stringResource(R.string.video_skip_back)
    val skipForward = stringResource(R.string.video_skip_forward)
    val fastLabel = stringResource(R.string.video_fast)
    val unsupported = stringResource(R.string.video_subtitles_unsupported)

    fun flash(text: String) {
        indicator = text
        indicatorSticky = false
    }

    val pickSubtitle = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = displayName(context, uri)
        val mime = subtitleMimeType(name, context.contentResolver.getType(uri))
        if (mime == null) {
            flash(unsupported)
        } else {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            player.setSubtitlesEnabled(true)
            onAddSubtitle(ExternalSubtitle(uri.toString(), mime, name?.substringBeforeLast('.') ?: "Subtitles"))
        }
    }

    // Gesture bookkeeping.
    var seekStartMs by remember { mutableStateOf(0L) }
    var seekTargetMs by remember { mutableStateOf(0L) }
    var levelStart by remember { mutableFloatStateOf(0f) }
    var speedBeforeHold by remember { mutableFloatStateOf(1f) }

    val handlers = GestureHandlers(
        onTap = { controlsVisible = !controlsVisible },
        onDoubleTap = { zone ->
            if (!locked) {
                val delta = doubleTapSeekMs(zone)
                if (zone == TapZone.CENTER) {
                    if (player.isPlaying) player.pause() else player.play()
                } else {
                    val limit = state.durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE
                    player.seekTo((player.currentPosition + delta).coerceIn(0L, limit))
                    flash(if (delta < 0) skipBack else skipForward)
                }
            }
        },
        onHold = { held ->
            if (!locked) {
                if (held) {
                    speedBeforeHold = player.playbackParameters.speed
                    player.setPlaybackSpeed(HOLD_SPEED)
                    indicator = fastLabel
                    indicatorSticky = true
                } else {
                    player.setPlaybackSpeed(speedBeforeHold)
                    indicatorSticky = false
                }
            }
        },
        onSeekStart = {
            seekStartMs = player.currentPosition
            seekTargetMs = seekStartMs
            indicatorSticky = true
        },
        onSeekDrag = { dx, width ->
            if (!locked) {
                seekTargetMs = swipeSeekTarget(seekStartMs, dx, width, state.durationMs)
                indicator = "${formatDuration(seekTargetMs)} / ${formatDuration(state.durationMs)}"
            }
        },
        onSeekEnd = {
            if (!locked && indicator != null) player.seekTo(seekTargetMs)
            indicatorSticky = false
        },
        onLevelStart = { right ->
            levelStart = if (right) volume.current() else brightness.current()
            indicatorSticky = true
        },
        onLevelDrag = { dy, height, right ->
            if (!locked) {
                val level = swipeLevel(levelStart, dy, height)
                val percent = (level * PERCENT).roundToInt()
                if (right) {
                    volume.set(level)
                    indicator = context.getString(R.string.video_volume, percent)
                } else {
                    brightness.set(level)
                    indicator = context.getString(R.string.video_brightness, percent)
                }
            }
        },
        onLevelEnd = { indicatorSticky = false },
    )

    val actions = ControlActions(
        onBack = onBack,
        onPlayPause = { if (player.isPlaying) player.pause() else player.play() },
        onNext = { player.seekToNextMediaItem() },
        onPrevious = { player.seekToPreviousMediaItem() },
        onSeekTo = { player.seekTo(it) },
        onSpeed = { sheet = VideoSheet.Speed },
        onResize = { resize = nextResize(resize) },
        onAudio = { sheet = VideoSheet.Audio },
        onSubtitles = { sheet = VideoSheet.Subtitles },
        onRotate = {
            userRotated = true
            orientation = if (isLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        },
        onLock = {
            locked = true
            controlsVisible = false
        },
        onPip = if (pipSupported) {
            { enterPictureInPicture(activity, state.videoWidth, state.videoHeight) }
        } else {
            null
        },
        onInteract = { interactions++ },
    )

    val surfaceDescription = stringResource(R.string.video_surface)
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VideoSurface(player, resize, state.isPlaying, SubtitleSizes[subtitleSize])
        GestureLayer(
            handlers = handlers,
            modifier = Modifier.fillMaxSize().semantics { contentDescription = surfaceDescription },
        )

        if (state.isBuffering && !inPip) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
        }
        if (!inPip && !locked && controlsVisible) VideoControls(state, resize, actions)
        if (!inPip && locked && controlsVisible) {
            IconButton(
                onClick = { locked = false },
                modifier = Modifier.align(Alignment.CenterStart).safeDrawingPadding().padding(KpTheme.spacing.md),
            ) {
                Icon(KpIcons.Lock, contentDescription = stringResource(R.string.video_unlock), tint = Color.White)
            }
        }
        indicator?.takeIf { !inPip }?.let {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = KpTheme.spacing.xl),
                color = Color.Black.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(horizontal = KpTheme.spacing.lg, vertical = KpTheme.spacing.sm),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }
    }

    val fallback = stringResource(R.string.video_track_fallback)
    when (sheet) {
        VideoSheet.Speed -> SpeedSheet(
            current = state.speed,
            onPick = {
                player.setPlaybackSpeed(it)
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        VideoSheet.Audio -> AudioSheet(
            options = trackOptions(state.tracks, C.TRACK_TYPE_AUDIO, fallback),
            onPick = {
                player.select(it)
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        VideoSheet.Subtitles -> SubtitleSheet(
            options = trackOptions(state.tracks, C.TRACK_TYPE_TEXT, fallback),
            subtitlesOn = player.subtitlesEnabled(),
            sizeIndex = subtitleSize,
            onPick = {
                player.select(it)
                sheet = null
            },
            onOff = {
                player.setSubtitlesEnabled(false)
                sheet = null
            },
            onLoadFile = {
                sheet = null
                pickSubtitle.launch(arrayOf("*/*"))
            },
            onSize = { subtitleSize = it },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(player: Player, resize: VideoResize, keepOn: Boolean, subtitleFraction: Float) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            }
        },
        update = { view ->
            if (view.player !== player) view.player = player
            view.resizeMode = when (resize) {
                VideoResize.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                VideoResize.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                VideoResize.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
            view.keepScreenOn = keepOn
            view.subtitleView?.setFractionalTextSize(subtitleFraction)
        },
        onRelease = { view -> view.player = null },
    )
}

private fun displayName(context: Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment
