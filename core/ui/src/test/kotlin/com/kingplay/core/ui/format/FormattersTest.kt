package com.kingplay.core.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {
    @Test
    fun durationUnderAnHour() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:09", formatDuration(9_999))
        assertEquals("3:45", formatDuration(225_000))
        assertEquals("59:59", formatDuration(3_599_999))
    }

    @Test
    fun durationFromOneHour() {
        assertEquals("1:00:00", formatDuration(3_600_000))
        assertEquals("2:05:07", formatDuration(7_507_000))
    }

    @Test
    fun negativeDurationIsZero() {
        assertEquals("0:00", formatDuration(-5_000))
    }

    @Test
    fun totalDuration() {
        assertEquals("0 min", formatTotalDuration(30_000))
        assertEquals("42 min", formatTotalDuration(42 * 60_000L + 59_000))
        assertEquals("3 h 05 min", formatTotalDuration((3 * 60 + 5) * 60_000L))
        assertEquals("1 h 00 min", formatTotalDuration(3_600_000))
    }

    @Test
    fun sampleRates() {
        assertEquals("44.1 kHz", formatSampleRate(44_100))
        assertEquals("48 kHz", formatSampleRate(48_000))
        assertEquals("96 kHz", formatSampleRate(96_000))
        assertEquals("88.2 kHz", formatSampleRate(88_200))
    }

    @Test
    fun qualitySkipsUnknownParts() {
        assertEquals("FLAC · 44.1 kHz · 920 kbps", formatQuality("flac", 44_100, 920))
        assertEquals("MP3 · 320 kbps", formatQuality("MP3", 0, 320))
        assertEquals("OGG", formatQuality("ogg", 0, 0))
        assertEquals("", formatQuality("", 0, 0))
    }

    @Test
    fun progress() {
        assertEquals(0f, progressFraction(1_000, 0), 0f)
        assertEquals(0.5f, progressFraction(50, 100), 0f)
        assertEquals(1f, progressFraction(500, 100), 0f)
        assertEquals(0f, progressFraction(-5, 100), 0f)
    }
}
