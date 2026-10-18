package com.kingplay.feature.equalizer

import com.kingplay.feature.equalizer.logic.bandLabel
import com.kingplay.feature.equalizer.logic.curveFrequencies
import com.kingplay.feature.equalizer.logic.formatDb
import com.kingplay.feature.equalizer.logic.formatSemitones
import com.kingplay.feature.equalizer.logic.formatTempo
import com.kingplay.feature.equalizer.logic.isFlat
import com.kingplay.feature.equalizer.logic.logPosition
import com.kingplay.feature.equalizer.logic.snapDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerLogicTest {
    @Test fun bandLabelsAreShort() {
        assertEquals("31", bandLabel(31))
        assertEquals("1k", bandLabel(1_000))
        assertEquals("16k", bandLabel(16_000))
    }

    @Test fun dbFormatting() {
        assertEquals("0 dB", formatDb(0f))
        assertEquals("+3 dB", formatDb(3f))
        assertEquals("+3.5 dB", formatDb(3.5f))
        assertEquals("-2 dB", formatDb(-2f))
        assertEquals("-0.5 dB", formatDb(-0.5f))
    }

    @Test fun tempoAndPitchFormatting() {
        assertEquals("1x", formatTempo(1f))
        assertEquals("1.25x", formatTempo(1.25f))
        assertEquals("0.5x", formatTempo(0.5f))
        assertEquals("+3", formatSemitones(3f))
        assertEquals("-2", formatSemitones(-2f))
        assertEquals("0", formatSemitones(0f))
    }

    @Test fun snapsToHalfDecibels() {
        assertEquals(1.5f, snapDb(1.4f), 0.0001f)
        assertEquals(-2f, snapDb(-1.9f), 0.0001f)
    }

    @Test fun curveAxisIsLogarithmic() {
        assertEquals(0.0, logPosition(20.0), 1e-9)
        assertEquals(1.0, logPosition(20_000.0), 1e-9)
        assertEquals(0.5, logPosition(632.455532), 1e-4)
        val f = curveFrequencies(5)
        assertEquals(5, f.size)
        assertEquals(20.0, f.first(), 1e-6)
        assertEquals(20_000.0, f.last(), 1e-3)
    }

    @Test fun flatDetection() {
        assertTrue(isFlat(List(10) { 0f }, 0f))
        assertFalse(isFlat(List(10) { if (it == 3) 1f else 0f }, 0f))
        assertFalse(isFlat(List(10) { 0f }, 2f))
    }
}
