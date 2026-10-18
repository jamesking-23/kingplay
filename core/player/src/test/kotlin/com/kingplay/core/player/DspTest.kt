package com.kingplay.core.player

import com.kingplay.core.model.AudioSettings
import com.kingplay.core.model.DeviceProfile
import com.kingplay.core.model.EffectSettings
import com.kingplay.core.model.EqPreset
import com.kingplay.core.model.OutputDevice
import com.kingplay.core.model.OutputDeviceType
import com.kingplay.core.model.ReplayGain
import com.kingplay.core.model.ReplayGainMode
import com.kingplay.core.model.sanitized
import com.kingplay.core.player.dsp.AudioSettingsCodec
import com.kingplay.core.player.dsp.BiquadCoefficients
import com.kingplay.core.player.dsp.BiquadDesign
import com.kingplay.core.player.dsp.BiquadState
import com.kingplay.core.player.dsp.DspConfig
import com.kingplay.core.player.dsp.EqChain
import com.kingplay.core.player.dsp.Fft
import com.kingplay.core.player.dsp.OutputCandidate
import com.kingplay.core.player.dsp.SPECTRUM_BANDS
import com.kingplay.core.player.dsp.SpectrumAnalyzer
import com.kingplay.core.player.dsp.bandEdges
import com.kingplay.core.player.dsp.buildDspConfig
import com.kingplay.core.player.dsp.fadeVolume
import com.kingplay.core.player.dsp.outputKey
import com.kingplay.core.player.dsp.parseGainDb
import com.kingplay.core.player.dsp.parsePeak
import com.kingplay.core.player.dsp.peakBoostDb
import com.kingplay.core.player.dsp.pickActiveOutput
import com.kingplay.core.player.dsp.profileFrom
import com.kingplay.core.player.dsp.replayGainDb
import com.kingplay.core.player.dsp.replayGainFromTags
import com.kingplay.core.player.dsp.responseDb
import com.kingplay.core.player.dsp.settingsFrom
import com.kingplay.core.player.dsp.softLimit
import com.kingplay.core.player.dsp.toOutputDevice
import com.kingplay.core.player.lyrics.parseUslt
import com.kingplay.core.player.lyrics.pickLyrics
import com.kingplay.core.player.lyrics.syltToLrc
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val FS = 48_000.0

private fun rms(values: DoubleArray, skip: Int): Double {
    var sum = 0.0
    for (i in skip until values.size) sum += values[i] * values[i]
    return sqrt(sum / (values.size - skip))
}

private fun sine(freq: Double, amplitude: Double, count: Int) =
    DoubleArray(count) { amplitude * sin(2 * PI * freq * it / FS) }

class BiquadTest {
    @Test
    fun peakingGainAtCenterMatchesRequest() {
        for (gain in listOf(-12.0, -6.0, 3.0, 6.0, 12.0)) {
            val c = BiquadDesign.peaking(FS, 1_000.0, 1.414, gain)
            assertEquals(gain, c.magnitudeDb(1_000.0, FS), 0.01)
        }
    }

    @Test
    fun peakingIsLocal() {
        val c = BiquadDesign.peaking(FS, 1_000.0, 1.414, 6.0)
        assertTrue(abs(c.magnitudeDb(30.0, FS)) < 0.2)
        assertTrue(abs(c.magnitudeDb(18_000.0, FS)) < 0.5)
    }

    @Test
    fun zeroGainIsIdentity() {
        assertTrue(BiquadDesign.peaking(FS, 1_000.0, 1.414, 0.0).isIdentity)
        assertTrue(BiquadDesign.lowShelf(FS, 100.0, 0.0).isIdentity)
        assertEquals(0.0, BiquadCoefficients.Identity.magnitudeDb(500.0, FS), 1e-9)
    }

