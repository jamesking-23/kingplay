package com.kingplay.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kingplay.core.designsystem.R
import com.kingplay.core.model.settings.AppFont

/*
 * Bundled fonts, all under the SIL Open Font License: Poppins, Hanken Grotesk, Source Sans, Source Serif, Lora,
 * Fredoka, Caveat and Architects Daughter. They ship inside the app, so a font never needs the internet.
 */

private val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_medium, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private val Hanken = FontFamily(
    Font(R.font.hanken_regular, FontWeight.Normal),
    Font(R.font.hanken_semibold, FontWeight.Medium),
    Font(R.font.hanken_semibold, FontWeight.SemiBold),
    Font(R.font.hanken_bold, FontWeight.Bold),
)

private val SourceSans = FontFamily(
    Font(R.font.sourcesans_regular, FontWeight.Normal),
    Font(R.font.sourcesans_semibold, FontWeight.Medium),
    Font(R.font.sourcesans_semibold, FontWeight.SemiBold),
    Font(R.font.sourcesans_bold, FontWeight.Bold),
)

private val SourceSerif = FontFamily(
    Font(R.font.sourceserif_regular, FontWeight.Normal),
    Font(R.font.sourceserif_regular, FontWeight.Medium),
    Font(R.font.sourceserif_bold, FontWeight.SemiBold),
    Font(R.font.sourceserif_bold, FontWeight.Bold),
)

private val Lora = FontFamily(
    Font(R.font.lora_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.lora_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.lora_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.lora_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

private val Fredoka = FontFamily(Font(R.font.fredoka_regular, FontWeight.Normal))

private val Handwritten = FontFamily(
    Font(R.font.caveat_semibold, FontWeight.Normal),
    Font(R.font.caveat_semibold, FontWeight.Medium),
    Font(R.font.caveat_semibold, FontWeight.SemiBold),
    Font(R.font.caveat_bold, FontWeight.Bold),
)

private val Sketch = FontFamily(Font(R.font.architects_daughter, FontWeight.Normal))

/** The family for a choice, or null for [AppFont.DEFAULT] (keep the design's own type). */
fun AppFont.family(): FontFamily? = when (this) {
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

private fun TextStyle.restyled(font: AppFont, family: FontFamily): TextStyle {
    val roomy = font == AppFont.READABLE
    return copy(
        fontFamily = family,
        letterSpacing = if (roomy) (letterSpacing.value.takeIf { !it.isNaN() } ?: 0f).plus(READABLE_TRACKING).sp else letterSpacing,
        lineHeight = if (roomy && lineHeight.isSp) (lineHeight.value * READABLE_LINE_GROWTH).sp else lineHeight,
    )
}

private const val READABLE_TRACKING = 0.4f
private const val READABLE_LINE_GROWTH = 1.2f

/** [this] with every text style set in the chosen font. [AppFont.DEFAULT] returns it unchanged. */
fun Typography.withFont(font: AppFont): Typography {
    val family = font.family() ?: return this
    fun s(style: TextStyle) = style.restyled(font, family)
    return copy(
        displayLarge = s(displayLarge), displayMedium = s(displayMedium), displaySmall = s(displaySmall),
        headlineLarge = s(headlineLarge), headlineMedium = s(headlineMedium), headlineSmall = s(headlineSmall),
        titleLarge = s(titleLarge), titleMedium = s(titleMedium), titleSmall = s(titleSmall),
        bodyLarge = s(bodyLarge), bodyMedium = s(bodyMedium), bodySmall = s(bodySmall),
        labelLarge = s(labelLarge), labelMedium = s(labelMedium), labelSmall = s(labelSmall),
    )
}
