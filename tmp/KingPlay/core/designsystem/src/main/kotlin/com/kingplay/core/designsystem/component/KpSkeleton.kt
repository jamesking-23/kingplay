package com.kingplay.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingplay.core.designsystem.R
import com.kingplay.core.designsystem.theme.KpTheme

/** Placeholder block. Shimmers normally, stays static in low-power visuals mode. */
@Composable
fun KpSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    val brush = if (KpTheme.lowPowerVisuals) {
        Brush.linearGradient(listOf(base, base))
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val shift by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 1_200, easing = LinearEasing), RepeatMode.Restart),
            label = "skeletonShift",
        )
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(shift - 500f, 0f),
            end = Offset(shift, 0f),
        )
    }
    androidx.compose.foundation.layout.Box(modifier = modifier.clip(shape).background(brush))
}

/** List of skeleton rows (artwork + two text lines). Announces one "Loading" to TalkBack. */
@Composable
fun KpSkeletonList(
    modifier: Modifier = Modifier,
    rows: Int = 6,
) {
    val loading = stringResource(R.string.kp_loading)
    Column(
        modifier = modifier.semantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.md),
    ) {
        repeat(rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KpTheme.spacing.lg)
                    .clearAndSetSemantics { },
                horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KpSkeleton(modifier = Modifier.size(56.dp), shape = MaterialTheme.shapes.medium)
                Column(verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
                    KpSkeleton(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp), shape = CircleShape)
                    KpSkeleton(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp), shape = CircleShape)
                }
            }
        }
    }
}
