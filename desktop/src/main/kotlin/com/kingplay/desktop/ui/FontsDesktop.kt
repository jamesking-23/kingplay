package com.kingplay.desktop.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp
import com.kingplay.core.model.settings.AppFont

/*
 * The same open-licence fonts as on the phone, bundled with the program (SIL Open Font License), so a font never
 * needs the internet.
 */

private fun family(vararg faces: Pair<String, FontWeight>) = FontFamily(faces.map { (file, weight) -> Font("fonts/$file", weight) })

private val Poppins by lazy {
    family(
        "poppins_regular.ttf" to FontWeight.Normal, "poppins_medium.ttf" to FontWeight.Medium,
        "poppins_medium.ttf" to FontWeight.SemiBold, "poppins_bold.ttf" to FontWeight.Bold,
    )
}
private val Hanken by lazy {
    family(
        "hanken_regular.otf" to FontWeight.Normal, "hanken_semibold.otf" to FontWeight.Medium,
        "hanken_semibold.otf" to FontWeight.SemiBold, "hanken_bold.otf" to FontWeight.Bold,
    )
}
private val SourceSans by lazy {
    family(
        "sourcesans_regular.ttf" to FontWeight.Normal, "sourcesans_semibold.ttf" to FontWeight.Medium,
        "sourcesans_semibold.ttf" to FontWeight.SemiBold, "sourcesans_bold.ttf" to FontWeight.Bold,
    )
}
private val SourceSerif by lazy {
    family(
        "sourceserif_regular.ttf" to FontWeight.Normal, "sourceserif_regular.ttf" to FontWeight.Medium,
        "sourceserif_bold.ttf" to FontWeight.SemiBold, "sourceserif_bold.ttf" to FontWeight.Bold,
    )
}
private val Lora by lazy { family("lora_variable.ttf" to FontWeight.Normal) }
private val Fredoka by lazy { family("fredoka_regular.ttf" to FontWeight.Normal) }
private val Handwritten by lazy {
    family(
        "caveat_semibold.ttf" to FontWeight.Normal, "caveat_semibold.ttf" to FontWeight.Medium,
        "caveat_semibold.ttf" to FontWeight.SemiBold, "caveat_bold.ttf" to FontWeight.Bold,
    )
}
private val Sketch by lazy { family("architects_daughter.ttf" to FontWeight.Normal) }

private fun AppFont.family(): FontFamily? = when (this) {
    AppFont.DEFAULT -> null
    AppFont.POPPINS -> Poppins
    AppFont.HANKEN, AppFont.READABLE -> Hanken
    AppFont.SOURCE_SANS -> SourceSans
    AppFont.SOURCE_SERIF -> SourceSerif
    AppFont.LORA -> Lora
    AppFont.FREDOKA -> Fredoka
    AppFont.HANDWRITTEN -> Handwritten
    AppFont.SKETCH -> Sketch
    AppFont.MONO -> FontFamily.Monospace
}

private const val READABLE_TRACKING = 0.4f
private const val READABLE_LINE_GROWTH = 1.2f

/** [this] with every text style in the chosen font; [AppFont.DEFAULT] returns it unchanged. */
fun Typography.withFont(font: AppFont): Typography {
    val fam = font.family() ?: return this
    val roomy = font == AppFont.READABLE
    fun s(style: TextStyle) = style.copy(
        fontFamily = fam,
        letterSpacing = if (roomy) (style.letterSpacing.value.takeIf { !it.isNaN() } ?: 0f).plus(READABLE_TRACKING).sp else style.letterSpacing,
        lineHeight = if (roomy && style.lineHeight.isSp) (style.lineHeight.value * READABLE_LINE_GROWTH).sp else style.lineHeight,
    )
    return copy(
        displayLarge = s(displayLarge), displayMedium = s(displayMedium), displaySmall = s(displaySmall),
        headlineLarge = s(headlineLarge), headlineMedium = s(headlineMedium), headlineSmall = s(headlineSmall),
        titleLarge = s(titleLarge), titleMedium = s(titleMedium), titleSmall = s(titleSmall),
        bodyLarge = s(bodyLarge), bodyMedium = s(bodyMedium), bodySmall = s(bodySmall),
        labelLarge = s(labelLarge), labelMedium = s(labelMedium), labelSmall = s(labelSmall),
    )
}

fun fontLabel(font: AppFont): String = when (font) {
    AppFont.DEFAULT -> "Design default"
    AppFont.POPPINS -> "Poppins"
    AppFont.HANKEN -> "Hanken Grotesk"
    AppFont.SOURCE_SANS -> "Source Sans"
    AppFont.SOURCE_SERIF -> "Source Serif"
    AppFont.LORA -> "Lora"
    AppFont.FREDOKA -> "Fredoka"
    AppFont.HANDWRITTEN -> "Handwritten"
    AppFont.SKETCH -> "Sketch"
    AppFont.MONO -> "Monospace"
    AppFont.READABLE -> "Easy reading"
}
