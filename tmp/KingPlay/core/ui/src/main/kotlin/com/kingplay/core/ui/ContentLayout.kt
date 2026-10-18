package com.kingplay.core.ui

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

/** Adaptive layout buckets for phones, foldables and tablets. */
enum class ContentLayout { COMPACT, MEDIUM, EXPANDED }

fun WindowSizeClass.toContentLayout(): ContentLayout = when (widthSizeClass) {
    WindowWidthSizeClass.Compact -> ContentLayout.COMPACT
    WindowWidthSizeClass.Medium -> ContentLayout.MEDIUM
    else -> ContentLayout.EXPANDED
}
