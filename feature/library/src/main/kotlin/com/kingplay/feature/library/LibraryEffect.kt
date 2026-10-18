package com.kingplay.feature.library

import androidx.annotation.StringRes
import com.kingplay.core.ui.UiEffect

/** One-shot feedback the screen shows as a snackbar. */
sealed interface LibraryEffect : UiEffect {
    data class Message(@StringRes val textRes: Int) : LibraryEffect
}
