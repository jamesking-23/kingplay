package com.kingplay.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.kingplay.core.model.settings.HapticKind

/** Icon button with a mandatory content description (TalkBack) and a 48dp minimum touch target. */
@Composable
fun KpIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    haptic: HapticKind = HapticKind.TAP,
) {
    IconButton(onClick = rememberHapticClick(haptic, onClick), modifier = modifier, enabled = enabled) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}
