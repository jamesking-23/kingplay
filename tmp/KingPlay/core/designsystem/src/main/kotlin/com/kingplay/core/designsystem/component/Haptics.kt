package com.kingplay.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Wraps [onClick] so the tap gives light haptic feedback. */
@Composable
fun rememberHapticClick(onClick: () -> Unit): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return remember(onClick, haptics) {
        {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    }
}