    @Test
    fun lowShelfBoostsBassOnly() {
        val c = BiquadDesign.lowShelf(FS, 100.0, 6.0)
        assertEquals(6.0, c.magnitudeDb(20.0, FS), 0.3)
        assertTrue(abs(c.magnitudeDb(5_000.0, FS)) < 0.1)
        assertEquals(3.0, c.magnitudeDb(100.0, FS), 0.3)
    }

    @Test
    fun filteredSineIsScaledByTheGain() {
        val c = BiquadDesign.peaking(FS, 1_000.0, 1.414, 6.0)
        val state = BiquadState()
        val input = sine(1_000.0, 0.25, 24_000)
        val output = DoubleArray(input.size) { state.process(input[it], c) }
        val ratio = rms(output, 4_000) / rms(input, 4_000)
        assertEquals(1.995, ratio, 0.03)
    }
}

class DspConfigTest {
    private val bumpAt1k = EqPreset.Flat.bandGainsDb.toMutableList().also { it[5] = 6f }

    @Test
    fun defaultSettingsBypass() {
        assertTrue(buildDspConfig(AudioSettings.Default, 0f).bypass)
    }

    @Test
    fun disabledEqIgnoresBands() {
        val settings = AudioSettings(eqEnabled = false, bandGainsDb = bumpAt1k, preampDb = 5f)
        assertTrue(buildDspConfig(settings, 0f).bypass)
    }

    @Test
    fun enabledEqIsNotBypassedAndHeadroomLowersTheLevel() {
        val settings = AudioSettings(eqEnabled = true, bandGainsDb = bumpAt1k)
        val config = buildDspConfig(settings, 0f)
        assertFalse(config.bypass)
        // The curve peaks near +6 dB, so the whole signal is lowered by about that much.
        val gainDb = 20 * log10(config.outputGain.toDouble())
        assertEquals(-6.0, gainDb, 0.2)
    }

    @Test
    fun headroomCanBeSwitchedOff() {
        val settings = AudioSettings(eqEnabled = true, bandGainsDb = bumpAt1k, autoHeadroom = false)
        assertEquals(1f, buildDspConfig(settings, 0f).outputGain, 1e-6f)
    }

    @Test
    fun cutsNeedNoHeadroom() {
        val cuts = List(EqPreset.BAND_COUNT) { -4f }
        val config = buildDspConfig(AudioSettings(eqEnabled = true, bandGainsDb = cuts), 0f)
        assertEquals(1f, config.outputGain, 1e-6f)
        assertFalse(config.bypass)
    }

    @Test
    fun loudnessAndReplayGainAddUp() {
        val settings = AudioSettings(effects = EffectSettings(loudnessGainMb = 600))
        val config = buildDspConfig(settings, -6f)
        assertEquals(1f, config.outputGain, 0.01f)
        val onlyRg = buildDspConfig(AudioSettings.Default, -6.0206f)
        assertEquals(0.5f, onlyRg.outputGain, 0.001f)
        assertFalse(onlyRg.bypass)
    }

    @Test
    fun peakBoostOfFlatCurveIsZeroAndOfOneBandIsItsGain() {
        assertEquals(0.0, peakBoostDb(DoubleArray(10), 0.0, FS), 0.0)
        val bands = DoubleArray(10).also { it[5] = 6.0 }
        assertEquals(6.0, peakBoostDb(bands, 0.0, FS), 0.2)
    }

    @Test
    fun bassBoostScalesWithStrength() {
        val half = buildDspConfig(AudioSettings(effects = EffectSettings(bassBoostStrength = 500)), 0f)
        assertEquals(4.5, half.bassShelfDb, 1e-9)
        assertFalse(half.bypass)
    }

    @Test
    fun responseMatchesTheBands() {
        val bands = DoubleArray(10).also { it[5] = 6.0 }
        assertEquals(6.0, responseDb(bands, 0.0, 1_000.0), 0.01)
        assertEquals(0.0, responseDb(DoubleArray(10), 0.0, 1_000.0), 1e-9)
    }

