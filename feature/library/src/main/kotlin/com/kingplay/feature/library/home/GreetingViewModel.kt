package com.kingplay.feature.library.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class GreetingState(val nickname: String = "", val photoPath: String? = null)

/** Who the home screen greets, taken from the profile. */
@HiltViewModel
class GreetingViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<GreetingState> = settings.settings
        .map { GreetingState(it.nickname, it.profilePhoto) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), GreetingState())

    private companion object {
        const val STOP_MS = 5_000L
    }
}
