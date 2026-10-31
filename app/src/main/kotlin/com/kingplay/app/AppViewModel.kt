package com.kingplay.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.repository.LibraryScanner
import com.kingplay.core.model.repository.SettingsRepository
import com.kingplay.core.model.settings.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-level state and actions that do not belong to any one screen. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val scanner: LibraryScanner,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** The saved settings, or null for the first instant while they load (so the wrong theme never flashes). */
    val settings: StateFlow<AppSettings?> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Remembers the version whose "What's new" has been dealt with. */
    fun markVersionSeen(tag: String) {
        viewModelScope.launch { settingsRepository.update { it.copy(lastSeenVersion = tag) } }
    }

    /** Media access was just granted: set up observers and scan now. Safe to call more than once. */
    fun onMediaAccessGranted() = scanner.start()
}