    @Test
    fun softLimitIsTransparentBelowTheKneeAndBoundedAbove() {
        assertEquals(0.5f, softLimit(0.5f), 0f)
        assertEquals(-0.9f, softLimit(-0.9f), 0f)
        for (x in listOf(0.95f, 1f, 1.5f, 4f, 100f)) {
            val y = softLimit(x)
            assertTrue(y in 0.9f..1f)
            assertEquals(-y, softLimit(-x), 0f)
        }
        assertTrue(softLimit(1.2f) > softLimit(1.0f))
    }

    @Test
    fun chainScalesTheSignalAndHeadroomCancelsTheBoost() {
        val bands = EqPreset.Flat.bandGainsDb.toMutableList().also { it[5] = 6f }
        val input = sine(1_000.0, 0.2, 24_000)

        fun run(settings: AudioSettings): Double {
            val chain = EqChain(FS.toInt(), 1)
            chain.configure(buildDspConfig(settings, 0f))
            val samples = FloatArray(input.size) { input[it].toFloat() }
            chain.process(samples, samples.size)
            return rms(DoubleArray(samples.size) { samples[it].toDouble() }, 4_000) / rms(input, 4_000)
        }

        assertEquals(1.995, run(AudioSettings(eqEnabled = true, bandGainsDb = bands, autoHeadroom = false)), 0.04)
        assertEquals(1.0, run(AudioSettings(eqEnabled = true, bandGainsDb = bands)), 0.04)
    }

    @Test
    fun chainKeepsChannelsSeparate() {
        val chain = EqChain(FS.toInt(), 2)
        val bands = DoubleArray(10).also { it[5] = 6.0 }
        chain.configure(DspConfig(bands, 0.0, 1f))
        val frames = 2_000
        val samples = FloatArray(frames * 2)
        for (i in 0 until frames) samples[i * 2] = (0.2 * sin(2 * PI * 1_000.0 * i / FS)).toFloat()
        chain.process(samples, frames)
        var rightMax = 0f
        for (i in 0 until frames) rightMax = maxOf(rightMax, abs(samples[i * 2 + 1]))
        assertEquals(0f, rightMax, 0f)
    }
}

class ReplayGainTest {
    @Test
    fun parsesGainAndPeakText() {
        assertEquals(-6.54f, parseGainDb("-6.54 dB")!!, 1e-6f)
        assertEquals(3.2f, parseGainDb("+3.2dB")!!, 1e-6f)
        assertEquals(-7.1f, parseGainDb(" -7,1 DB ")!!, 1e-6f)
        assertNull(parseGainDb("loud"))
        assertNull(parseGainDb("NaN"))
        assertEquals(0.98f, parsePeak("0.98")!!, 1e-6f)
        assertNull(parsePeak("0"))
        assertNull(parsePeak("-1"))
    }

    @Test
    fun readsTagsIgnoringCase() {
        val tags = replayGainFromTags(
            listOf(
                "REPLAYGAIN_TRACK_GAIN" to "-5.00 dB",
                "replaygain_album_gain" to "-7.50 dB",
                "ReplayGain_Track_Peak" to "0.95",
                "TITLE" to "ignored",
            ),
        )
        assertNotNull(tags)
        assertEquals(-5f, tags!!.trackGainDb!!, 1e-6f)
        assertEquals(-7.5f, tags.albumGainDb!!, 1e-6f)
        assertEquals(0.95f, tags.trackPeak!!, 1e-6f)
        assertNull(tags.albumPeak)
    }

    @Test
    fun noGainTagsMeansNull() {
        assertNull(replayGainFromTags(listOf("TITLE" to "x", "REPLAYGAIN_TRACK_PEAK" to "0.9")))
    }

    private val tags = ReplayGain(trackGainDb = -5f, albumGainDb = -8f, trackPeak = null, albumPeak = null)

