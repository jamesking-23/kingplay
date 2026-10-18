package com.kingplay.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingplay.core.model.RepeatMode
import com.kingplay.core.model.repository.PlaybackState
import com.kingplay.desktop.playback.DesktopPlaybackController

@Composable
fun NowPlayingBar(controller: DesktopPlaybackController, state: PlaybackState, modifier: Modifier = Modifier) {
    var volume by remember { mutableFloatStateOf(DEFAULT_VOLUME) }
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(1)
    val shownPosition = dragging ?: (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime((shownPosition * duration).toLong()), style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = shownPosition,
                    onValueChange = { dragging = it },
                    onValueChangeFinished = {
                        dragging?.let { controller.seekTo((it * duration).toLong()) }
                        dragging = null
                    },
                    enabled = state.hasMedia,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .semantics { contentDescription = "Seek" },
                )
                Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title ?: "Nothing playing",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.error ?: listOfNotNull(state.artist, state.album).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.error != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToggleIcon(Icons.Filled.Shuffle, "Shuffle", state.shuffleEnabled) {
                        controller.setShuffleEnabled(!state.shuffleEnabled)
                    }
                    IconButton(onClick = controller::skipToPrevious, enabled = state.hasMedia) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous song")
                    }
                    IconButton(
                        onClick = controller::togglePlayPause,
                        enabled = state.hasMedia,
                        colors = IconButtonDefaults.filledIconButtonColors(),
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                        )
                    }
                    IconButton(onClick = controller::skipToNext, enabled = state.hasMedia) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next song")
                    }
                    ToggleIcon(
                        icon = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        description = when (state.repeatMode) {
                            RepeatMode.OFF -> "Repeat off"
                            RepeatMode.ALL -> "Repeat all"
                            RepeatMode.ONE -> "Repeat one"
                        },
                        on = state.repeatMode != RepeatMode.OFF,
                    ) { controller.setRepeatMode(nextRepeatMode(state.repeatMode)) }
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null)
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            controller.setVolume((it * PERCENT).toInt())
                        },
                        modifier = Modifier
                            .width(120.dp)
                            .padding(start = 8.dp)
                            .semantics { contentDescription = "Volume" },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleIcon(icon: ImageVector, description: String, on: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val DEFAULT_VOLUME = 0.8f
private const val PERCENT = 100
