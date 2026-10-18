package com.kingplay.feature.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.component.KpEmptyState
import com.kingplay.core.designsystem.component.KpIconButton
import com.kingplay.core.designsystem.component.rememberHapticClick
import com.kingplay.core.designsystem.theme.KpIcons
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.RepeatMode
import com.kingplay.core.model.navigation.AppNavigator
import com.kingplay.core.ui.artwork.ArtworkImage
import com.kingplay.core.ui.artwork.ArtworkSize
import com.kingplay.core.ui.displayName
import com.kingplay.core.ui.format.formatDuration
import com.kingplay.core.ui.format.formatQuality
import com.kingplay.core.ui.format.progressFraction
import com.kingplay.feature.nowplaying.logic.seekTargetMs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

private val PlayButtonSize = 72.dp
private const val SEEK_SETTLE_MS = 1_000L
private const val SEEK_SETTLE_TOLERANCE_MS = 1_500L

@Composable
fun NowPlayingScreen(
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showQueue by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.statusBarsPadding().navigationBarsPadding()) {
            TopBar(onClose = navigator::back, onQueue = { showQueue = true })
            if (!state.playback.hasMedia) {
                KpEmptyState(
                    icon = KpIcons.MusicNote,
                    title = stringResource(R.string.np_nothing_title),
                    message = stringResource(R.string.np_nothing_message),
                )
            } else {
                PlayerBody(
                    state = state,
                    positionMs = viewModel.positionMs,
                    onEvent = viewModel::onEvent,
                )
            }
        }
    }
    if (showQueue) {
        QueueSheet(state = state, onEvent = viewModel::onEvent, onDismiss = { showQueue = false })
    }
}