    @Test
    fun modeChoosesTrackOrAlbum() {
        assertEquals(-5f, replayGainDb(ReplayGainMode.TRACK, tags, 0f), 1e-6f)
        assertEquals(-8f, replayGainDb(ReplayGainMode.ALBUM, tags, 0f), 1e-6f)
        assertEquals(0f, replayGainDb(ReplayGainMode.OFF, tags, 0f), 0f)
    }

    @Test
    fun missingValuesFallBackAndUntaggedSongsAreUntouched() {
        val onlyAlbum = ReplayGain(null, -8f, null, null)
        assertEquals(-8f, replayGainDb(ReplayGainMode.TRACK, onlyAlbum, 0f), 1e-6f)
        assertEquals(0f, replayGainDb(ReplayGainMode.TRACK, null, 3f), 0f)
    }

    @Test
    fun preampIsAddedButPeakStopsClipping() {
        assertEquals(-2f, replayGainDb(ReplayGainMode.TRACK, tags, 3f), 1e-6f)
        val loudQuiet = ReplayGain(trackGainDb = 4f, albumGainDb = null, trackPeak = 0.8f, albumPeak = null)
        // 0.8 peak leaves 1.94 dB of room, so a +4 dB gain is capped there.
        assertEquals(1.938f, replayGainDb(ReplayGainMode.TRACK, loudQuiet, 0f), 0.01f)
    }
}

class FadeTest {
    @Test
    fun noFadeIsFullVolume() {
        assertEquals(1f, fadeVolume(0, 200_000, 0), 0f)
        assertEquals(1f, fadeVolume(100, 0, 5_000), 0f)
    }

    @Test
    fun fadesInAndOut() {
        assertEquals(0f, fadeVolume(0, 200_000, 4_000), 1e-6f)
        assertEquals(0.707f, fadeVolume(2_000, 200_000, 4_000), 0.01f)
        assertEquals(1f, fadeVolume(4_000, 200_000, 4_000), 1e-6f)
        assertEquals(1f, fadeVolume(100_000, 200_000, 4_000), 0f)
        assertEquals(0.707f, fadeVolume(198_000, 200_000, 4_000), 0.01f)
        assertEquals(0f, fadeVolume(200_000, 200_000, 4_000), 1e-6f)
    }

    @Test
    fun shortSongsFadeForHalfTheirLength() {
        val mid = fadeVolume(5_000, 10_000, 12_000)
        assertEquals(1f, mid, 1e-6f)
        assertTrue(fadeVolume(2_500, 10_000, 12_000) < 1f)
    }

    @Test
    fun neverOutOfRange() {
        for (p in listOf(-100L, 0L, 1L, 50_000L, 199_999L, 400_000L)) {
            val v = fadeVolume(p, 200_000, 6_000)
            assertTrue(v in 0f..1f)
        }
    }
}

class OutputTest {
    private val speaker = OutputCandidate(OutputDeviceType.SPEAKER, "Phone speaker")
    private val bt = OutputCandidate(OutputDeviceType.BLUETOOTH, "Sony WH-1000XM5")
    private val usb = OutputCandidate(OutputDeviceType.USB_DAC, "FiiO KA3")
    private val wired = OutputCandidate(OutputDeviceType.WIRED, "Headphones")

    @Test
    fun picksTheMostSpecificOutput() {
        assertEquals(usb, pickActiveOutput(listOf(speaker, bt, usb, wired)))
        assertEquals(wired, pickActiveOutput(listOf(speaker, bt, wired)))
        assertEquals(bt, pickActiveOutput(listOf(speaker, bt)))
        assertEquals(speaker, pickActiveOutput(listOf(speaker)))
        assertNull(pickActiveOutput(emptyList()))
    }

    @Test
    fun keysAreStableAndCaseInsensitive() {
        assertEquals("speaker", outputKey(speaker))
        assertEquals("bluetooth:sony wh-1000xm5", outputKey(bt))
        assertEquals(outputKey(bt), outputKey(OutputCandidate(OutputDeviceType.BLUETOOTH, " SONY WH-1000XM5 ")))
    }

