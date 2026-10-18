package com.kingplay.feature.equalizer

import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.AudioSettings
import com.kingplay.core.model.EqPreset
import com.kingplay.core.model.OutputDevice
import com.kingplay.core.model.ReplayGainMode
import com.kingplay.core.model.repository.AudioSettingsRepository
import com.kingplay.core.model.repository.EqRepository
import com.kingplay.core.ui.MviViewModel
import com.kingplay.core.ui.NoEffect
import com.kingplay.core.ui.UiEvent
import com.kingplay.core.ui.UiState
import com.kingplay.feature.equalizer.logic.snapDb
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class EqualizerState(
    val settings: AudioSettings = AudioSettings.Default,
    val presets: List<EqPreset> = emptyList(),
    val output: OutputDevice? = null,
    val hasProfile: Boolean = false,
) : UiState

sealed interface EqualizerEvent : UiEvent {
    data class SetEnabled(val enabled: Boolean) : EqualizerEvent

    data class SetPreamp(val db: Float) : EqualizerEvent

    data class SetBand(val index: Int, val db: Float) : EqualizerEvent

    data class ApplyPreset(val preset: EqPreset) : EqualizerEvent

    data object ResetCurve : EqualizerEvent

    data class SetBass(val strength: Int) : EqualizerEvent

    data class SetLoudness(val millibels: Int) : EqualizerEvent

    data class SetTempo(val tempo: Float) : EqualizerEvent

    data class SetPitch(val semitones: Float) : EqualizerEvent

    data class SetReplayGain(val mode: ReplayGainMode) : EqualizerEvent

    data class SetReplayGainPreamp(val db: Float) : EqualizerEvent

    data class SetFade(val ms: Int) : EqualizerEvent

    data class SetHeadroom(val enabled: Boolean) : EqualizerEvent

    data class SetRememberForDevice(val remember: Boolean) : EqualizerEvent
}

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val audio: AudioSettingsRepository,
    eq: EqRepository,
) : MviViewModel<EqualizerState, EqualizerEvent, NoEffect>(EqualizerState()) {

    init {
        combine(audio.settings, eq.presets(), audio.output, audio.hasProfileForOutput) { settings, presets, out, has ->
            EqualizerState(settings, presets, out, has)
        }.onEach { next -> setState { next } }.launchIn(viewModelScope)
    }

    override fun onEvent(event: EqualizerEvent) {
        when (event) {
            is EqualizerEvent.SetEnabled -> audio.update { it.copy(eqEnabled = event.enabled) }
            is EqualizerEvent.SetPreamp -> audio.update { it.copy(preampDb = snapDb(event.db)) }
            is EqualizerEvent.SetBand -> audio.update { current ->
                val bands = current.bandGainsDb.toMutableList()
                if (event.index in bands.indices) bands[event.index] = snapDb(event.db)
                current.copy(bandGainsDb = bands)
            }
            is EqualizerEvent.ApplyPreset -> audio.applyPreset(event.preset)
            EqualizerEvent.ResetCurve -> audio.applyPreset(EqPreset.Flat)
            is EqualizerEvent.SetBass -> audio.update {
                it.copy(effects = it.effects.copy(bassBoostStrength = event.strength))
            }
            is EqualizerEvent.SetLoudness -> audio.update {
                it.copy(effects = it.effects.copy(loudnessGainMb = event.millibels))
            }
            is EqualizerEvent.SetTempo -> audio.update {
                it.copy(effects = it.effects.copy(tempo = event.tempo))
            }
            is EqualizerEvent.SetPitch -> audio.update {
                it.copy(effects = it.effects.copy(pitchSemitones = event.semitones))
            }
            is EqualizerEvent.SetReplayGain -> audio.update { it.copy(replayGainMode = event.mode) }
            is EqualizerEvent.SetReplayGainPreamp -> audio.update { it.copy(replayGainPreampDb = snapDb(event.db)) }
            is EqualizerEvent.SetFade -> audio.update { it.copy(fadeMs = event.ms) }
            is EqualizerEvent.SetHeadroom -> audio.update { it.copy(autoHeadroom = event.enabled) }
            is EqualizerEvent.SetRememberForDevice -> viewModelScope.launch {
                if (event.remember) audio.saveProfileForOutput() else audio.removeProfileForOutput()
            }
        }
    }
}