@Composable
private fun TopBar(onClose: () -> Unit, onQueue: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = KpTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        KpIconButton(
            icon = Icons.Default.KeyboardArrowDown,
            contentDescription = stringResource(R.string.np_close),
            onClick = onClose,
        )
        Text(
            text = stringResource(R.string.np_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        KpIconButton(
            icon = KpIcons.QueueMusic,
            contentDescription = stringResource(R.string.np_open_queue),
            onClick = onQueue,
        )
    }
}

@Composable
private fun PlayerBody(
    state: NowPlayingState,
    positionMs: StateFlow<Long>,
    onEvent: (NowPlayingEvent) -> Unit,
) {
    val spacing = KpTheme.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(horizontal = spacing.xl)) {
        val landscape = maxWidth > maxHeight
        val artwork: @Composable (Modifier) -> Unit = { artModifier ->
            ArtworkImage(
                audioUri = state.track?.uri,
                cacheKey = state.track?.let { "album:${it.albumId}" }.orEmpty(),
                sizePx = ArtworkSize.FULL,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = artModifier,
            )
        }
        val controls: @Composable (Modifier) -> Unit = { controlsModifier ->
            Controls(state, positionMs, onEvent, controlsModifier)
        }
        if (landscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xl),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                artwork(Modifier.fillMaxHeight(0.85f).aspectRatio(1f))
                controls(Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    artwork(Modifier.fillMaxWidth().aspectRatio(1f))
                }
                controls(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Controls(
    state: NowPlayingState,
    positionMs: StateFlow<Long>,
    onEvent: (NowPlayingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KpTheme.spacing
    val playback = state.playback
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        TitleBlock(
            title = playback.title.orEmpty(),
            artist = playback.artist.orEmpty(),
            album = playback.album.orEmpty(),
        )
        SeekBar(
            positionMs = positionMs,
            durationMs = playback.durationMs,
            onSeek = { onEvent(NowPlayingEvent.SeekTo(it)) },
        )
        TransportRow(state, onEvent)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val track = state.track
            val favorite = track?.isFavorite == true
            IconButton(
                onClick = rememberHapticClick { onEvent(NowPlayingEvent.ToggleFavorite) },
                enabled = track != null,
            ) {
                Icon(
                    imageVector = if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(
                        if (favorite) R.string.np_favorite_remove else R.string.np_favorite_add,
                    ),
                    tint = if (favorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (track != null) {
                Text(
                    text = formatQuality(track.format, track.sampleRateHz, track.bitrateKbps),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TitleBlock(title: String, artist: String, album: String) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val secondary = listOf(artist, album).filter { it.isNotBlank() }.joinToString(" · ") { displayName(it) }
        if (secondary.isNotEmpty()) {
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Slider that follows playback until the user touches it. While dragging it shows the finger position, and after the
 * seek it holds the target for a moment so it does not jump back to the old position before the player catches up.
 */
@Composable
private fun SeekBar(positionMs: StateFlow<Long>, durationMs: Long, onSeek: (Long) -> Unit) {
    val position by positionMs.collectAsStateWithLifecycle()
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var settleTargetMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(position, settleTargetMs) {
        val target = settleTargetMs ?: return@LaunchedEffect
        if (kotlin.math.abs(position - target) < SEEK_SETTLE_TOLERANCE_MS) settleTargetMs = null
    }
    LaunchedEffect(settleTargetMs) {
        if (settleTargetMs != null) {
            delay(SEEK_SETTLE_MS)
            settleTargetMs = null
        }
    }

    val shownMs = dragFraction?.let { seekTargetMs(it, durationMs) } ?: settleTargetMs ?: position
    val fraction = dragFraction ?: progressFraction(shownMs, durationMs)
    val description = stringResource(R.string.np_position, formatDuration(shownMs), formatDuration(durationMs))
    val seekLabel = stringResource(R.string.np_seek)

    Column {
        Slider(
            value = fraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { final ->
                    val target = seekTargetMs(final, durationMs)
                    settleTargetMs = target
                    onSeek(target)
                }
                dragFraction = null
            },
            enabled = durationMs > 0,
            modifier = Modifier.semantics {
                contentDescription = seekLabel
                stateDescription = description
            },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(shownMs), style = MaterialTheme.typography.labelMedium)
            Text(formatDuration(durationMs), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun TransportRow(state: NowPlayingState, onEvent: (NowPlayingEvent) -> Unit) {
    val playback = state.playback
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ToggleIconButton(
            icon = KpIcons.Shuffle,
            contentDescription = stringResource(
                if (playback.shuffleEnabled) R.string.np_shuffle_on else R.string.np_shuffle_off,
            ),
            active = playback.shuffleEnabled,
            onClick = { onEvent(NowPlayingEvent.ToggleShuffle) },
        )
        KpIconButton(
            icon = KpIcons.SkipPrevious,
            contentDescription = stringResource(R.string.np_previous),
            onClick = { onEvent(NowPlayingEvent.Previous) },
        )
        FilledIconButton(
            onClick = rememberHapticClick { onEvent(NowPlayingEvent.TogglePlayPause) },
            modifier = Modifier.size(PlayButtonSize),
        ) {
            Icon(
                imageVector = if (playback.isPlaying) KpIcons.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(if (playback.isPlaying) R.string.np_pause else R.string.np_play),
                modifier = Modifier.size(40.dp),
            )
        }
        KpIconButton(
            icon = KpIcons.SkipNext,
            contentDescription = stringResource(R.string.np_next),
            onClick = { onEvent(NowPlayingEvent.Next) },
        )
        ToggleIconButton(
            icon = if (playback.repeatMode == RepeatMode.ONE) KpIcons.RepeatOne else KpIcons.Repeat,
            contentDescription = stringResource(
                when (playback.repeatMode) {
                    RepeatMode.OFF -> R.string.np_repeat_off
                    RepeatMode.ALL -> R.string.np_repeat_all
                    RepeatMode.ONE -> R.string.np_repeat_one
                },
            ),
            active = playback.repeatMode != RepeatMode.OFF,
            onClick = { onEvent(NowPlayingEvent.CycleRepeat) },
        )
    }
}

@Composable
private fun ToggleIconButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = rememberHapticClick(onClick)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