    @Test
    fun speakerGetsTheLocalisedName() {
        assertEquals("Phone speaker", speaker.toOutputDevice("Phone speaker").displayName)
        assertEquals("Sony WH-1000XM5", bt.toOutputDevice("Phone speaker").displayName)
    }
}

class ProfileAndCodecTest {
    private val device = OutputDevice("bluetooth:sony", OutputDeviceType.BLUETOOTH, "Sony")
    private val rock = EqPreset(-4, "Rock", 0f, List(10) { if (it < 3) 4f else 0f }, true)

    @Test
    fun profileKeepsPresetEffectsAndReplayGain() {
        val settings = AudioSettings(
            eqEnabled = true,
            presetId = -4,
            effects = EffectSettings(bassBoostStrength = 300),
            replayGainMode = ReplayGainMode.ALBUM,
        )
        val profile = profileFrom(settings, device, presetId = -4)
        assertEquals("bluetooth:sony", profile.deviceKey)
        assertEquals(-4L, profile.eqPresetId)
        assertEquals(300, profile.effects.bassBoostStrength)
        assertEquals(ReplayGainMode.ALBUM, profile.replayGainMode)
    }

    @Test
    fun profileWithEqOffStoresNoPreset() {
        val profile = profileFrom(AudioSettings(eqEnabled = false), device, presetId = -4)
        assertNull(profile.eqPresetId)
    }

    @Test
    fun applyingAProfileKeepsTheNonProfileSettings() {
        val profile = DeviceProfile("k", OutputDeviceType.BLUETOOTH, "Sony", -4, EffectSettings(loudnessGainMb = 200), ReplayGainMode.TRACK)
        val base = AudioSettings(fadeMs = 3_000, autoHeadroom = false, replayGainPreampDb = 2f)
        val result = settingsFrom(profile, rock, base)
        assertTrue(result.eqEnabled)
        assertEquals(-4L, result.presetId)
        assertEquals(rock.bandGainsDb, result.bandGainsDb)
        assertEquals(200, result.effects.loudnessGainMb)
        assertEquals(ReplayGainMode.TRACK, result.replayGainMode)
        assertEquals(3_000, result.fadeMs)
        assertFalse(result.autoHeadroom)
        assertEquals(2f, result.replayGainPreampDb, 0f)
    }

    @Test
    fun profileWithoutPresetTurnsEqOff() {
        val profile = DeviceProfile("k", OutputDeviceType.WIRED, "Wired", null, EffectSettings(), ReplayGainMode.OFF)
        val result = settingsFrom(profile, null, AudioSettings(eqEnabled = true, preampDb = 3f))
        assertFalse(result.eqEnabled)
        assertEquals(0f, result.preampDb, 0f)
        assertEquals(EqPreset.Flat.bandGainsDb, result.bandGainsDb)
    }

    @Test
    fun codecRoundTrips() {
        val settings = AudioSettings(
            eqEnabled = true,
            preampDb = -2.5f,
            bandGainsDb = List(10) { it - 4f },
            presetId = null,
            effects = EffectSettings(250, 400, 1.25f, -3f),
            replayGainMode = ReplayGainMode.ALBUM,
            replayGainPreampDb = 1.5f,
            autoHeadroom = false,
            fadeMs = 4_000,
        )
        assertEquals(settings, AudioSettingsCodec.decode(AudioSettingsCodec.encode(settings)))
    }

