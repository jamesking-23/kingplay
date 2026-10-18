package com.kingplay.feature.library.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingplay.core.ui.Async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Loading / empty / error / content for a plain list screen. The source is a database flow, so it stays live. */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ListViewModel<T>(source: Flow<List<T>>) : ViewModel() {

    private val retryTick = MutableStateFlow(0)

    val state: StateFlow<Async<List<T>>> = retryTick
        .flatMapLatest {
            source
                .map { items -> if (items.isEmpty()) Async.Empty else Async.Content(items) }
                .catch { error -> emit(Async.Error(error.message)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), Async.Loading)

    fun retry() {
        retryTick.update { it + 1 }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
