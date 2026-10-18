package com.kingplay.core.ui.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kingplay.core.model.Track
import com.kingplay.core.ui.R

/** The actions every song row offers. Pass it as the `menu` of [MediaRow]. */
@Composable
fun ColumnScope.TrackMenuItems(
    track: Track,
    dismiss: () -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.ui_play_next)) },
        onClick = {
            dismiss()
            onPlayNext(track)
        },
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.ui_add_to_queue)) },
        onClick = {
            dismiss()
            onAddToQueue(track)
        },
    )
    DropdownMenuItem(
        text = {
            Text(
                stringResource(
                    if (track.isFavorite) R.string.ui_remove_favorite else R.string.ui_add_favorite,
                ),
            )
        },
        onClick = {
            dismiss()
            onToggleFavorite(track)
        },
    )
}
