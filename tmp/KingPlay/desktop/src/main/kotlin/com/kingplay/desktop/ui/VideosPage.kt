package com.kingplay.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingplay.desktop.video.VideoFile
import com.kingplay.desktop.video.VideoProgress
import com.kingplay.desktop.video.formatSize
import com.kingplay.desktop.video.resumePositionMs

@Composable
fun VideosPage(
    videos: List<VideoFile>,
    progress: Map<Long, VideoProgress>,
    onPlay: (VideoFile, startMs: Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Videos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
        if (videos.isEmpty()) {
            Text(
                "No videos found in your library folders. Add a folder that has videos on the Library page.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(videos, key = { it.id }) { video ->
                val resumeAt = resumePositionMs(progress[video.id])
                val detail = buildString {
                    append(folderLabel(video.folder)).append(" · ").append(formatSize(video.sizeBytes))
                    if (resumeAt > 0) append(" · Resume at ").append(formatTime(resumeAt))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(role = Role.Button) { onPlay(video, resumeAt) }
                        .semantics { contentDescription = "${video.title}, $detail" }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            video.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
