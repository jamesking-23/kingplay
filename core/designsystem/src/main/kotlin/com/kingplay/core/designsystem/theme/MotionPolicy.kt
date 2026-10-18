package com.kingplay.core.designsystem.theme

/**
 * Decorative motion (visualizer, lyric glow, shimmer) is reduced when the person chose low-power visuals
 * or turned system animations off.
 */
fun effectiveLowPower(setting: Boolean, animatorScale: Float): Boolean = setting || animatorScale <= 0f
