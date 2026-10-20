package com.kingplay.core.ui.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The person's round profile picture, or their initial on a coloured disc when there is no picture. */
@Composable
fun ProfileAvatar(photoPath: String?, nickname: String, size: Dp, modifier: Modifier = Modifier) {
    val picture: ImageBitmap? by produceState<ImageBitmap?>(null, photoPath) {
        value = photoPath?.let { path ->
            withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
        }
    }
    val label = "Profile picture of ${nickname.ifBlank { "you" }}"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        val shown = picture
        if (shown != null) {
            Image(bitmap = shown, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Text(
                text = nickname.trim().take(1).uppercase().ifEmpty { "?" },
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = initialSize(size.value),
            )
        }
    }
}

private fun initialSize(dp: Float): TextUnit = (dp * INITIAL_RATIO).sp

private const val INITIAL_RATIO = 0.42f
