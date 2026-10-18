package com.kingplay.core.player.dsp

import com.kingplay.core.model.AudioSettings
import com.kingplay.core.model.EffectSettings
import com.kingplay.core.model.EqPreset
import com.kingplay.core.model.ReplayGainMode
import com.kingplay.core.model.sanitized

/**
 * Settings as a flat string map for SharedPreferences. Decoding never fails: missing or damaged values fall back
 * to defaults, and everything is clamped into range afterwards.
 */
object AudioSettingsCodec {
    private const val EQ_ENABLED = "eq_enabled"
    private const val PREAMP = "preamp_db"
    private const val BANDS = "band_gains_db"
    private const val PRESET = "preset_id"
    private const val BASS = "bass_boost"
    private const val LOUDNESS = "loudness_mb"
    private const val TEMPO = "tempo"
    private const val PITCH = "pitch_semitones"
    private const val RG_MODE = "replay_gain_mode"
    private const val RG_PREAMP = "replay_gain_preamp_db"
    private const val HEADROOM = "auto_headroom"
    private const val FADE = "fade_ms"

    fun encode(settings: AudioSettings): Map<String, String> = mapOf(
        EQ_ENABLED to settings.eqEnabled.toString(),
        PREAMP to settings.preampDb.toString(),
        BANDS to settings.bandGainsDb.joinToString(","),
        PRESET to (settings.presetId?.toString() ?: ""),
        BASS to settings.effects.bassBoostStrength.toString(),
        LOUDNESS to settings.effects.loudnessGainMb.toString(),
        TEMPO to settings.effects.tempo.toString(),
        PITCH to settings.effects.pitchSemitones.toString(),
        RG_MODE to settings.replayGainMode.name,
        RG_PREAMP to settings.replayGainPreampDb.toString(),
        HEADROOM to settings.autoHeadroom.toString(),
        FADE to settings.fadeMs.toString(),
    )

    private fun finite(text: String?): Float? = text?.trim()?.toFloatOrNull()?.takeIf { it.isFinite() }

    fun decode(values: Map<String, String>): AudioSettings {
        val defaults = AudioSettings.Default
        val bands = values[BANDS]
            ?.split(',')
            ?.mapNotNull { finite(it) }
            ?.takeIf { it.size == EqPreset.BAND_COUNT }
            ?: defaults.bandGainsDb
        return AudioSettings(
            eqEnabled = values[EQ_ENABLED]?.toBooleanStrictOrNull() ?: defaults.eqEnabled,
            preampDb = finite(values[PREAMP]) ?: defaults.preampDb,
            bandGainsDb = bands,
            presetId = if (values.containsKey(PRESET)) values[PRESET]?.toLongOrNull() else defaults.presetId,
            effects = EffectSettings(
                bassBoostStrength = values[BASS]?.toIntOrNull() ?: 0,
                loudnessGainMb = values[LOUDNESS]?.toIntOrNull() ?: 0,
                tempo = finite(values[TEMPO]) ?: 1f,
                pitchSemitones = finite(values[PITCH]) ?: 0f,
            ),
            replayGainMode = values[RG_MODE]?.let { name -> ReplayGainMode.entries.firstOrNull { it.name == name } }
                ?: defaults.replayGainMode,
            replayGainPreampDb = finite(values[RG_PREAMP]) ?: defaults.replayGainPreampDb,
            autoHeadroom = values[HEADROOM]?.toBooleanStrictOrNull() ?: defaults.autoHeadroom,
            fadeMs = values[FADE]?.toIntOrNull() ?: defaults.fadeMs,
        ).sanitized()
    }
}
