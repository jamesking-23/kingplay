package com.kingplay.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class LayoutDensity { COMFORTABLE, COMPACT }

@Immutable
data class Spacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
)

val ComfortableSpacing = Spacing()

val CompactSpacing = Spacing(
    xxs = 1.dp,
    xs = 3.dp,
    sm = 6.dp,
    md = 9.dp,
    lg = 12.dp,
    xl = 18.dp,
    xxl = 24.dp,
    xxxl = 36.dp,
)

val LocalSpacing = staticCompositionLocalOf { ComfortableSpacing }

/** Visuals-heavy effects (shimmer, blur, particles) check this to save battery and RAM. */
val LocalLowPowerVisuals = staticCompositionLocalOf { false }
