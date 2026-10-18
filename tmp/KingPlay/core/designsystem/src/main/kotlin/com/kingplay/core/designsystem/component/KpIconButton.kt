package com.kingplay.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/** Icon button with a mandatory content description (TalkBack) and a 48dp minimum touch target. */
@Composable
fun KpIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(onClick = rememberHapticClick(onClick), modifier = modifier, enabled = enabled) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}
