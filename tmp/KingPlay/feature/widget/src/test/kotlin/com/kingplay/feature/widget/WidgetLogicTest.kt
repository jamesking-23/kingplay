package com.kingplay.feature.widget

import com.kingplay.core.model.repository.NowPlayingSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLogicTest {
    private fun song(title: String = "Song", artist: String = "Artist") =
        NowPlayingSnapshot("a:1", title, artist, isPlaying = true, artworkSource = null)

    @Test fun idleShowsHint() {
        val l = widgetLines(NowPlayingSnapshot.Idle, "Nothing playing", "Tap to open")
        assertTrue(l.idle)
        assertEquals("Nothing playing", l.title)
        assertEquals("Tap to open", l.subtitle)
    }

    @Test fun playingShowsTitleAndArtist() {
        val l = widgetLines(song(), "x", "y")
        assertFalse(l.idle)
        assertEquals("Song", l.title)
        assertEquals("Artist", l.subtitle)
    }

    @Test fun longTextIsClampedWithEllipsis() {
        val out = clampText("a".repeat(200))
        assertEquals(WIDGET_TEXT_LIMIT, out.length)
        assertTrue(out.endsWith("…"))
    }

    @Test fun shortTextIsUntouchedAndNewlinesFlattened() {
        assertEquals("hello world", clampText("  hello\nworld "))
    }

    @Test fun sampleSizeHalvesUntilNearTarget() {
        assertEquals(1, sampleSizeFor(200, 200))
        assertEquals(1, sampleSizeFor(511, 400))
        assertEquals(2, sampleSizeFor(512, 512))
        assertEquals(8, sampleSizeFor(2048, 1500))
    }

    @Test fun sampleSizeHandlesBadInput() {
        assertEquals(1, sampleSizeFor(0, 0))
        assertEquals(1, sampleSizeFor(100, 100, 0))
    }
}
