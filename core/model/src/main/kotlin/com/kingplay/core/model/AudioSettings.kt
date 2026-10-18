package com.kingplay.core.model

/**
 * Everything the audio engine needs to know. One immutable snapshot; the engine swaps snapshots atomically, so the
 * audio thread never sees a half-applied change.
 */
data class AudioSettings(
    val eqEnabled: Boolean = false,
    val preampDb: Float = 0f,
    val bandGainsDb: List<Float> = EqPreset.Flat.bandGainsDb,
    /** The preset the curve came from, or null once the user has moved a slider ("Custom"). */
    val presetId: Long? = EqPreset.Flat.id,
    val effects: EffectSettings = EffectSettings(),
    val replayGainMode: ReplayGainMode = ReplayGainMode.OFF,
    /** Extra gain on top of ReplayGain, for people who find the standard -18 LUFS target too quiet. */
    val replayGainPreampDb: Float = 0f,
    /** Lowers the overall level by the EQ's biggest boost so the curve cannot clip. */
    val autoHeadroom: Boolean = true,
    /** Fade out at the end and in at the start of each song. 0 means gapless, no fade. */
    val fadeMs: Int = 0,
) {
    init {
        require(bandGainsDb.size == EqPreset.BAND_COUNT) { "EQ needs exactly ${EqPreset.BAND_COUNT} bands" }
    }

    companion object {
        const val MAX_FADE_MS = 12_000
        const val MAX_BASS_BOOST = 1_000
        const val MAX_LOUDNESS_MB = 2_000
        const val MIN_TEMPO = 0.5f
        const val MAX_TEMPO = 2.0f
        const val MAX_PITCH_SEMITONES = 12f
        const val MAX_PREAMP_DB = 12f
        val Default = AudioSettings()
    }
}

/** An audio output the phone can play through. [key] is stable across reconnects, so profiles can be matched. */
data class OutputDevice(
    val key: String,
    val type: OutputDeviceType,
    val displayName: String,
)

/** Clamps every value into its allowed range. Used on anything read from disk or typed by the user. */
fun AudioSettings.sanitized(): AudioSettings = copy(
    preampDb = preampDb.coerceIn(-AudioSettings.MAX_PREAMP_DB, AudioSettings.MAX_PREAMP_DB),
    bandGainsDb = bandGainsDb.map { it.coerceIn(EqPreset.MIN_GAIN_DB, EqPreset.MAX_GAIN_DB) },
    effects = effects.copy(
        bassBoostStrength = effects.bassBoostStrength.coerceIn(0, AudioSettings.MAX_BASS_BOOST),
        loudnessGainMb = effects.loudnessGainMb.coerceIn(0, AudioSettings.MAX_LOUDNESS_MB),
        tempo = effects.tempo.coerceIn(AudioSettings.MIN_TEMPO, AudioSettings.MAX_TEMPO),
        pitchSemitones = effects.pitchSemitones.coerceIn(
            -AudioSettings.MAX_PITCH_SEMITONES,
            AudioSettings.MAX_PITCH_SEMITONES,
        ),
    ),
    replayGainPreampDb = replayGainPreampDb.coerceIn(-AudioSettings.MAX_PREAMP_DB, AudioSettings.MAX_PREAMP_DB),
    fadeMs = fadeMs.coerceIn(0, AudioSettings.MAX_FADE_MS),
)
