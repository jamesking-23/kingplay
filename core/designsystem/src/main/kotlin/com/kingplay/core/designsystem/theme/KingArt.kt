package com.kingplay.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingplay.core.designsystem.R
import com.kingplay.core.model.theme.KingArtPalette
import com.kingplay.core.model.theme.Sketch

/* ---------- colours ---------- */

private val Paper = Color(KingArtPalette.PAPER)
private val Ink = Color(KingArtPalette.INK)
private val Yellow = Color(KingArtPalette.YELLOW)

/**
 * Pencil on paper (light) or chalk on charcoal (dark). The red is a touch deeper than the logo's so that red text and
 * icons keep readable contrast on paper.
 */
internal fun kingArtColorScheme(dark: Boolean): ColorScheme = if (dark) {
    val bg = Color(KingArtPalette.DARK_PAPER)
    val ink = Color(KingArtPalette.DARK_INK)
    val red = Color(KingArtPalette.DARK_RED)
    val container = Color(KingArtPalette.DARK_YELLOW_CONTAINER)
    darkColorScheme(
        primary = red,
        onPrimary = bg,
        primaryContainer = container,
        onPrimaryContainer = Yellow,
        secondary = ink,
        onSecondary = bg,
        secondaryContainer = Color(KingArtPalette.DARK_PAPER_SHADE_HIGH),
        onSecondaryContainer = ink,
        tertiary = Yellow,
        onTertiary = bg,
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
        onTertiary = Ink,
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

/* ---------- type: Caveat for headings, Architects Daughter for everything else ---------- */

private val Caveat = FontFamily(
    Font(R.font.caveat_semibold, FontWeight.Normal),
    Font(R.font.caveat_semibold, FontWeight.Medium),
    Font(R.font.caveat_semibold, FontWeight.SemiBold),
    Font(R.font.caveat_bold, FontWeight.Bold),
)

private val Architects = FontFamily(
    Font(R.font.architects_daughter, FontWeight.Normal),
    Font(R.font.architects_daughter, FontWeight.Medium),
)

private val base = Typography()

private fun TextStyle.heading(scale: Float, weight: FontWeight = FontWeight.Bold) = copy(
    fontFamily = Caveat,
    fontWeight = weight,
    fontSize = fontSize * scale,
    lineHeight = lineHeight * scale,
    letterSpacing = 0.sp,
)

private fun TextStyle.hand() = copy(fontFamily = Architects, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)

/** Headings are handwritten (and scaled up, because Caveat sets small); body text is a friendly print hand. */
val KingArtTypography = Typography(
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

/* ---------- shapes: outlines drawn by hand ---------- */

/**
 * A rounded rectangle whose edge wobbles like a pencil line. The wobble is seeded, so a given shape never
 * shimmers or jumps when the screen redraws.
 */
data class SketchShape(
    val corner: Dp,
    val seed: Int,
    val wobble: Dp = 1.1.dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val pts = with(density) {
            Sketch.roundRect(size.width, size.height, corner.toPx(), seed, wobble.toPx(), STEP.toPx())
        }
        return Outline.Generic(closedSmoothPath(pts))
    }

    private companion object {
        val STEP = 14.dp
    }
}

/** Joins the points with smooth curves through their midpoints, the way a hand rounds off a drawn corner. */
internal fun closedSmoothPath(pts: FloatArray): Path {
    val n = pts.size / 2
    val path = Path()
    if (n < 3) return path
    fun x(i: Int) = pts[(i % n) * 2]
    fun y(i: Int) = pts[(i % n) * 2 + 1]
    path.moveTo((x(n - 1) + x(0)) / 2f, (y(n - 1) + y(0)) / 2f)
    for (i in 0 until n) {
        path.quadraticTo(x(i), y(i), (x(i) + x(i + 1)) / 2f, (y(i) + y(i + 1)) / 2f)
    }
    path.close()
    return path
}

val KingArtShapes = Shapes(
    extraSmall = SketchShape(4.dp, seed = 11),
    small = SketchShape(10.dp, seed = 23),
    medium = SketchShape(16.dp, seed = 37),
    large = SketchShape(22.dp, seed = 41),
    extraLarge = SketchShape(30.dp, seed = 53),
)
