package com.kingplay.feature.settings

import androidx.lifecycle.viewModelScope
import com.kingplay.core.common.ConsentGate
import com.kingplay.core.model.ScanState
import com.kingplay.core.model.backup.RestoreResult
import com.kingplay.core.model.backup.RestoreSummary
import com.kingplay.core.model.repository.BackupRepository
import com.kingplay.core.model.repository.LibraryFolderSettings
import com.kingplay.core.model.repository.LibraryScanner
import com.kingplay.core.model.repository.SettingsRepository
import com.kingplay.core.model.settings.AppAccent
import com.kingplay.core.model.settings.AppFont
import com.kingplay.core.model.settings.AppSettings
import com.kingplay.core.model.settings.HapticSettings
import com.kingplay.core.model.settings.AppThemeMode
import com.kingplay.core.model.settings.AppUiTheme
import com.kingplay.core.ui.MviViewModel
import com.kingplay.core.ui.NoEffect
import com.kingplay.core.ui.UiEvent
import com.kingplay.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed interface BackupStatus {
    data object Idle : BackupStatus

    data object Working : BackupStatus

    data object Exported : BackupStatus

    data object ExportFailed : BackupStatus

    data class Restored(val summary: RestoreSummary) : BackupStatus

    /** The picked file is not a usable backup. [reason] is already written for the user. */
    data class Invalid(val reason: String) : BackupStatus

    data object RestoreFailed : BackupStatus
}

data class SettingsState(
    val settings: AppSettings = AppSettings(),
    val analyticsConsent: Boolean = false,
    val scan: ScanState = ScanState.Idle,
    val excludedFolders: List<String> = emptyList(),
    val backup: BackupStatus = BackupStatus.Idle,
) : UiState

sealed interface SettingsEvent : UiEvent {
    data class SetThemeMode(val mode: AppThemeMode) : SettingsEvent

    data class SetUiTheme(val uiTheme: AppUiTheme) : SettingsEvent

    data class SetAccent(val accent: AppAccent) : SettingsEvent

    data class SetDynamicColor(val enabled: Boolean) : SettingsEvent

    data class SetLowPower(val enabled: Boolean) : SettingsEvent

    data class SetFont(val font: AppFont) : SettingsEvent

    data class SetCoverTheme(val enabled: Boolean) : SettingsEvent

    data class SetTextScale(val scale: Float) : SettingsEvent

    data class SetCustomAccent(val argb: Long?) : SettingsEvent

    data class SetHaptics(val transform: (HapticSettings) -> HapticSettings) : SettingsEvent

    data class SetAnalytics(val granted: Boolean) : SettingsEvent

    data class Rescan(val full: Boolean) : SettingsEvent

    data class IncludeFolder(val path: String) : SettingsEvent

    /** [save] writes the backup text where the user chose and says whether it worked. */
    data class Export(val save: suspend (String) -> Boolean) : SettingsEvent

    /** [read] returns the text of the file the user chose, or null if it could not be read. */
    data class Import(val read: suspend () -> String?) : SettingsEvent

    data object DismissBackupStatus : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val consent: ConsentGate,
    private val scanner: LibraryScanner,
    private val folders: LibraryFolderSettings,
    private val backups: BackupRepository,
) : MviViewModel<SettingsState, SettingsEvent, NoEffect>(SettingsState()) {

    init {
        settingsRepository.settings.onEach { next -> setState { copy(settings = next) } }.launchIn(viewModelScope)
        consent.analyticsConsent.onEach { next -> setState { copy(analyticsConsent = next) } }.launchIn(viewModelScope)
        scanner.state.onEach { next -> setState { copy(scan = next) } }.launchIn(viewModelScope)
        folders.excludedFolders.onEach { next -> setState { copy(excludedFolders = next) } }.launchIn(viewModelScope)
    }

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetThemeMode -> update { it.copy(themeMode = event.mode) }
            is SettingsEvent.SetUiTheme -> update { it.copy(uiTheme = event.uiTheme) }
            is SettingsEvent.SetAccent -> update { it.copy(accent = event.accent) }
            is SettingsEvent.SetDynamicColor -> update { it.copy(dynamicColor = event.enabled) }
            is SettingsEvent.SetLowPower -> update { it.copy(lowPowerVisuals = event.enabled) }
            is SettingsEvent.SetFont -> update { it.copy(font = event.font) }
            is SettingsEvent.SetCoverTheme -> update { it.copy(coverTheme = event.enabled) }
            is SettingsEvent.SetTextScale -> update {
                it.copy(textScale = event.scale.coerceIn(AppSettings.MIN_TEXT_SCALE, AppSettings.MAX_TEXT_SCALE))
            }
            is SettingsEvent.SetCustomAccent -> update { it.copy(customAccent = event.argb) }
            is SettingsEvent.SetHaptics -> update { it.copy(haptics = event.transform(it.haptics)) }
            is SettingsEvent.SetAnalytics -> viewModelScope.launch { consent.setAnalyticsConsent(event.granted) }
            is SettingsEvent.Rescan -> scanner.requestScan(event.full)
            is SettingsEvent.IncludeFolder -> viewModelScope.launch { folders.include(event.path) }
            is SettingsEvent.Export -> export(event.save)
            is SettingsEvent.Import -> import(event.read)
            SettingsEvent.DismissBackupStatus -> setState { copy(backup = BackupStatus.Idle) }
        }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    private fun export(save: suspend (String) -> Boolean) {
        setState { copy(backup = BackupStatus.Working) }
        viewModelScope.launch {
            val ok = try {
                save(backups.create())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
            setState { copy(backup = if (ok) BackupStatus.Exported else BackupStatus.ExportFailed) }
        }
    }

    private fun import(read: suspend () -> String?) {
        setState { copy(backup = BackupStatus.Working) }
        viewModelScope.launch {
            val status = try {
                val text = read()
                if (text == null) {
                    BackupStatus.RestoreFailed
                } else {
                    when (val result = backups.restore(text)) {
                        is RestoreResult.Done -> BackupStatus.Restored(result.summary)
                        is RestoreResult.Invalid -> BackupStatus.Invalid(result.reason)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                BackupStatus.RestoreFailed
            }
            setState { copy(backup = status) }
        }
    }
}
