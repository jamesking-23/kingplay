package com.kingplay.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object Motion {
    const val DurationShort = 150
    const val DurationMedium = 300
    const val DurationLong = 500

    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Springy movement for position/size changes. */
    fun <T> spatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Timed fades and color changes. */
    fun <T> effects(durationMillis: Int = DurationMedium): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = Emphasized)
}
