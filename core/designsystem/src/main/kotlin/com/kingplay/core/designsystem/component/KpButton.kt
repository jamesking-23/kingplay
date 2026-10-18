package com.kingplay.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.designsystem.theme.UiStyle

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
    if (KpTheme.uiStyle == UiStyle.KING_ART) {
        SketchButton(click, sized, enabled, style, label)
        return
    }
    when (style) {
        KpButtonStyle.Filled -> Button(onClick = click, modifier = sized, enabled = enabled) { label() }
        KpButtonStyle.Tonal -> FilledTonalButton(onClick = click, modifier = sized, enabled = enabled) { label() }
        KpButtonStyle.Outlined -> OutlinedButton(onClick = click, modifier = sized, enabled = enabled) { label() }
        KpButtonStyle.Text -> TextButton(onClick = click, modifier = sized, enabled = enabled) { label() }
    }
}

/** King Art buttons: a pencil outline on paper, with the primary button highlighted like a marker swipe. */
@Composable
private fun SketchButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    style: KpButtonStyle,
    label: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ink = colors.onSurface
    val container = when (style) {
        KpButtonStyle.Filled -> colors.primaryContainer
        KpButtonStyle.Tonal -> colors.surfaceContainerHigh
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val content = if (style == KpButtonStyle.Filled) colors.onPrimaryContainer else ink
    val border = if (style == KpButtonStyle.Text) null else BorderStroke(SKETCH_BORDER, ink)
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = DISABLED_ALPHA),
            disabledContentColor = ink.copy(alpha = DISABLED_CONTENT_ALPHA),
        ),
        border = border,
        elevation = null,
    ) { label() }
}

private val SKETCH_BORDER = 1.5.dp
private const val DISABLED_ALPHA = 0.4f
private const val DISABLED_CONTENT_ALPHA = 0.4f
