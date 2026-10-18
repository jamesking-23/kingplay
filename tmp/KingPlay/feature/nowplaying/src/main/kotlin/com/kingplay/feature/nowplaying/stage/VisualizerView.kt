package com.kingplay.feature.nowplaying.stage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.repository.SpectrumSource
import com.kingplay.feature.nowplaying.R
import com.kingplay.feature.nowplaying.logic.smoothBars
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val LOW_POWER_FRAME_MS = 66L
private const val IDLE_FRAME_MS = 250L
private const val SILENT = 0.01f
private const val BAR_FILL = 0.7f
private const val MIN_BAR_DP = 3

/**
 * Bars that move with the music. The analysis only runs while this is on screen. With low-power visuals on, the
 * bars update about 15 times a second instead of every frame.
 */
@Composable
fun VisualizerView(source: SpectrumSource, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val lowPower = KpTheme.lowPowerVisuals
    var bars by remember(source) { mutableStateOf(FloatArray(source.bandCount)) }

    DisposableEffect(source) {
        source.setActive(true)
        onDispose { source.setActive(false) }
    }
    LaunchedEffect(source, isPlaying, lowPower) {
        val shown = FloatArray(source.bandCount)
        val silence = FloatArray(source.bandCount)
        while (isActive) {
            val resting = !isPlaying && shown.all { it < SILENT }
            when {
                resting -> delay(IDLE_FRAME_MS)
                lowPower -> delay(LOW_POWER_FRAME_MS)
                else -> withFrameNanos { }
            }
            smoothBars(shown, if (isPlaying) source.latest() else silence)
            bars = shown.copyOf()
        }
    }

    val color = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.np_visualizer)
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(KpTheme.spacing.lg)
            .semantics { contentDescription = description },
    ) {
        val slot = size.width / bars.size
        val barWidth = slot * BAR_FILL
        val minHeight = MIN_BAR_DP.dp.toPx()
        bars.forEachIndexed { i, level ->
            val height = (size.height * level).coerceAtLeast(minHeight)
            drawRoundRect(
                color = color,
                topLeft = Offset(i * slot + (slot - barWidth) / 2, size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2),
            )
        }
    }
}