    @Test
    fun codecSurvivesGarbageAndMissingValues() {
        assertEquals(AudioSettings.Default, AudioSettingsCodec.decode(emptyMap()))
        val damaged = mapOf(
            "band_gains_db" to "1,2,three",
            "preamp_db" to "NaN",
            "tempo" to "fast",
            "replay_gain_mode" to "LOUDEST",
            "fade_ms" to "999999",
            "bass_boost" to "-50",
        )
        val result = AudioSettingsCodec.decode(damaged)
        assertEquals(EqPreset.Flat.bandGainsDb, result.bandGainsDb)
        assertEquals(0f, result.preampDb, 0f)
        assertEquals(1f, result.effects.tempo, 0f)
        assertEquals(ReplayGainMode.OFF, result.replayGainMode)
        assertEquals(AudioSettings.MAX_FADE_MS, result.fadeMs)
        assertEquals(0, result.effects.bassBoostStrength)
    }

    @Test
    fun sanitizedClampsEverything() {
        val wild = AudioSettings(
            preampDb = 99f,
            bandGainsDb = List(10) { 50f },
            effects = EffectSettings(5_000, 9_999, 9f, 40f),
            replayGainPreampDb = -99f,
            fadeMs = -5,
        ).sanitized()
        assertEquals(AudioSettings.MAX_PREAMP_DB, wild.preampDb, 0f)
        assertTrue(wild.bandGainsDb.all { it == EqPreset.MAX_GAIN_DB })
        assertEquals(AudioSettings.MAX_BASS_BOOST, wild.effects.bassBoostStrength)
        assertEquals(AudioSettings.MAX_LOUDNESS_MB, wild.effects.loudnessGainMb)
        assertEquals(AudioSettings.MAX_TEMPO, wild.effects.tempo, 0f)
        assertEquals(AudioSettings.MAX_PITCH_SEMITONES, wild.effects.pitchSemitones, 0f)
        assertEquals(-AudioSettings.MAX_PREAMP_DB, wild.replayGainPreampDb, 0f)
        assertEquals(0, wild.fadeMs)
    }
}

class SpectrumTest {
    private val fs = 48_000

    private fun sine(hz: Double, amplitude: Double, frames: Int): FloatArray =
        FloatArray(frames) { (amplitude * Math.sin(2 * Math.PI * hz * it / fs)).toFloat() }

    private fun run(samples: FloatArray, channels: Int = 1): FloatArray {
        var last = FloatArray(SPECTRUM_BANDS)
        val analyzer = SpectrumAnalyzer(fs, channels) { last = it }
        analyzer.process(samples, samples.size / channels)
        return last
    }

    @Test fun fftFindsASineInTheRightBin() {
        val fft = Fft(1_024)
        val re = FloatArray(1_024) { Math.sin(2 * Math.PI * 64 * it / 1_024).toFloat() }
        val im = FloatArray(1_024)
        fft.transform(re, im)
        val mags = FloatArray(512) { Math.hypot(re[it].toDouble(), im[it].toDouble()).toFloat() }
        val peak = mags.indices.maxByOrNull { mags[it] }!!
        assertEquals(64, peak)
        assertEquals(512.0, mags[64].toDouble(), 1.0)
    }

    @Test fun silenceReadsZero() {
        assertTrue(run(FloatArray(8_192)).all { it == 0f })
    }

    @Test fun aToneLightsTheBandItBelongsTo() {
        val bands = run(sine(1_000.0, 0.5, 16_384))
        val edges = bandEdges(SPECTRUM_BANDS, fs)
        val binOf1k = (1_000.0 / (fs / 1_024.0)).toInt()
        val expected = (0 until SPECTRUM_BANDS).first { binOf1k in edges[it]..edges[it + 1] }
        val loudest = bands.indices.maxByOrNull { bands[it] }!!
        assertTrue(Math.abs(loudest - expected) <= 1)
        assertTrue(bands[loudest] > 0.8f)
        assertTrue(bands[0] < 0.3f)
        assertTrue(bands[SPECTRUM_BANDS - 1] < 0.3f)
    }

