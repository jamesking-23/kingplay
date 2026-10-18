package com.kingplay.core.player.dsp

import com.kingplay.core.model.ReplayGain
import com.kingplay.core.model.ReplayGainMode
import java.util.Locale
import kotlin.math.log10
import kotlin.math.min

/**
 * ReplayGain adjustment in dB for one song.
 *
 * Track mode uses the track gain, album mode the album gain (each falls back to the other when missing). A song with
 * no tags is left alone. The result is capped so that the loudest sample, scaled by the gain, stays at or below 0 dBFS.
 */
fun replayGainDb(mode: ReplayGainMode, tags: ReplayGain?, preampDb: Float): Float {
    if (mode == ReplayGainMode.OFF || tags == null) return 0f
    val preferAlbum = mode == ReplayGainMode.ALBUM
    val gain = (if (preferAlbum) tags.albumGainDb ?: tags.trackGainDb else tags.trackGainDb ?: tags.albumGainDb)
        ?: return 0f
    val peak = if (preferAlbum) tags.albumPeak ?: tags.trackPeak else tags.trackPeak ?: tags.albumPeak
    val wanted = gain + preampDb
    if (peak == null || peak <= 0f) return wanted
    val ceilingDb = (-20 * log10(peak.toDouble())).toFloat()
    return min(wanted, ceilingDb)
}

/** "-6.54 dB", "+3.2dB" or "-6.54" to -6.54. Anything else is null. */
fun parseGainDb(text: String): Float? {
    val cleaned = text.trim().lowercase(Locale.ROOT).removeSuffix("db").trim().replace(',', '.')
    return cleaned.toFloatOrNull()?.takeIf { it.isFinite() }
}

/** Peak sample value, normally 0..~2. Zero, negative and junk values are rejected. */
fun parsePeak(text: String): Float? =
    text.trim().replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() && it > 0f }

/**
 * Builds ReplayGain from raw tag name/value pairs, whatever the container (ID3 TXXX, Vorbis comments, MP4 atoms).
 * Names are matched ignoring case. Returns null when the file has none of the four values.
 */
fun replayGainFromTags(entries: List<Pair<String, String>>): ReplayGain? {
    var trackGain: Float? = null
    var albumGain: Float? = null
    var trackPeak: Float? = null
    var albumPeak: Float? = null
    for ((name, value) in entries) {
        when (name.trim().lowercase(Locale.ROOT)) {
            "replaygain_track_gain" -> trackGain = parseGainDb(value) ?: trackGain
            "replaygain_album_gain" -> albumGain = parseGainDb(value) ?: albumGain
            "replaygain_track_peak" -> trackPeak = parsePeak(value) ?: trackPeak
            "replaygain_album_peak" -> albumPeak = parsePeak(value) ?: albumPeak
        }
    }
    if (trackGain == null && albumGain == null) return null
    return ReplayGain(trackGain, albumGain, trackPeak, albumPeak)
}
