package com.kingplay.feature.library.home

import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.ScanState
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.LibraryScanner
import com.kingplay.core.ui.MviViewModel
import com.kingplay.core.ui.UiEvent
import com.kingplay.core.ui.UiState
import com.kingplay.feature.library.LibraryEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class LibraryHomeState(
    val scan: ScanState = ScanState.Idle,
    val songCount: Int = 0,
) : UiState

sealed interface LibraryHomeEvent : UiEvent {
    data class Rescan(val full: Boolean) : LibraryHomeEvent
}

@HiltViewModel
class LibraryHomeViewModel @Inject constructor(
    library: LibraryRepository,
    private val scanner: LibraryScanner,
) : MviViewModel<LibraryHomeState, LibraryHomeEvent, LibraryEffect>(LibraryHomeState()) {

    init {
        scanner.state.onEach { scan -> setState { copy(scan = scan) } }.launchIn(viewModelScope)
        library.songCount().onEach { count -> setState { copy(songCount = count) } }.launchIn(viewModelScope)
    }

    override fun onEvent(event: LibraryHomeEvent) {
        when (event) {
            is LibraryHomeEvent.Rescan -> scanner.requestScan(event.full)
        }
    }
}
