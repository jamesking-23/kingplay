package com.kingplay.feature.video

import com.kingplay.feature.video.logic.DragAxis
import com.kingplay.feature.video.logic.TapZone
import com.kingplay.feature.video.logic.VideoResize
import com.kingplay.feature.video.logic.doubleTapSeekMs
import com.kingplay.feature.video.logic.dragAxis
import com.kingplay.feature.video.logic.formatSpeed
import com.kingplay.feature.video.logic.naturalCompare
import com.kingplay.feature.video.logic.nextResize
import com.kingplay.feature.video.logic.nextSpeed
import com.kingplay.feature.video.logic.prefersLandscape
import com.kingplay.feature.video.logic.resumeStartMs
import com.kingplay.feature.video.logic.subtitleMimeType
import com.kingplay.feature.video.logic.swipeLevel
import com.kingplay.feature.video.logic.swipeSeekTarget
import com.kingplay.feature.video.logic.tapZone
import com.kingplay.feature.video.logic.trackLabel
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoLogicTest {
    @Test fun tapZonesAreThirdsWithAWideMiddle() {
        assertEquals(TapZone.LEFT, tapZone(100f, 1000f))
        assertEquals(TapZone.CENTER, tapZone(500f, 1000f))
        assertEquals(TapZone.RIGHT, tapZone(900f, 1000f))
        assertEquals(TapZone.CENTER, tapZone(10f, 0f))
    }

    @Test fun doubleTapSkipsTenSeconds() {
        assertEquals(-10_000L, doubleTapSeekMs(TapZone.LEFT))
        assertEquals(10_000L, doubleTapSeekMs(TapZone.RIGHT))
        assertEquals(0L, doubleTapSeekMs(TapZone.CENTER))
    }

    @Test fun dragDirectionIsDecidedAfterTheSlop() {
        assertNull(dragAxis(3f, 2f, 10f))
        assertEquals(DragAxis.HORIZONTAL, dragAxis(30f, 5f, 10f))
        assertEquals(DragAxis.VERTICAL, dragAxis(5f, -40f, 10f))
    }

    @Test fun swipeSeekIsClampedToTheVideo() {
        assertEquals(70_000L, swipeSeekTarget(10_000, 500f, 1000f, 3_600_000))
        assertEquals(0L, swipeSeekTarget(10_000, -1000f, 1000f, 3_600_000))
        assertEquals(3_600_000L, swipeSeekTarget(3_590_000, 1000f, 1000f, 3_600_000))
        assertEquals(40_000L, swipeSeekTarget(0, 1000f, 1000f, 40_000))
        assertEquals(5_000L, swipeSeekTarget(5_000, 100f, 0f, 60_000))
    }

    @Test fun swipeLevelUpRaisesDownLowers() {
        assertTrue(swipeLevel(0.5f, -100f, 1000f) > 0.5f)
        assertTrue(swipeLevel(0.5f, 100f, 1000f) < 0.5f)
        assertEquals(1f, swipeLevel(0.9f, -1000f, 1000f), 0.0001f)
        assertEquals(0f, swipeLevel(0.1f, 1000f, 1000f), 0.0001f)
    }

    @Test fun resumeIgnoresTheVeryStartAndTheVeryEnd() {
        assertEquals(0L, resumeStartMs(3_000, 600_000))
        assertEquals(120_000L, resumeStartMs(120_000, 600_000))
        assertEquals(0L, resumeStartMs(595_000, 600_000))
        assertEquals(120_000L, resumeStartMs(120_000, 0))
    }

    @Test fun speedCyclesAndWrapsAndRecovers() {
        assertEquals(1.25f, nextSpeed(1f), 0.0001f)
        assertEquals(0.5f, nextSpeed(3f), 0.0001f)
        assertEquals(1f, nextSpeed(1.1f), 0.0001f)
        assertEquals("1x", formatSpeed(1f))
        assertEquals("1.25x", formatSpeed(1.25f))
        assertEquals("0.5x", formatSpeed(0.5f))
    }

    @Test fun resizeCyclesThroughAllModes() {
        assertEquals(VideoResize.ZOOM, nextResize(VideoResize.FIT))
        assertEquals(VideoResize.FILL, nextResize(VideoResize.ZOOM))
        assertEquals(VideoResize.FIT, nextResize(VideoResize.FILL))
    }

    @Test fun landscapeForWidePictures() {
        assertTrue(prefersLandscape(1920, 1080))
        assertTrue(prefersLandscape(1080, 1080))
        assertFalse(prefersLandscape(1080, 1920))
        assertFalse(prefersLandscape(0, 0))
    }

    @Test fun trackLabelsPreferTheFilesOwnName() {
        val en = Locale.ENGLISH
        assertEquals("Director commentary · Stereo", trackLabel("Director commentary", "en", 2, 0, en, "Track"))
        assertEquals("French · 5.1", trackLabel(null, "fr", 6, 1, en, "Track"))
        assertEquals("Track 3", trackLabel(null, null, 0, 2, en, "Track"))
        assertEquals("Track 1 · Mono", trackLabel("", "und", 1, 0, en, "Track"))
    }

    @Test fun subtitleTypesByNameThenByReportedType() {
        assertEquals("application/x-subrip", subtitleMimeType("Movie.EN.srt", null))
        assertEquals("text/vtt", subtitleMimeType("a.vtt", "application/octet-stream"))
        assertEquals("text/x-ssa", subtitleMimeType("a.ass", null))
        assertEquals("text/vtt", subtitleMimeType("noextension", "text/vtt"))
        assertNull(subtitleMimeType("notes.txt", "text/plain"))
        assertNull(subtitleMimeType(null, null))
    }

    @Test fun namesSortNaturally() {
        val sorted = listOf("Episode 10", "episode 2", "Episode 1", "Episode 02a", "Extra").sortedWith(::naturalCompare)
        assertEquals(listOf("Episode 1", "episode 2", "Episode 02a", "Episode 10", "Extra"), sorted)
        assertTrue(naturalCompare("a", "a") == 0)
        assertTrue(naturalCompare("a1", "a01") == 0)
        assertTrue(naturalCompare("file", "file2") < 0)
    }
}
