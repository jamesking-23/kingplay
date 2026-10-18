package com.kingplay.desktop.settings

import com.kingplay.core.model.BuiltInEqPresets
import com.kingplay.core.model.EqPreset

fun clampGain(db: Float): Float = db.coerceIn(EqPreset.MIN_GAIN_DB, EqPreset.MAX_GAIN_DB)

/** Same gains with one band changed; an out-of-range band index changes nothing. */
fun DesktopSettings.withBand(band: Int, db: Float): DesktopSettings {
    if (band !in eqGainsDb.indices) return this
    return copy(eqGainsDb = eqGainsDb.toMutableList().apply { this[band] = clampGain(db) })
}

fun DesktopSettings.withPreamp(db: Float): DesktopSettings = copy(eqPreampDb = clampGain(db))

fun DesktopSettings.withPreset(preset: EqPreset): DesktopSettings =
    copy(eqPreampDb = clampGain(preset.preampDb), eqGainsDb = preset.bandGainsDb.map(::clampGain))

fun DesktopSettings.flat(): DesktopSettings = withPreset(EqPreset.Flat)

/** The built-in preset these settings match exactly, or null when the person has moved a slider ("Custom"). */
fun DesktopSettings.matchingPreset(): EqPreset? = BuiltInEqPresets.all.firstOrNull {
    it.preampDb == eqPreampDb && it.bandGainsDb == eqGainsDb
}
