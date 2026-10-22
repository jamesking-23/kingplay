package com.kingplay.core.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingplay.core.designsystem.theme.KpIcons
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.ui.R
import com.kingplay.core.ui.artwork.ArtworkImage

private val ArtworkSizeDp = 52.dp
private val MinRowHeight = 64.dp

/**
 * The one list row used for songs, albums, artists, genres and folders, so they all look and behave alike.
 * Give it [artworkUri] for cover art or [leadingIcon] for things without art (folders, genres).
 * The whole row is one TalkBack stop that reads title and subtitle; the overflow menu is a second stop.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkUri: String? = null,
    artworkKey: String = "",
    leadingIcon: ImageVector? = null,
    trailingText: String? = null,
    highlighted: Boolean = false,
    menu: (@Composable ColumnScope.(dismiss: () -> Unit) -> Unit)? = null,
) {
    val spacing = KpTheme.spacing
    var menuOpen by remember { mutableStateOf(false) }
    val description = stringResource(R.string.ui_row_description, title, subtitle)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinRowHeight)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = menu?.let { { menuOpen = true } })
            .padding(horizontal = spacing.lg, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    selected = highlighted // TalkBack announces "selected" on the song that is playing
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkImage(
                audioUri = artworkUri,
                cacheKey = artworkKey,
                modifier = Modifier.size(ArtworkSizeDp),
                placeholderIcon = leadingIcon ?: KpIcons.MusicNote,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = spacing.md),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = spacing.xs),
                )
            }
        }
        if (menu != null) RowMenu(title = title, expanded = menuOpen, onExpanded = { menuOpen = it }, content = menu)
    }
}

@Composable
private fun RowMenu(
    title: String,
    expanded: Boolean,
    onExpanded: (Boolean) -> Unit,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    Box {
        IconButton(onClick = { onExpanded(true) }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.ui_more_options, title),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpanded(false) }) {
            content { onExpanded(false) }
        }
    }
}
