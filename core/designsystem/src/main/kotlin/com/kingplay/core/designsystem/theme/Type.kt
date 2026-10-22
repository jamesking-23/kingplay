package com.kingplay.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

/** KingPlay type scale: Material 3 scale with bolder display/headline and tighter titles. */
val KingPlayTypography = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
    bodySmall = base.bodySmall,
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium,
    labelSmall = base.labelSmall,
)

private const val READABLE_GROWTH = 1.1f
private const val MIN_TEXT_SP = 13f

/** Every size 10% bigger, and nothing smaller than 13sp, so small print is never too small by default. */
fun Typography.readable(): Typography {
    fun s(style: TextStyle): TextStyle {
        if (!style.fontSize.isSp) return style
        val size = maxOf(style.fontSize.value * READABLE_GROWTH, MIN_TEXT_SP)
        val line = if (style.lineHeight.isSp) maxOf(style.lineHeight.value * size / style.fontSize.value, size * 1.2f) else style.lineHeight.value
        return style.copy(
            fontSize = size.sp,
            lineHeight = if (style.lineHeight.isSp) line.sp else style.lineHeight,
        )
    }
    return copy(
        displayLarge = s(displayLarge), displayMedium = s(displayMedium), displaySmall = s(displaySmall),
        headlineLarge = s(headlineLarge), headlineMedium = s(headlineMedium), headlineSmall = s(headlineSmall),
        titleLarge = s(titleLarge), titleMedium = s(titleMedium), titleSmall = s(titleSmall),
        bodyLarge = s(bodyLarge), bodyMedium = s(bodyMedium), bodySmall = s(bodySmall),
        labelLarge = s(labelLarge), labelMedium = s(labelMedium), labelSmall = s(labelSmall),
    )
}
