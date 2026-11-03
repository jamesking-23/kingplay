package com.kingplay.desktop

import com.kingplay.desktop.video.AbLoop
import com.kingplay.desktop.video.VideoCommand
import com.kingplay.desktop.video.clampVolume
import com.kingplay.desktop.video.commandForKey
import com.kingplay.desktop.video.formatDelay
import com.kingplay.desktop.video.formatRate
import com.kingplay.desktop.video.nextInCycle
import com.kingplay.desktop.video.steppedSpeed
import java.awt.event.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoControlsTest {
    @Test fun basicKeysDoWhatVlcDoes() {
        assertEquals(VideoCommand.TogglePlay, commandForKey(KeyEvent.VK_SPACE, false, false))
        assertEquals(VideoCommand.VolumeBy(5), commandForKey(KeyEvent.VK_UP, false, false))
        assertEquals(VideoCommand.VolumeBy(-5), commandForKey(KeyEvent.VK_DOWN, false, false))
        assertEquals(VideoCommand.ToggleFullscreen, commandForKey(KeyEvent.VK_F, false, false))
        assertEquals(VideoCommand.ExitFullscreen, commandForKey(KeyEvent.VK_ESCAPE, false, false))
        assertEquals(VideoCommand.ToggleMute, commandForKey(KeyEvent.VK_M, false, false))
        assertNull(commandForKey(KeyEvent.VK_Z, false, false))
    }

    @Test fun arrowsSeekByShortNormalAndLongSteps() {
        assertEquals(VideoCommand.Seek(10_000), commandForKey(KeyEvent.VK_RIGHT, false, false))
        assertEquals(VideoCommand.Seek(-3_000), commandForKey(KeyEvent.VK_LEFT, true, false))
        assertEquals(VideoCommand.Seek(60_000), commandForKey(KeyEvent.VK_RIGHT, false, true))
    }

    @Test fun speedStepsStayWithinTheList() {
        assertEquals(1.25f, steppedSpeed(1f, true), 0.0001f)
        assertEquals(0.75f, steppedSpeed(1f, false), 0.0001f)
        assertEquals(4f, steppedSpeed(4f, true), 0.0001f)
        assertEquals(0.25f, steppedSpeed(0.25f, false), 0.0001f)
        assertEquals("1.25x", formatRate(1.25f))
        assertEquals("1x", formatRate(1f))
    }

    @Test fun delaysAndVolumeAreFormattedAndClamped() {
        assertEquals("+0.05 s", formatDelay(50))
        assertEquals("-0.10 s", formatDelay(-100))
        assertEquals(0, clampVolume(-5))
        assertEquals(150, clampVolume(300))
    }

    @Test fun cyclingWrapsAndHandlesEmptyLists() {
        assertEquals(2, nextInCycle(listOf(-1, 1, 2), 1))
        assertEquals(-1, nextInCycle(listOf(-1, 1, 2), 2))
        assertEquals(-1, nextInCycle(listOf(-1, 1, 2), 99))
        assertNull(nextInCycle(emptyList(), 0))
    }

    @Test fun abLoopJumpsBackAtTheEnd() {
        var loop = AbLoop().mark(10_000)
        assertFalse(loop.active)
        loop = loop.mark(20_000)
        assertTrue(loop.active)
        assertNull(loop.jumpFor(19_000))
        assertEquals(10_000L, loop.jumpFor(20_500))
        assertEquals(AbLoop(start = 30_000), loop.mark(30_000))
    }
}
