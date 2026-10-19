package com.kingplay.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingplay.core.model.settings.AppUiTheme
import com.kingplay.core.model.theme.KingArtPalette
import com.kingplay.core.model.theme.Sketch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Which design is active ("King Art" or "kk-ui"); lets deep widgets such as the cover pick their look. */
val LocalUiTheme = staticCompositionLocalOf { AppUiTheme.KING_ART }

private val Paper = Color(KingArtPalette.PAPER)
private val Ink = Color(KingArtPalette.INK)
private val Yellow = Color(KingArtPalette.YELLOW)

/** Same palette as Android: pencil on paper (light), chalk on charcoal (dark). */
internal fun kingArtColorScheme(dark: Boolean, black: Boolean): ColorScheme = if (dark) {
    val bg = if (black) Color.Black else Color(KingArtPalette.DARK_PAPER)
    val ink = Color(KingArtPalette.DARK_INK)
    val container = Color(KingArtPalette.DARK_YELLOW_CONTAINER)
    darkColorScheme(
        primary = Color(KingArtPalette.DARK_RED),
        onPrimary = Color(KingArtPalette.DARK_PAPER),
        primaryContainer = container,
        onPrimaryContainer = Yellow,
        secondary = ink,
        onSecondary = bg,
        secondaryContainer = Color(KingArtPalette.DARK_PAPER_SHADE_HIGH),
        onSecondaryContainer = ink,
        tertiary = Yellow,
        tertiaryContainer = container,
        onTertiaryContainer = Yellow,
        background = bg,
        onBackground = ink,
        surface = bg,
        onSurface = ink,
        surfaceVariant = Color(KingArtPalette.DARK_PAPER_SHADE),
        onSurfaceVariant = Color(KingArtPalette.DARK_INK_SOFT),
        surfaceContainerLowest = bg,
        surfaceContainerLow = Color(0xFF242220),
        surfaceContainer = Color(KingArtPalette.DARK_PAPER_SHADE),
        surfaceContainerHigh = Color(KingArtPalette.DARK_PAPER_SHADE_HIGH),
        surfaceContainerHighest = Color(0xFF3E3B36),
        outline = Color(KingArtPalette.DARK_INK_SOFT),
        outlineVariant = Color(0xFF4A463F),
    )
} else {
    lightColorScheme(
        primary = Color(0xFFC74440),
        onPrimary = Paper,
        primaryContainer = Yellow,
        onPrimaryContainer = Ink,
        secondary = Ink,
        onSecondary = Paper,
        secondaryContainer = Color(KingArtPalette.PAPER_SHADE_HIGH),
        onSecondaryContainer = Ink,
        tertiary = Color(KingArtPalette.RED),
        tertiaryContainer = Yellow,
        onTertiaryContainer = Ink,
        background = Paper,
        onBackground = Ink,
        surface = Paper,
        onSurface = Ink,
        surfaceVariant = Color(KingArtPalette.PAPER_SHADE),
        onSurfaceVariant = Color(KingArtPalette.INK_SOFT),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF5F2EC),
        surfaceContainer = Color(KingArtPalette.PAPER_SHADE),
        surfaceContainerHigh = Color(KingArtPalette.PAPER_SHADE_HIGH),
        surfaceContainerHighest = Color(0xFFDDD7CC),
        outline = Ink,
        outlineVariant = Color(0xFFCFC9BE),
    )
}

private val Caveat = FontFamily(
    Font("fonts/caveat_semibold.ttf", FontWeight.Normal),
    Font("fonts/caveat_semibold.ttf", FontWeight.SemiBold),
    Font("fonts/caveat_bold.ttf", FontWeight.Bold),
)
private val Architects = FontFamily(Font("fonts/architects_daughter.ttf", FontWeight.Normal))

private val base = Typography()

private fun TextStyle.heading(scale: Float, weight: FontWeight = FontWeight.Bold) = copy(
    fontFamily = Caveat,
    fontWeight = weight,
    fontSize = fontSize * scale,
    lineHeight = lineHeight * scale,
    letterSpacing = 0.sp,
)

private fun TextStyle.hand() = copy(fontFamily = Architects, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)

