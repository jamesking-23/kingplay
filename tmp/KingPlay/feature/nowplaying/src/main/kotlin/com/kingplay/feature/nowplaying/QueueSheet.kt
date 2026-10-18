package com.kingplay.feature.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kingplay.core.designsystem.theme.KpIcons
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.ui.component.MediaRow
import com.kingplay.core.ui.displayName

/**
 * The play queue. Rows can be moved with menu items instead of dragging, so the list is fully usable with TalkBack
 * and switch access; drag-to-reorder can be added on top later.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    state: NowPlayingState,
    onEvent: (NowPlayingEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val queue = state.queue
    val currentIndex = state.playback.currentIndex
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (currentIndex - 1).coerceIn(0, (queue.size - 1).coerceAtLeast(0)),
    )
    val unavailable = stringResource(R.string.np_unavailable)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KpTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.np_queue),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            TextButton(
                onClick = {
                    onEvent(NowPlayingEvent.ClearQueue)
                    onDismiss()
                },
                enabled = queue.isNotEmpty(),
            ) { Text(stringResource(R.string.np_queue_clear)) }
        }
        if (queue.isEmpty()) {
            Text(
                text = stringResource(R.string.np_queue_empty),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(KpTheme.spacing.lg),
            )
        } else {
            LazyColumn(state = listState, modifier = Modifier.navigationBarsPadding()) {
                itemsIndexed(queue, key = { index, item -> "${item.ref}#$index" }) { index, item ->
                    MediaRow(
                        title = item.title ?: unavailable,
                        subtitle = item.subtitle?.let { displayName(it) }.orEmpty(),
                        artworkUri = item.artworkUri,
                        artworkKey = item.artworkKey,
                        leadingIcon = if (item.isVideo) Icons.Default.PlayArrow else KpIcons.MusicNote,
                        highlighted = index == currentIndex,
                        onClick = { onEvent(NowPlayingEvent.SkipTo(index)) },
                        menu = { dismiss ->
                            if (index > 0) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.np_move_up)) },
                                    onClick = {
                                        dismiss()
                                        onEvent(NowPlayingEvent.MoveUp(index))
                                    },
                                )
                            }
                            if (index < queue.lastIndex) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.np_move_down)) },
                                    onClick = {
                                        dismiss()
                                        onEvent(NowPlayingEvent.MoveDown(index))
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.np_remove)) },
                                onClick = {
                                    dismiss()
                                    onEvent(NowPlayingEvent.RemoveAt(index))
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}
