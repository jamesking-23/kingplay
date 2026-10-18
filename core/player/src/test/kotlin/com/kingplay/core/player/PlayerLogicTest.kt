package com.kingplay.core.player

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.QueueState
import com.kingplay.core.model.RepeatMode
import com.kingplay.core.player.logic.MediaIds
import com.kingplay.core.player.logic.PlayCountTracker
import com.kingplay.core.player.logic.defaultPlayThresholdMs
import com.kingplay.core.player.logic.playNextInsertIndex
import com.kingplay.core.player.logic.pruneQueue
import com.kingplay.core.player.logic.snapshotOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaIdsTest {
    @Test
    fun roundTripsAudioAndVideo() {
        assertEquals("a:42", MediaIds.of(MediaRef.Audio(42)))
        assertEquals("v:7", MediaIds.of(MediaRef.Video(7)))
        assertEquals(MediaRef.Audio(42), MediaIds.parse("a:42"))
        assertEquals(MediaRef.Video(7), MediaIds.parse("v:7"))
    }

    @Test
    fun rejectsAnythingElse() {
        assertNull(MediaIds.parse(null))
        assertNull(MediaIds.parse(""))
        assertNull(MediaIds.parse("x:1"))
        assertNull(MediaIds.parse("a:"))
        assertNull(MediaIds.parse("a:abc"))
        assertNull(MediaIds.parse("42"))
        assertNull(MediaIds.parse("kingplay_root"))
    }
}

class QueueSnapshotTest {
    @Test
    fun snapshotKeepsOrderIndexAndModes() {
        val s = snapshotOf(listOf("a:1", "v:2", "a:3"), 1, 12_000, true, RepeatMode.ALL)
        assertEquals(listOf(MediaRef.Audio(1), MediaRef.Video(2), MediaRef.Audio(3)), s.items)
        assertEquals(1, s.currentIndex)
        assertEquals(12_000L, s.positionMs)
        assertEquals(true, s.shuffleEnabled)
        assertEquals(RepeatMode.ALL, s.repeatMode)
    }

    @Test
    fun unparsableIdsAreDroppedAndIndexFollowsItsItem() {
        val s = snapshotOf(listOf("junk", "a:1", "junk", "a:2"), 3, 500, false, RepeatMode.OFF)
        assertEquals(listOf(MediaRef.Audio(1), MediaRef.Audio(2)), s.items)
        assertEquals(1, s.currentIndex)
        assertEquals(MediaRef.Audio(2), s.current)
    }

    @Test
    fun noCurrentItemMeansNoPosition() {
        val s = snapshotOf(listOf("a:1"), -1, 9_000, false, RepeatMode.OFF)
        assertEquals(-1, s.currentIndex)
        assertEquals(0L, s.positionMs)
    }

    private val queue = QueueState(
        items = listOf(MediaRef.Audio(1), MediaRef.Audio(2), MediaRef.Audio(3), MediaRef.Audio(4)),
        currentIndex = 2,
        positionMs = 40_000,
        shuffleEnabled = true,
        repeatMode = RepeatMode.ONE,
    )

    @Test
    fun pruneKeepsEverythingWhenAllExist() {
        assertEquals(queue.copy(shuffleOrder = emptyList()), pruneQueue(queue) { true })
    }

    @Test
    fun pruneBeforeCurrentShiftsIndexAndKeepsPosition() {
        val p = pruneQueue(queue) { it != MediaRef.Audio(1) }
        assertEquals(listOf(MediaRef.Audio(2), MediaRef.Audio(3), MediaRef.Audio(4)), p.items)
        assertEquals(1, p.currentIndex)
        assertEquals(MediaRef.Audio(3), p.current)
        assertEquals(40_000L, p.positionMs)
        assertEquals(RepeatMode.ONE, p.repeatMode)
    }

    @Test
    fun pruneCurrentResumesAtNextItemFromTheStart() {
        val p = pruneQueue(queue) { it != MediaRef.Audio(3) }
        assertEquals(listOf(MediaRef.Audio(1), MediaRef.Audio(2), MediaRef.Audio(4)), p.items)
        assertEquals(MediaRef.Audio(4), p.current)
        assertEquals(0L, p.positionMs)
    }

    @Test
    fun pruneCurrentAtEndFallsBackToLastItem() {
        val atEnd = queue.copy(currentIndex = 3)
        val p = pruneQueue(atEnd) { it != MediaRef.Audio(4) }
        assertEquals(MediaRef.Audio(3), p.current)
        assertEquals(0L, p.positionMs)
    }

    @Test
    fun pruneEverythingGivesEmptyQueueButKeepsModes() {
        val p = pruneQueue(queue) { false }
        assertEquals(emptyList<MediaRef>(), p.items)
        assertEquals(-1, p.currentIndex)
        assertEquals(true, p.shuffleEnabled)
        assertEquals(RepeatMode.ONE, p.repeatMode)
    }

    @Test
    fun playNextGoesAfterCurrentOrAtEnd() {
        assertEquals(3, playNextInsertIndex(2, 5))
        assertEquals(5, playNextInsertIndex(-1, 5))
        assertEquals(0, playNextInsertIndex(-1, 0))
        assertEquals(5, playNextInsertIndex(9, 5))
    }
}

class PlayCountTrackerTest {
    @Test
    fun thresholdIsHalfTheTrackCappedAtThirtySecondsWithAFloor() {
        assertEquals(30_000L, defaultPlayThresholdMs(240_000))
        assertEquals(20_000L, defaultPlayThresholdMs(40_000))
        assertEquals(5_000L, defaultPlayThresholdMs(6_000))
        assertEquals(30_000L, defaultPlayThresholdMs(0))
    }

    @Test
    fun countsOncePerStartAfterEnoughRealPlayTime() {
        val t = PlayCountTracker()
        t.onItemStarted("a:1")
        assertNull(t.addPlayed(10_000, 240_000))
        assertNull(t.addPlayed(10_000, 240_000))
        assertEquals("a:1", t.addPlayed(10_000, 240_000))
        assertNull(t.addPlayed(60_000, 240_000))
    }

    @Test
    fun repeatOfSameItemCountsAgain() {
        val t = PlayCountTracker()
        t.onItemStarted("a:1")
        assertEquals("a:1", t.addPlayed(30_000, 240_000))
        t.onItemStarted("a:1")
        assertEquals("a:1", t.addPlayed(30_000, 240_000))
    }

    @Test
    fun skippingEarlyDoesNotCount() {
        val t = PlayCountTracker()
        t.onItemStarted("a:1")
        assertNull(t.addPlayed(4_000, 240_000))
        t.onItemStarted("a:2")
        assertNull(t.addPlayed(4_000, 240_000))
    }

    @Test
    fun ignoresTimeWithNoItemOrNonPositiveDelta() {
        val t = PlayCountTracker()
        assertNull(t.addPlayed(60_000, 240_000))
        t.onItemStarted("a:1")
        assertNull(t.addPlayed(0, 240_000))
        assertNull(t.addPlayed(-5, 240_000))
        t.onItemStarted(null)
        assertNull(t.addPlayed(60_000, 240_000))
    }
}
