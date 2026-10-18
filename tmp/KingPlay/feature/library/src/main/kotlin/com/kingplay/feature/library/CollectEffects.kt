package com.kingplay.feature.library

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.Flow

/** Shows each [LibraryEffect.Message] as a snackbar. */
@Composable
fun CollectEffects(effects: Flow<LibraryEffect>, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is LibraryEffect.Message -> snackbar.showSnackbar(context.getString(effect.textRes))
            }
        }
    }
}
