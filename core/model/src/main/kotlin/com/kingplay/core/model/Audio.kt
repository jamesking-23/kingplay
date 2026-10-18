package com.kingplay.core.model

enum class ReplayGainMode { OFF, TRACK, ALBUM }

enum class OutputDeviceType { SPEAKER, WIRED, BLUETOOTH, USB_DAC, OTHER }

data class EqPreset(
    val id: Long,
    val name: String,
    val preampDb: Float,
    val bandGainsDb: List<Float>,
    val isBuiltIn: Boolean,
) {
    init {
        require(bandGainsDb.size == BAND_COUNT) { "EQ preset needs exactly $BAND_COUNT bands" }
    }

    companion object {
        const val BAND_COUNT = 10
        const val MIN_GAIN_DB = -12f
        const val MAX_GAIN_DB = 12f
        val CENTER_FREQUENCIES_HZ = listOf(31, 62, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 16_000)

        val Flat = EqPreset(
            id = 0,
            name = "Flat",
            preampDb = 0f,
            bandGainsDb = List(BAND_COUNT) { 0f },
            isBuiltIn = true,
        )
    }
}

/** Non-EQ effect settings. Ranges follow the platform effect conventions. */
data class EffectSettings(
    val bassBoostStrength: Int = 0, // 0..1000
    val loudnessGainMb: Int = 0, // millibels
    val tempo: Float = 1f, // 0.5..2.0
    val pitchSemitones: Float = 0f, // -12..12
)

/** Settings automatically restored when an output device becomes active. */
data class DeviceProfile(
    val deviceKey: String,
    val deviceType: OutputDeviceType,
    val displayName: String,
    val eqPresetId: Long?,
    val effects: EffectSettings = EffectSettings(),
    val replayGainMode: ReplayGainMode = ReplayGainMode.OFF,
)
