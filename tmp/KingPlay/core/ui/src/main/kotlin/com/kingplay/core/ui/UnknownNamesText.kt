package com.kingplay.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kingplay.core.model.UnknownNames

/** The library stores English placeholders for missing tags; this shows them in the user's language. */
@Composable
fun displayName(stored: String): String = when (stored) {
    UnknownNames.ARTIST -> stringResource(R.string.ui_unknown_artist)
    UnknownNames.ALBUM -> stringResource(R.string.ui_unknown_album)
    UnknownNames.VARIOUS_ARTISTS -> stringResource(R.string.ui_various_artists)
    else -> stored
}
