package com.kingplay.desktop.ui

import com.kingplay.core.model.settings.AppAccent
import com.kingplay.core.model.settings.AppThemeMode

/** The accent colour as 0xAARRGGBB: a lighter tone on dark backgrounds, a deeper one on light ones. */
fun accentArgb(accent: AppAccent, dark: Boolean): Long = when (accent) {
    AppAccent.VIOLET -> if (dark) 0xFFB4A7FF else 0xFF5B4BD6
    AppAccent.OCEAN -> if (dark) 0xFF7FC8FF else 0xFF0B6BA8
    AppAccent.EMBER -> if (dark) 0xFFFFB27A else 0xFFB84A00
    AppAccent.EMERALD -> if (dark) 0xFF6FDDB0 else 0xFF00805A
}

/** Whether to draw dark: the person's choice, or the Windows setting for "System". */
fun isDark(mode: AppThemeMode, systemDark: Boolean): Boolean = when (mode) {
    AppThemeMode.SYSTEM -> systemDark
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK, AppThemeMode.AMOLED -> true
}
