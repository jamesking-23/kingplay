package com.kingplay.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.designsystem.theme.closedSmoothPath
import com.kingplay.core.model.theme.Sketch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A picture-disc record: [art] (the song's cover) is clipped into the disc face under hand-drawn grooves, with a
 * paper label and a spindle dot. It spins while [spinning] unless low-power visuals are on.
 */
@Composable
fun VinylDisc(
    spinning: Boolean,
    modifier: Modifier = Modifier,
    art: @Composable BoxScope.() -> Unit,
) {
    val lowPower = KpTheme.lowPowerVisuals
    var angle by remember { mutableFloatStateOf(0f) }
    val shouldSpin = spinning && !lowPower
    LaunchedEffect(shouldSpin) {
        if (!shouldSpin) return@LaunchedEffect
        var last = androidx.compose.runtime.withFrameNanos { it }
        while (true) {
            val now = androidx.compose.runtime.withFrameNanos { it }
            angle = (angle + (now - last) / NANOS_PER_SECOND * DEGREES_PER_SECOND) % FULL_TURN
            last = now
        }
    }
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.surface
    val accent = MaterialTheme.colorScheme.primary
    Box(modifier = modifier.aspectRatio(1f).graphicsLayer { rotationZ = angle }) {
        Box(modifier = Modifier.fillMaxSize().padding(RIM).clip(CircleShape)) { art() }
        Canvas(modifier = Modifier.fillMaxSize()) { drawDiscDetails(ink, paper, accent) }
    }
}

private fun DrawScope.drawDiscDetails(ink: Color, paper: Color, accent: Color) {
    val c = center
    val r = size.minDimension / 2f
    val stroke = (r * 0.012f).coerceAtLeast(1.2f)

    // Grooves: thin rings between the label and the rim.
    var ring = 0.40f
    var i = 0
    while (ring < 0.96f) {
        drawCircle(ink.copy(alpha = if (i % 3 == 0) 0.22f else 0.11f), r * ring, c, style = Stroke(stroke))
        ring += 0.045f
        i++
    }

    // Sheen: two slanted bands across the face, like light on vinyl.
    for ((k, offset) in listOf(-0.55f, 0.15f).withIndex()) {
        val x0 = c.x + r * offset
        val a = 0.45f
        val band = androidx.compose.ui.graphics.Path().apply {
            moveTo(x0, c.y - r)
            lineTo(x0 + r * 0.28f, c.y - r)
            lineTo(x0 + r * 0.28f + r * a, c.y + r)
            lineTo(x0 + r * a, c.y + r)
            close()
        }
        androidx.compose.ui.graphics.drawscope.clipPath(
            androidx.compose.ui.graphics.Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r * 0.97f)) },
        ) { drawPath(band, ink.copy(alpha = 0.16f - k * 0.04f)) }
    }

    // Rim, drawn twice with a different wobble for a pencil look.
    for (pass in 0..1) {
        drawPath(wobblyCircle(c, r * (0.985f - pass * 0.01f), seed = 5 + pass * 9, wobble = r * 0.006f), ink, style = Stroke(stroke * 1.7f))
    }

    // Paper label with a coloured centre and the spindle hole.
    drawCircle(paper, r * 0.21f, c)
    drawPath(wobblyCircle(c, r * 0.21f, seed = 21, wobble = r * 0.005f), ink, style = Stroke(stroke * 1.5f))
    drawCircle(accent.copy(alpha = 0.85f), r * 0.12f, c)
    drawPath(wobblyCircle(c, r * 0.12f, seed = 33, wobble = r * 0.004f), ink, style = Stroke(stroke))
    drawCircle(ink, r * 0.028f, c)
}

private fun wobblyCircle(center: Offset, radius: Float, seed: Int, wobble: Float): androidx.compose.ui.graphics.Path {
    val random = Sketch.Random(seed)
    val n = CIRCLE_POINTS
    val pts = FloatArray(n * 2)
    for (i in 0 until n) {
        val a = i.toFloat() / n * 2f * PI.toFloat()
        val rr = radius + random.signed() * wobble
        pts[i * 2] = center.x + cos(a) * rr
        pts[i * 2 + 1] = center.y + sin(a) * rr
    }
    return closedSmoothPath(pts)
}

private val RIM = 3.dp
private const val CIRCLE_POINTS = 28
private const val NANOS_PER_SECOND = 1_000_000_000f
private const val DEGREES_PER_SECOND = 36f
private const val FULL_TURN = 360f
