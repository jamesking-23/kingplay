package com.kingplay.feature.nowplaying.stage

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.lyrics.LyricLine
import com.kingplay.core.model.lyrics.Lyrics
import com.kingplay.core.model.lyrics.LyricsState
import com.kingplay.core.model.lyrics.activeLineIndex
import com.kingplay.feature.nowplaying.R
import kotlinx.coroutines.flow.StateFlow

private const val AUTO_SCROLL_PAUSE_NS = 3_000_000_000L
private const val VIEWPORT_ANCHOR_DIVISOR = 3
private const val INACTIVE_ALPHA = 0.55f

/**
 * Lyrics of the playing song: follows the music when they are timed, scrolls freely when they are not. When the song
 * has none, the user can import a .lrc file for it.
 */
@Composable
fun LyricsView(
    state: LyricsState,
    positionMs: StateFlow<Long>,
    imported: Boolean,
    importFailed: Boolean,
    onSeek: (Long) -> Unit,
    onImport: () -> Unit,
    onFindOnline: () -> Unit,
    onRemoveImported: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        LyricsState.Loading -> Box(modifier)
        LyricsState.None -> Column(
            modifier = modifier.fillMaxSize().padding(KpTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.md, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.np_lyrics_none),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (importFailed) {
                Text(
                    text = stringResource(R.string.np_lyrics_import_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            OutlinedButton(onClick = onFindOnline) { Text(stringResource(R.string.np_lyrics_find_online)) }
            OutlinedButton(onClick = onImport) { Text(stringResource(R.string.np_lyrics_import)) }
        }
        is LyricsState.Available -> Box(modifier) {
            LyricsList(state.lyrics, positionMs, onSeek, Modifier)
            if (imported) {
                TextButton(onClick = onRemoveImported, modifier = Modifier.align(Alignment.BottomEnd)) {
                    Text(stringResource(R.string.np_lyrics_remove_imported))
                }
            }
        }
    }
}

@Composable
private fun LyricsList(
    lyrics: Lyrics,
    positionMs: StateFlow<Long>,
    onSeek: (Long) -> Unit,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    val position = positionMs.collectAsStateWithLifecycle()
    val active by remember(lyrics) { derivedStateOf { activeLineIndex(lyrics, position.value) } }
    var lastUserScrollNs by remember { mutableLongStateOf(0L) }
    val lowPower = KpTheme.lowPowerVisuals

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) lastUserScrollNs = System.nanoTime()
    }
    LaunchedEffect(active, lyrics) {
        val idle = System.nanoTime() - lastUserScrollNs > AUTO_SCROLL_PAUSE_NS
        if (lyrics.synced && active >= 0 && idle && !listState.isScrollInProgress) {
            val offset = -(listState.layoutInfo.viewportSize.height / VIEWPORT_ANCHOR_DIVISOR)
            if (lowPower) listState.scrollToItem(active, offset) else listState.animateScrollToItem(active, offset)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = KpTheme.spacing.lg, vertical = KpTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm),
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            LyricRow(
                line = line,
                isActive = lyrics.synced && index == active,
                onClick = line.timeMs?.let { time -> { onSeek(time) } },
            )
        }
    }
}

@Composable
private fun LyricRow(line: LyricLine, isActive: Boolean, onClick: (() -> Unit)?) {
    if (line.text.isEmpty()) {
        Box(Modifier.fillMaxWidth().heightIn(min = 12.dp))
        return
    }
    val color by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = INACTIVE_ALPHA)
        },
        label = "lyricColor",
    )
    val tap = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Text(
        text = line.text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .then(tap)
            .heightIn(min = 48.dp)
            .semantics { selected = isActive },
    )
}