internal val KingArtTypography = Typography(
    displayLarge = base.displayLarge.heading(1.1f),
    displayMedium = base.displayMedium.heading(1.1f),
    displaySmall = base.displaySmall.heading(1.1f),
    headlineLarge = base.headlineLarge.heading(1.2f),
    headlineMedium = base.headlineMedium.heading(1.2f),
    headlineSmall = base.headlineSmall.heading(1.2f),
    titleLarge = base.titleLarge.heading(1.25f),
    titleMedium = base.titleMedium.heading(1.2f, FontWeight.SemiBold),
    titleSmall = base.titleSmall.heading(1.15f, FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.hand(),
    bodyMedium = base.bodyMedium.hand(),
    bodySmall = base.bodySmall.hand(),
    labelLarge = base.labelLarge.hand(),
    labelMedium = base.labelMedium.hand(),
    labelSmall = base.labelSmall.hand(),
)

/** A rounded rectangle whose edge wobbles like a pencil line; seeded so it never shimmers. */
data class SketchShape(val corner: Dp, val seed: Int, val wobble: Dp = 1.1.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val pts = with(density) {
            Sketch.roundRect(size.width, size.height, corner.toPx(), seed, wobble.toPx(), 14.dp.toPx())
        }
        return Outline.Generic(closedSmoothPath(pts))
    }
}

internal fun closedSmoothPath(pts: FloatArray): Path {
    val n = pts.size / 2
    val path = Path()
    if (n < 3) return path
    fun x(i: Int) = pts[(i % n) * 2]
    fun y(i: Int) = pts[(i % n) * 2 + 1]
    path.moveTo((x(n - 1) + x(0)) / 2f, (y(n - 1) + y(0)) / 2f)
    for (i in 0 until n) path.quadraticTo(x(i), y(i), (x(i) + x(i + 1)) / 2f, (y(i) + y(i + 1)) / 2f)
    path.close()
    return path
}

internal val KingArtShapes = Shapes(
    extraSmall = SketchShape(4.dp, 11),
    small = SketchShape(10.dp, 23),
    medium = SketchShape(16.dp, 37),
    large = SketchShape(22.dp, 41),
    extraLarge = SketchShape(30.dp, 53),
)

/** A picture-disc record: [art] is clipped into the disc under hand-drawn grooves. Spins while [spinning]. */
@Composable
fun VinylDisc(spinning: Boolean, modifier: Modifier = Modifier, art: @Composable BoxScope.() -> Unit) {
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(spinning) {
        if (!spinning) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            angle = (angle + (now - last) / 1_000_000_000f * DEGREES_PER_SECOND) % 360f
            last = now
        }
    }
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.surface
    val accent = MaterialTheme.colorScheme.primary
    Box(modifier = modifier.aspectRatio(1f).graphicsLayer { rotationZ = angle }) {
        Box(modifier = Modifier.fillMaxSize().padding(1.dp).clip(CircleShape)) { art() }
        Canvas(modifier = Modifier.fillMaxSize()) { drawDiscDetails(ink, paper, accent) }
    }
}

private fun DrawScope.drawDiscDetails(ink: Color, paper: Color, accent: Color) {
    val c = center
    val r = size.minDimension / 2f
    val stroke = (r * 0.012f).coerceAtLeast(1f)
    var ring = 0.40f
    var i = 0
    while (ring < 0.96f) {
        drawCircle(ink.copy(alpha = if (i % 3 == 0) 0.22f else 0.11f), r * ring, c, style = Stroke(stroke))
        ring += 0.045f
        i++
    }
    for ((k, offset) in listOf(-0.55f, 0.15f).withIndex()) {
        val x0 = c.x + r * offset
        val band = Path().apply {
            moveTo(x0, c.y - r)
            lineTo(x0 + r * 0.28f, c.y - r)
            lineTo(x0 + r * 0.28f + r * 0.45f, c.y + r)
            lineTo(x0 + r * 0.45f, c.y + r)
            close()
        }
        clipPath(Path().apply { addOval(Rect(c, r * 0.97f)) }) { drawPath(band, ink.copy(alpha = 0.16f - k * 0.04f)) }
    }
    for (pass in 0..1) {
        drawPath(wobblyCircle(c, r * (0.985f - pass * 0.01f), 5 + pass * 9, r * 0.006f), ink, style = Stroke(stroke * 1.7f))
    }
    drawCircle(paper, r * 0.21f, c)
    drawPath(wobblyCircle(c, r * 0.21f, 21, r * 0.005f), ink, style = Stroke(stroke * 1.5f))
    drawCircle(accent.copy(alpha = 0.85f), r * 0.12f, c)
    drawPath(wobblyCircle(c, r * 0.12f, 33, r * 0.004f), ink, style = Stroke(stroke))
    drawCircle(ink, r * 0.028f, c)
}

private fun wobblyCircle(center: Offset, radius: Float, seed: Int, wobble: Float): Path {
    val random = Sketch.Random(seed)
    val n = 28
    val pts = FloatArray(n * 2)
    for (i in 0 until n) {
        val a = i.toFloat() / n * 2f * PI.toFloat()
        val rr = radius + random.signed() * wobble
        pts[i * 2] = center.x + cos(a) * rr
        pts[i * 2 + 1] = center.y + sin(a) * rr
    }
    return closedSmoothPath(pts)
}

private const val DEGREES_PER_SECOND = 36f
