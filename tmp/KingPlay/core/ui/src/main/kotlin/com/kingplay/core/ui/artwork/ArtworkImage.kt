package com.kingplay.core.ui.artwork

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kingplay.core.designsystem.theme.KpIcons

/** Standard decode sizes. Lists stay small to save memory; Now Playing asks for the large one. */
object ArtworkSize {
    const val LIST = 192
    const val CARD = 384
    const val FULL = 1024
}

/**
 * Cover art with a quiet placeholder underneath: the placeholder shows while loading and when the file has no art,
 * so there is never a blank hole. Decorative by default; the row it sits in already says what it is.
 */
@Composable
fun ArtworkImage(
    audioUri: String?,
    cacheKey: String,
    modifier: Modifier = Modifier,
    sizePx: Int = ArtworkSize.LIST,
    shape: Shape = MaterialTheme.shapes.medium,
    placeholderIcon: ImageVector = KpIcons.MusicNote,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = placeholderIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize(PLACEHOLDER_FRACTION),
        )
        if (audioUri != null) {
            val context = LocalContext.current
            val request = remember(audioUri, cacheKey, sizePx) {
                ImageRequest.Builder(context)
                    .data(ArtworkModel(audioUri, cacheKey, sizePx))
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private const val PLACEHOLDER_FRACTION = 0.5f
