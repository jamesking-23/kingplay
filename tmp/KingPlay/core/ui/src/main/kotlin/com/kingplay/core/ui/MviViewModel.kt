package com.kingplay.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

interface UiState

interface UiEvent

interface UiEffect

/** For view models that never need to tell the screen anything one-shot. */
data object NoEffect : UiEffect

/**
 * Unidirectional data flow base: UI sends [UiEvent]s, the ViewModel reduces them into [UiState]
 * and emits one-shot [UiEffect]s (navigation, snackbars) that are delivered exactly once.
 */
abstract class MviViewModel<S : UiState, E : UiEvent, F : UiEffect>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<F>(Channel.BUFFERED)
    val effects: Flow<F> = _effects.receiveAsFlow()

    abstract fun onEvent(event: E)

    protected fun setState(reducer: S.() -> S) {
        _state.value = _state.value.reducer()
    }

    protected fun sendEffect(effect: F) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
