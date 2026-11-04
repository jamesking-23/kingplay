package com.kingplay.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kingplay.core.model.Track
import com.kingplay.core.model.repository.PlaybackState

/**
 * The full "now playing" view, opened by clicking the empty space on the play bar: a big spinning disc with the
 * song's cover, its name, artist and album, and shortcuts. The play bar underneath keeps the controls.
 */
@Composable
fun NowPlayingPage(
    track: Track?,
    playback: PlaybackState,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close the now playing view")
            }
            Text("Now playing", style = MaterialTheme.typography.titleMedium)
        }
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp)) {
            val wide = maxWidth > maxHeight * WIDE_RATIO
            val disc: Dp = if (wide) minOf(maxHeight, maxWidth * DISC_SHARE) else minOf(maxWidth, maxHeight * DISC_SHARE)
            val details: @Composable (Modifier) -> Unit = { m ->
                Details(track, playback, favorite, onToggleFavorite, onLyrics, onQueue, wide, m)
            }
            val art: @Composable () -> Unit = {
                VinylDisc(spinning = playback.isPlaying, modifier = Modifier.size(disc)) {
                    CoverImage(track, disc, shape = CircleShape, modifier = Modifier.fillMaxSize())
                }
            }
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    art()
                    details(Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                ) {
                    art()
                    details(Modifier)
                }
            }
        }
    }
}

@Composable
private fun Details(
    track: Track?,
    playback: PlaybackState,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    wide: Boolean,
    modifier: Modifier,
) {
    val align = if (wide) TextAlign.Start else TextAlign.Center
    Column(
        modifier = modifier,
        horizontalAlignment = if (wide) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            playback.title ?: "Nothing playing",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = align,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        playback.artist?.let {
            Text(it, style = MaterialTheme.typography.titleMedium, textAlign = align, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        playback.album?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = align,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        track?.let { t ->
            val facts = listOfNotNull(
                t.year?.toString(),
                t.format.takeIf { it.isNotBlank() },
                t.bitrateKbps.takeIf { it > 0 }?.let { "$it kbps" },
                t.sampleRateHz.takeIf { it > 0 }?.let { "${it / KHZ_DIV} kHz" },
            )
            if (facts.isNotEmpty()) {
                Text(
                    facts.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (track != null) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
                        tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedButton(onClick = onLyrics) { Text("Lyrics") }
            OutlinedButton(onClick = onQueue) { Text("Queue") }
        }
    }
}

private const val WIDE_RATIO = 1.3f
private const val DISC_SHARE = 0.9f
private const val KHZ_DIV = 1000.0
