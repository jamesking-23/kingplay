package com.kingplay.core.model

import com.kingplay.core.model.lyrics.activeLineIndex
import com.kingplay.core.model.lyrics.parseLyrics
import com.kingplay.core.model.sleep.SleepTimerSpec
import com.kingplay.core.model.sleep.SleepTimerState
import com.kingplay.core.model.sleep.extendSleepTimer
import com.kingplay.core.model.sleep.remainingMs
import com.kingplay.core.model.sleep.sleepTick
import com.kingplay.core.model.sleep.sleepTrackFinished
import com.kingplay.core.model.sleep.sleepVolume
import com.kingplay.core.model.sleep.startSleepTimer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTest {
    @Test fun parsesSyncedLines() {
        val lyrics = parseLyrics("[ar:Someone]\n[ti:Song]\n[00:12.50]First\n[01:02.5]Second\n[01:05]Third")!!
        assertTrue(lyrics.synced)
        assertEquals(listOf(12_500L, 62_500L, 65_000L), lyrics.lines.map { it.timeMs })
        assertEquals(listOf("First", "Second", "Third"), lyrics.lines.map { it.text })
    }

    @Test fun repeatedTimestampsAreSortedIn() {
        val lyrics = parseLyrics("[00:30.00][01:30.00]Chorus\n[00:10.00]Verse")!!
        assertEquals(listOf("Verse", "Chorus", "Chorus"), lyrics.lines.map { it.text })
        assertEquals(listOf(10_000L, 30_000L, 90_000L), lyrics.lines.map { it.timeMs })
    }

    @Test fun offsetMovesLyricsEarlier() {
        val lyrics = parseLyrics("[offset:+500]\n[00:10.00]Line")!!
        assertEquals(9_500L, lyrics.lines.single().timeMs)
    }

    @Test fun wordTagsAreRemoved() {
        val lyrics = parseLyrics("[00:01.00]<00:01.00>Hel<00:01.40>lo")!!
        assertEquals("Hello", lyrics.lines.single().text)
    }

    @Test fun plainTextKeepsVerseBreaks() {
        val lyrics = parseLyrics("\nOne\nTwo\n\n\nThree\n\n")!!
        assertFalse(lyrics.synced)
        assertEquals(listOf("One", "Two", "", "Three"), lyrics.lines.map { it.text })
        assertTrue(lyrics.lines.all { it.timeMs == null })
    }

    @Test fun emptyAndTagOnlyTextIsNull() {
        assertNull(parseLyrics(""))
        assertNull(parseLyrics("   \n  "))
        assertNull(parseLyrics("[ar:A]\n[ti:B]"))
    }

    @Test fun bracketedSectionNamesStayAsPlainText() {
        val lyrics = parseLyrics("[Chorus]\nSing")!!
        assertEquals(listOf("[Chorus]", "Sing"), lyrics.lines.map { it.text })
    }

    @Test fun windowsLineEndingsAndBom() {
        val lyrics = parseLyrics("﻿[00:01.00]A\r\n[00:02.00]B\r\n")!!
        assertEquals(listOf("A", "B"), lyrics.lines.map { it.text })
    }

    @Test fun activeLineFollowsPosition() {
        val lyrics = parseLyrics("[00:10.00]A\n[00:20.00]B\n[00:30.00]C")!!
        assertEquals(-1, activeLineIndex(lyrics, 5_000))
        assertEquals(0, activeLineIndex(lyrics, 10_000))
        assertEquals(0, activeLineIndex(lyrics, 19_999))
        assertEquals(1, activeLineIndex(lyrics, 20_000))
        assertEquals(2, activeLineIndex(lyrics, 999_999))
    }

    @Test fun plainLyricsHaveNoActiveLine() {
        assertEquals(-1, activeLineIndex(parseLyrics("a\nb")!!, 10_000))
    }
}

class SleepTimerTest {
    @Test fun minutesBecomeACountdown() {
        val state = startSleepTimer(SleepTimerSpec.Minutes(30), nowMs = 1_000)
        assertEquals(SleepTimerState.Timed(endsAtMs = 1_801_000, totalMs = 1_800_000), state)
        assertEquals(1_800_000L, remainingMs(state, 1_000))
        assertEquals(0L, remainingMs(state, 5_000_000))
    }

    @Test fun inputIsClamped() {
        assertEquals(60_000L, (startSleepTimer(SleepTimerSpec.Minutes(0), 0) as SleepTimerState.Timed).totalMs)
        assertEquals(720 * 60_000L, (startSleepTimer(SleepTimerSpec.Minutes(9_999), 0) as SleepTimerState.Timed).totalMs)
        assertEquals(1, (startSleepTimer(SleepTimerSpec.Tracks(-3), 0) as SleepTimerState.AfterTracks).remaining)
    }

    @Test fun tickPausesOnlyWhenTimeIsUp() {
        val state = startSleepTimer(SleepTimerSpec.Minutes(1), 0)
        assertFalse(sleepTick(state, 59_999).pause)
        val done = sleepTick(state, 60_000)
        assertTrue(done.pause)
        assertEquals(SleepTimerState.Off, done.state)
    }

    @Test fun trackCountdown() {
        var step = sleepTrackFinished(SleepTimerState.AfterTracks(2))
        assertFalse(step.pause)
        assertEquals(SleepTimerState.AfterTracks(1), step.state)
        step = sleepTrackFinished(step.state)
        assertTrue(step.pause)
        assertEquals(SleepTimerState.Off, step.state)
    }

    @Test fun otherStatesIgnoreTicksAndFinishedTracks() {
        assertFalse(sleepTick(SleepTimerState.Off, 0).pause)
        assertFalse(sleepTick(SleepTimerState.AfterTracks(1), 0).pause)
        assertFalse(sleepTrackFinished(SleepTimerState.Off).pause)
        assertFalse(sleepTrackFinished(startSleepTimer(SleepTimerSpec.Minutes(5), 0)).pause)
    }

    @Test fun extendAddsTimeToACountdownOnly() {
        val state = startSleepTimer(SleepTimerSpec.Minutes(10), 0)
        val longer = extendSleepTimer(state, 5) as SleepTimerState.Timed
        assertEquals(15 * 60_000L, longer.endsAtMs)
        assertEquals(15 * 60_000L, longer.totalMs)
        assertEquals(SleepTimerState.AfterTracks(2), extendSleepTimer(SleepTimerState.AfterTracks(2), 5))
    }

    @Test fun volumeFadesOverTheLastThirtySeconds() {
        val state = startSleepTimer(SleepTimerSpec.Minutes(10), 0) as SleepTimerState.Timed
        assertEquals(1f, sleepVolume(state, 0), 0.0001f)
        assertEquals(1f, sleepVolume(state, state.endsAtMs - 30_000), 0.0001f)
        val mid = sleepVolume(state, state.endsAtMs - 15_000)
        assertTrue(mid > 0.6f && mid < 0.8f)
        assertEquals(0f, sleepVolume(state, state.endsAtMs), 0.0001f)
        assertEquals(1f, sleepVolume(SleepTimerState.Off, 0), 0.0001f)
    }
}
