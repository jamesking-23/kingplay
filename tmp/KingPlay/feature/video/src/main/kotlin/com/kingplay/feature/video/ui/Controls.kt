package com.kingplay.feature.video.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingplay.core.designsystem.component.KpIconButton
import com.kingplay.core.designsystem.component.rememberHapticClick
import com.kingplay.core.designsystem.theme.KpIcons
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.ui.format.formatDuration
import com.kingplay.feature.video.R
import com.kingplay.feature.video.logic.VideoResize
import com.kingplay.feature.video.logic.formatSpeed

private val ScrimColor = Color.Black.copy(alpha = 0.55f)
private val PlayButtonSize = 72.dp
private val PlayIconSize = 40.dp

/** Everything the controls can ask for. The screen decides what each one does. */
class ControlActions(
    val onBack: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onSeekTo: (Long) -> Unit,
    val onSpeed: () -> Unit,
    val onResize: () -> Unit,
    val onAudio: () -> Unit,
    val onSubtitles: () -> Unit,
    val onRotate: () -> Unit,
    val onLock: () -> Unit,
    val onPip: (() -> Unit)?,
    val onInteract: () -> Unit,
)

/** The on-screen controls: title and options on top, transport in the middle, seek bar and speed at the bottom. */
@Composable
fun VideoControls(
    state: VideoPlayerState,
    resize: VideoResize,
    actions: ControlActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(ScrimColor).safeDrawingPadding()) {
        TopRow(state, actions, Modifier.align(Alignment.TopCenter))
        TransportRow(state, actions, Modifier.align(Alignment.Center))
        BottomRow(state, resize, actions, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun TopRow(state: VideoPlayerState, actions: ControlActions, modifier: Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KpIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.video_back),
            onClick = actions.onBack,
        )
        Text(
            text = state.title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        KpIconButton(
            icon = KpIcons.Subtitles,
            contentDescription = stringResource(R.string.video_subtitles),
            onClick = actions.onSubtitles,
        )
        KpIconButton(
            icon = KpIcons.AudioTrack,
            contentDescription = stringResource(R.string.video_audio_tracks),
            onClick = actions.onAudio,
        )
        actions.onPip?.let { pip ->
            KpIconButton(
                icon = KpIcons.PictureInPicture,
                contentDescription = stringResource(R.string.video_pip),
                onClick = pip,
            )
        }
        KpIconButton(
            icon = KpIcons.ScreenRotation,
            contentDescription = stringResource(R.string.video_rotate),
            onClick = actions.onRotate,
        )
        KpIconButton(
            icon = KpIcons.Lock,
            contentDescription = stringResource(R.string.video_lock),
            onClick = actions.onLock,
        )
    }
}

@Composable
private fun TransportRow(state: VideoPlayerState, actions: ControlActions, modifier: Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.xl),
    ) {
        KpIconButton(
            icon = KpIcons.SkipPrevious,
            contentDescription = stringResource(R.string.video_previous),
            onClick = actions.onPrevious,
            enabled = state.hasPrevious,
        )
        FilledIconButton(
            onClick = rememberHapticClick(actions.onPlayPause),
            modifier = Modifier.size(PlayButtonSize),
        ) {
            Icon(
                imageVector = if (state.isPlaying) KpIcons.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(if (state.isPlaying) R.string.video_pause else R.string.video_play),
                modifier = Modifier.size(PlayIconSize),
            )
        }
        KpIconButton(
            icon = KpIcons.SkipNext,
            contentDescription = stringResource(R.string.video_next),
            onClick = actions.onNext,
            enabled = state.hasNext,
        )
    }
}

@Composable
private fun BottomRow(state: VideoPlayerState, resize: VideoResize, actions: ControlActions, modifier: Modifier) {
    var scrub by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs
    val fraction = if (duration > 0) (state.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val shownMs = scrub?.let { (it * duration).toLong() } ?: state.positionMs
    val spoken = stringResource(R.string.video_position, formatDuration(shownMs), formatDuration(duration))
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = KpTheme.spacing.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatDuration(shownMs), style = MaterialTheme.typography.labelMedium, color = Color.White)
            Slider(
                value = scrub ?: fraction,
                onValueChange = {
                    scrub = it
                    actions.onInteract()
                },
                onValueChangeFinished = {
                    scrub?.let { actions.onSeekTo((it * duration).toLong()) }
                    scrub = null
                },
                enabled = duration > 0,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = KpTheme.spacing.sm)
                    .semantics { stateDescription = spoken },
            )
            Text(formatDuration(duration), style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = rememberHapticClick(actions.onSpeed)) {
                Text(
                    text = formatSpeed(state.speed),
                    color = Color.White,
                )
            }
            KpIconButton(
                icon = KpIcons.AspectRatio,
                contentDescription = stringResource(
                    R.string.video_resize_button,
                    stringResource(resizeLabel(resize)),
                ),
                onClick = actions.onResize,
            )
        }
    }
}

fun resizeLabel(resize: VideoResize): Int = when (resize) {
    VideoResize.FIT -> R.string.video_resize_fit
    VideoResize.ZOOM -> R.string.video_resize_zoom
    VideoResize.FILL -> R.string.video_resize_fill
}
