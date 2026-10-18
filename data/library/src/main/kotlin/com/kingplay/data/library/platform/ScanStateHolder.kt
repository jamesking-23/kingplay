package com.kingplay.data.library.platform

import com.kingplay.core.model.ScanState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-wide scan progress. The worker writes it, the scanner exposes it. */
@Singleton
class ScanStateHolder @Inject constructor() {
    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    fun set(state: ScanState) {
        _state.value = state
    }
}
