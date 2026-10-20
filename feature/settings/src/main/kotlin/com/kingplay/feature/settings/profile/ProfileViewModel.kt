package com.kingplay.feature.settings.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.repository.PlaybackRecordsData
import com.kingplay.core.model.repository.PlaybackRecordsRepository
import com.kingplay.core.model.repository.SettingsRepository
import com.kingplay.core.model.settings.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileState(
    val nickname: String = "",
    val photoPath: String? = null,
    val records: PlaybackRecordsData = PlaybackRecordsData(),
    val loaded: Boolean = false,
)

/** Shared by the first-run welcome and the profile page. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settings: SettingsRepository,
    records: PlaybackRecordsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val state: StateFlow<ProfileState> = combine(settings.settings, records.records()) { s, r ->
        ProfileState(nickname = s.nickname, photoPath = s.profilePhoto, records = r, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), ProfileState())

    fun saveNickname(name: String) {
        val clean = name.trim().take(AppSettings.MAX_NICKNAME)
        if (clean.isEmpty()) return
        viewModelScope.launch { settings.update { it.copy(nickname = clean) } }
    }

    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            val path = ProfilePhotos.save(context, uri) ?: return@launch
            settings.update { it.copy(profilePhoto = path) }
        }
    }

    fun removePhoto() {
        viewModelScope.launch {
            settings.update { it.copy(profilePhoto = null) }
            ProfilePhotos.delete(context)
        }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
