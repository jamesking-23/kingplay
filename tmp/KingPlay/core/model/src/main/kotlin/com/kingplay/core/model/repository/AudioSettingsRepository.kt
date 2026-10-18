package com.kingplay.core.model.repository

import com.kingplay.core.model.AudioSettings
import com.kingplay.core.model.EqPreset
import com.kingplay.core.model.OutputDevice
import kotlinx.coroutines.flow.StateFlow

/**
 * The live audio settings, shared by the equalizer screen and the playback service. Changes apply to playback at
 * once and are saved. When the current output device has a saved profile, its settings are applied automatically
 * and edits update that profile.
 */
interface AudioSettingsRepository {
    val settings: StateFlow<AudioSettings>

    /** The output the audio is going to, or null while it is not known yet. */
    val output: StateFlow<OutputDevice?>

    /** True when [settings] came from a saved profile for [output]. */
    val hasProfileForOutput: StateFlow<Boolean>

    fun update(transform: (AudioSettings) -> AudioSettings)

    /** Applies a preset's curve and preamp, and remembers which preset it was. */
    fun applyPreset(preset: EqPreset)

    /** Saves the current settings for the current output so they come back whenever it is connected. */
    suspend fun saveProfileForOutput()

    suspend fun removeProfileForOutput()
}
