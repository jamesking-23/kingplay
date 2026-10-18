package com.kingplay.core.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kingplay.core.designsystem.theme.KpTheme

enum class KpButtonStyle { Filled, Tonal, Outlined, Text }

private val MinTouchTarget = 48.dp

@Composable
fun KpButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: KpButtonStyle = KpButtonStyle.Filled,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val click = rememberHapticClick(onClick)
    val sized = modifier.heightIn(min = MinTouchTarget)
    val label: @Composable () -> Unit = {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(KpTheme.spacing.sm))
        }
        Text(text)
    }
    when (style) {
        KpButtonStyle.Filled -> Button(onClick = click, modifier = sized, enabled = enabled) { label() }
        KpButtonStyle.Tonal -> FilledTonalButton(onClick = click, modifier = sized, enabled = enabled) { label() }
        KpButtonStyle.Outlined -> OutlinedButton(onClick = click, modifier = sized, enabled = enabled) { label() }
        KpButtonStyle.Text -> TextButton(onClick = click, modifier = sized, enabled = enabled) { label() }
    }
}
