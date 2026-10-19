package com.kingplay.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.repository.PlaybackState
import com.kingplay.desktop.DesktopAppState
import com.kingplay.desktop.cover.CoverRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** A small always-on-top window: cover, title, progress and the three transport buttons. */
@Composable
fun MiniPlayer(app: DesktopAppState, covers: CoverRepository, onOpenMain: () -> Unit) {
    val settings by app.settings.collectAsState()
    val index by app.index.collectAsState()
    val idle = remember { MutableStateFlow(PlaybackState()) }
    val state by (app.controller?.state ?: idle).collectAsState()
    val controller = app.controller
    val track = (state.current as? MediaRef.Audio)?.id?.let(index::get)

    KingPlayDesktopTheme(settings) {
        CompositionLocalProvider(LocalCovers provides covers) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayingCover(track, MINI_COVER, state.isPlaying, modifier = Modifier.padding(end = 12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.title ?: "Nothing playing",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = state.artist.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = onOpenMain) {
                            Icon(Icons.Filled.OpenInFull, contentDescription = "Open the full window")
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progressOf(state.positionMs, state.durationMs) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { controller?.skipToPrevious() }, enabled = state.hasMedia) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous song")
                        }
                        IconButton(
                            onClick = { controller?.togglePlayPause() },
                            enabled = state.hasMedia,
                            colors = IconButtonDefaults.filledIconButtonColors(),
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                            )
                        }
                        IconButton(onClick = { controller?.skipToNext() }, enabled = state.hasMedia) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Next song")
                        }
                    }
                }
            }
        }
    }
}

private val MINI_COVER = 56.dp

private fun progressOf(positionMs: Long, durationMs: Long): Float =
    (positionMs.toFloat() / durationMs.coerceAtLeast(1)).coerceIn(0f, 1f)
