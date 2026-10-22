package com.kingplay.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingplay.core.model.Track
import com.kingplay.core.model.lyrics.Lyrics
import com.kingplay.core.model.lyrics.activeLineIndex
import com.kingplay.desktop.DesktopAppState
import com.kingplay.desktop.lyrics.LyricsResolver
import com.kingplay.desktop.lyrics.MAX_LYRICS_BYTES
import com.kingplay.desktop.lyrics.decodeLyricsBytes

private sealed interface LyricsLoad {
    data object Loading : LyricsLoad

    data class Done(val lyrics: Lyrics?) : LyricsLoad
}

@Composable
fun LyricsPage(
    app: DesktopAppState,
    track: Track?,
    positionMs: Long,
    importedKeys: Set<String>,
    onNotice: (String) -> Unit,
) {
    if (track == null) {
        Message("Play a song to see its lyrics here.")
        return
    }
    val key = LyricsResolver.keyOf(track)
    val imported = key in importedKeys
    // Re-read when the song changes or the person imports or removes lyrics for it.
    val load by produceState<LyricsLoad>(LyricsLoad.Loading, track.id, imported) {
        value = LyricsLoad.Loading
        value = LyricsLoad.Done(app.lyricsFor(track))
    }
    var pending by remember(track.id) { mutableStateOf<String?>(null) }
    pending?.let { text ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Add these lyrics?") },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    Text(text.take(PREVIEW_CHARS) + if (text.length > PREVIEW_CHARS) "\u2026" else "")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val saved = text.length <= MAX_LYRICS_BYTES && app.importLyrics(track, text)
                    onNotice(if (saved) "Lyrics saved for ${track.title}." else "That text could not be used as lyrics.")
                    pending = null
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
        )
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Text(
                    track.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            OutlinedButton(onClick = { openLyricsSearch(track, onNotice) }) { Text("Find lyrics online") }
            OutlinedButton(onClick = {
                val copied = readClipboardText()
                if (copied.isBlank()) {
                    onNotice("Nothing copied yet. Select the lyrics on the web page, copy them, then come back.")
                } else {
                    pending = copied
                }
            }) { Text("Add copied lyrics") }
            OutlinedButton(onClick = { importLrc(app, track, onNotice) }) { Text("Import .lrc") }
            if (imported) {
                OutlinedButton(onClick = { app.removeImportedLyrics(track) }) { Text("Remove imported") }
            }
        }
        when (val state = load) {
            LyricsLoad.Loading -> Message("Looking for lyrics…")
            is LyricsLoad.Done -> {
                val lyrics = state.lyrics
                if (lyrics == null) {
                    Message(
                        "No lyrics found. KingPlay looks for a .lrc file next to the song, then inside the song's " +
                            "tags. You can also import a .lrc file for this song.",
                    )
                } else {
                    LyricsLines(lyrics, positionMs, onSeek = { app.controller?.seekTo(it) })
                }
            }
        }
    }
}

@Composable
private fun LyricsLines(lyrics: Lyrics, positionMs: Long, onSeek: (Long) -> Unit) {
    val active = if (lyrics.synced) activeLineIndex(lyrics, positionMs) else -1
    val listState = rememberLazyListState()
    // Keep the current line near the top third so upcoming lines stay visible.
    LaunchedEffect(active) {
        if (active >= 0) listState.animateScrollToItem((active - FOLLOW_OFFSET).coerceAtLeast(0))
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        itemsIndexed(lyrics.lines) { position, line ->
            val current = position == active
            val timeMs = line.timeMs
            Text(
                text = line.text.ifBlank { "·" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    current -> MaterialTheme.colorScheme.primary
                    lyrics.synced -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (timeMs != null) {
                            Modifier.clickable(role = Role.Button) { onSeek(timeMs) }
                        } else {
                            Modifier
                        },
                    )
                    .padding(vertical = 8.dp)
                    .semantics { selected = current },
            )
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}

private fun importLrc(app: DesktopAppState, track: Track, onNotice: (String) -> Unit) {
    val picked = chooseLyricsFile() ?: return
    if (picked.length() > MAX_LYRICS_BYTES) {
        onNotice("That file is too big to be lyrics.")
        return
    }
    val text = runCatching { decodeLyricsBytes(picked.readBytes()) }.getOrNull()
    val saved = text != null && app.importLyrics(track, text)
    onNotice(if (saved) "Lyrics saved for ${track.title}." else "No lyrics found in that file.")
}

private const val FOLLOW_OFFSET = 3
private const val PREVIEW_CHARS = 1_500

/** The web search for a song's lyrics, as an address. */
fun lyricsSearchUrl(track: Track): String =
    "https://duckduckgo.com/?q=" + java.net.URLEncoder.encode("${track.artistName} ${track.title} lyrics", "UTF-8")

private fun openLyricsSearch(track: Track, onNotice: (String) -> Unit) {
    val opened = runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(lyricsSearchUrl(track))) }.isSuccess
    if (!opened) onNotice("Could not open your web browser.")
}

private fun readClipboardText(): String = runCatching {
    java.awt.Toolkit.getDefaultToolkit().systemClipboard
        .getData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String
}.getOrNull().orEmpty().trim()
