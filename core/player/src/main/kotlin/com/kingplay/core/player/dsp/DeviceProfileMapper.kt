package com.kingplay.core.player.dsp

import com.kingplay.core.model.AudioSettings
import com.kingplay.core.model.DeviceProfile
import com.kingplay.core.model.EqPreset
import com.kingplay.core.model.OutputDevice
import com.kingplay.core.model.sanitized

/**
 * A profile stores a preset id, effects and the ReplayGain mode. [presetId] must be a saved preset (built-in or user);
 * null means the EQ is off for this device.
 */
fun profileFrom(settings: AudioSettings, device: OutputDevice, presetId: Long?): DeviceProfile = DeviceProfile(
    deviceKey = device.key,
    deviceType = device.type,
    displayName = device.displayName,
    eqPresetId = if (settings.eqEnabled) presetId else null,
    effects = settings.effects,
    replayGainMode = settings.replayGainMode,
)

/**
 * Settings to use while [profile] is active. Things a profile does not store (fade, headroom, ReplayGain preamp)
 * keep their current values from [base].
 */
fun settingsFrom(profile: DeviceProfile, preset: EqPreset?, base: AudioSettings): AudioSettings = base.copy(
    eqEnabled = preset != null,
    preampDb = preset?.preampDb ?: 0f,
    bandGainsDb = preset?.bandGainsDb ?: EqPreset.Flat.bandGainsDb,
    presetId = preset?.id,
    effects = profile.effects,
    replayGainMode = profile.replayGainMode,
).sanitized()