    @Test fun stereoIsMixedToMono() {
        val mono = sine(2_000.0, 0.5, 8_192)
        val stereo = FloatArray(mono.size * 2) { mono[it / 2] }
        val a = run(mono)
        val b = run(stereo, channels = 2)
        for (i in a.indices) assertEquals(a[i].toDouble(), b[i].toDouble(), 0.001)
    }

    @Test fun barsFallAfterTheSoundStops() {
        var last = FloatArray(SPECTRUM_BANDS)
        val analyzer = SpectrumAnalyzer(fs, 1) { last = it }
        val tone = sine(1_000.0, 0.5, 8_192)
        analyzer.process(tone, tone.size)
        val loud = last.max()
        val quiet = FloatArray(8_192)
        analyzer.process(quiet, quiet.size)
        assertTrue(last.max() < loud * 0.5f)
    }

    @Test fun bandEdgesAreOrderedAndInRange() {
        for (rate in listOf(22_050, 44_100, 48_000, 96_000)) {
            val e = bandEdges(SPECTRUM_BANDS, rate)
            assertEquals(SPECTRUM_BANDS + 1, e.size)
            for (i in 1 until e.size) assertTrue(e[i] >= e[i - 1])
            assertTrue(e.first() >= 1 && e.last() <= 511)
        }
    }
}

class EmbeddedLyricsTest {
    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test fun usltInLatin1() {
        val data = bytes(0, 'e'.code, 'n'.code, 'g'.code, 0) + "Hello\nWorld".toByteArray(Charsets.ISO_8859_1)
        assertEquals("Hello\nWorld", parseUslt(data))
    }

    @Test fun usltInUtf8SkipsTheDescriptor() {
        val data = bytes(3, 'e'.code, 'n'.code, 'g'.code) + "desc".toByteArray() + bytes(0) + "Zażółć".toByteArray()
        assertEquals("Zażółć", parseUslt(data))
    }

    @Test fun usltInUtf16WithBom() {
        val text = "Hi".toByteArray(Charsets.UTF_16LE)
        val data = bytes(1, 'e'.code, 'n'.code, 'g'.code, 0, 0) + bytes(0xFF, 0xFE) + text
        assertEquals("Hi", parseUslt(data))
    }

    @Test fun malformedUsltIsNull() {
        assertNull(parseUslt(bytes(0, 1)))
        assertNull(parseUslt(bytes(9, 'e'.code, 'n'.code, 'g'.code, 0, 65)))
        assertNull(parseUslt(bytes(0, 'e'.code, 'n'.code, 'g'.code, 65, 66)))
    }

    @Test fun syltBecomesLrc() {
        val body = bytes(0, 'e'.code, 'n'.code, 'g'.code, 2, 1, 0) +
            "Hello".toByteArray() + bytes(0, 0, 0, 0x03, 0xE8) +
            "World".toByteArray() + bytes(0, 0, 0, 0xEA, 0x60)
        assertEquals("[00:01.00]Hello\n[01:00.00]World\n", syltToLrc(body))
    }

    @Test fun syltInFrameUnitsIsSkipped() {
        val body = bytes(0, 'e'.code, 'n'.code, 'g'.code, 1, 1, 0) + "A".toByteArray() + bytes(0, 0, 0, 0, 5)
        assertNull(syltToLrc(body))
    }

    @Test fun picksTimedLyricsOverPlain() {
        val picked = pickLyrics(
            listOf(
                "TITLE" to "[00:01.00]Not lyrics",
                "UNSYNCEDLYRICS" to "A very long plain text version of the song that goes on and on",
                "SYLT" to "[00:01.00]Timed\n[00:02.00]Lines",
            ),
        )!!
        assertTrue(picked.synced)
        assertEquals(listOf("Timed", "Lines"), picked.lines.map { it.text })
    }

    @Test fun noLyricsTagsGivesNull() {
        assertNull(pickLyrics(listOf("ARTIST" to "Someone", "REPLAYGAIN_TRACK_GAIN" to "-3 dB")))
    }
}
