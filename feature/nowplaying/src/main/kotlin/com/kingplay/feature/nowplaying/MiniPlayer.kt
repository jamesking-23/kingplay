package com.kingplay.feature.nowplaying

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.draw.shadow
import com.kingplay.core.designsystem.theme.UiStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.component.KpIconButton
import com.kingplay.core.model.settings.HapticKind
import com.kingplay.core.designsystem.component.shakeOn
import com.kingplay.core.designsystem.theme.KpIcons
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.ui.artwork.ArtworkImage
import com.kingplay.core.ui.displayName

private val MiniArtwork = 44.dp

/**
 * Sits above the navigation bar on every screen except Now Playing, and hides itself when nothing is loaded.
 * The artwork and text are one tap target that opens Now Playing; play/pause and next stay separate buttons.
 */
@Composable
fun MiniPlayer(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MiniPlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.visible) return
    var showQueue by rememberSaveable { mutableStateOf(false) }
    if (showQueue) {
        val queueModel: NowPlayingViewModel = hiltViewModel()
        val queueState by queueModel.state.collectAsStateWithLifecycle()
        QueueSheet(state = queueState, onEvent = queueModel::onEvent, onDismiss = { showQueue = false })
    }

    val artist = displayName(state.artist)
    val openDescription = if (artist.isBlank()) {
        stringResource(R.string.np_mini_open_no_artist, state.title)
    } else {
        stringResource(R.string.np_mini_open, state.title, artist)
    }
    val style = KpTheme.uiStyle
    val shape = when (style) {
        UiStyle.KING_CLASSIC -> RoundedCornerShape(4.dp)
        UiStyle.KING_NEON -> RoundedCornerShape(14.dp)
        UiStyle.KING_GLASS -> RoundedCornerShape(28.dp)
        else -> RoundedCornerShape(20.dp)
    }
    val tint = if (style == UiStyle.KING_GLASS) 0.85f else 1f
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .shadow(8.dp, shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp).copy(alpha = tint),
        border = if (style == UiStyle.KING_NEON) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        } else {
            null
        },
    ) {
        Column {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
            )
            Row(
                modifier = Modifier.padding(horizontal = KpTheme.spacing.sm, vertical = KpTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(role = Role.Button, onClick = onOpen)
                        .semantics(mergeDescendants = true) { contentDescription = openDescription },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtworkImage(
                        audioUri = state.artworkUri,
                        cacheKey = state.artworkKey,
                        modifier = Modifier.size(MiniArtwork),
                    )
                    Column(modifier = Modifier.weight(1f).padding(horizontal = KpTheme.spacing.md)) {
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (artist.isNotBlank()) {
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                KpIconButton(
                    icon = if (state.isPlaying) KpIcons.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(if (state.isPlaying) R.string.np_pause else R.string.np_play),
                    onClick = { viewModel.onEvent(MiniPlayerEvent.TogglePlayPause) },
                    haptic = HapticKind.PLAY,
                    modifier = Modifier.shakeOn(state.isPlaying),
                )
                KpIconButton(
                    icon = KpIcons.QueueMusic,
                    contentDescription = stringResource(R.string.np_open_queue),
                    onClick = { showQueue = true },
                )
                KpIconButton(
                    icon = KpIcons.SkipNext,
                    contentDescription = stringResource(R.string.np_next),
                    onClick = { viewModel.onEvent(MiniPlayerEvent.Next) },
                    haptic = HapticKind.TRANSPORT,
                )
            }
        }
    }
}
