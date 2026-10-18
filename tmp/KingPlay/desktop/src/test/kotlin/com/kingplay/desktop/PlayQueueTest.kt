package com.kingplay.desktop

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.RepeatMode
import com.kingplay.desktop.queue.PlayQueue
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun refs(n: Int) = List(n) { MediaRef.Audio(it.toLong() + 1) }

class PlayQueueTest {
    @Test fun nextWalksForwardThenStopsWhenRepeatIsOff() {
        var q = PlayQueue().replace(refs(3), 0)
        assertEquals(1, q.nextIndex(auto = true))
        q = q.jumpTo(2)
        assertNull(q.nextIndex(auto = true))
        assertNull(q.nextIndex(auto = false))
    }

    @Test fun repeatAllWrapsAround() {
        val q = PlayQueue().replace(refs(3), 2).withRepeat(RepeatMode.ALL)
        assertEquals(0, q.nextIndex(auto = true))
        assertEquals(2, PlayQueue().replace(refs(3), 0).withRepeat(RepeatMode.ALL).previousIndex())
    }

    @Test fun repeatOneOnlyAppliesWhenTheSongEndsByItself() {
        val q = PlayQueue().replace(refs(3), 1).withRepeat(RepeatMode.ONE)
        assertEquals(1, q.nextIndex(auto = true))
        assertEquals(2, q.nextIndex(auto = false))
    }

    @Test fun previousAtTheStartMeansRestart() {
        assertNull(PlayQueue().replace(refs(3), 0).previousIndex())
        assertEquals(0, PlayQueue().replace(refs(3), 1).previousIndex())
    }

    @Test fun emptyQueueGoesNowhere() {
        val q = PlayQueue()
        assertNull(q.nextIndex(true))
        assertNull(q.previousIndex())
        assertNull(q.current)
    }

    @Test fun shuffleKeepsCurrentSongAndPlaysEverythingOnce() {
        var q = PlayQueue().replace(refs(6), 3).withShuffle(true, Random(7))
        assertEquals(3, q.index)
        val seen = mutableListOf(q.index)
        while (true) {
            val next = q.nextIndex(auto = true) ?: break
            q = q.jumpTo(next)
            seen += next
        }
        assertEquals(6, seen.size)
        assertEquals((0..5).toSet(), seen.toSet())
        assertEquals(3, seen.first())
    }

    @Test fun shuffleOffKeepsCurrentSongAndRestoresOrder() {
        val q = PlayQueue().replace(refs(5), 2).withShuffle(true, Random(1))
        val moved = q.jumpTo(q.nextIndex(true)!!)
        val off = moved.withShuffle(false)
        assertEquals(moved.index, off.index)
        assertEquals((moved.index + 1).takeIf { it < 5 }, off.nextIndex(true))
    }

    @Test fun insertNextPutsSongRightAfterCurrent() {
        val q = PlayQueue().replace(refs(3), 1).insertNext(listOf(MediaRef.Audio(99)))
        assertEquals(1, q.index)
        assertEquals(MediaRef.Audio(99), q.items[2])
        assertEquals(2, q.nextIndex(false))
    }

    @Test fun insertNextInShuffleIsReallyNext() {
        val q = PlayQueue().replace(refs(5), 0).withShuffle(true, Random(3)).insertNext(listOf(MediaRef.Audio(99)))
        assertEquals(MediaRef.Audio(99), q.items[q.nextIndex(false)!!])
    }

    @Test fun insertBeforeCurrentShiftsIndex() {
        val q = PlayQueue().replace(refs(3), 2).insertNext(emptyList())
        assertEquals(q, PlayQueue().replace(refs(3), 2))
        val moved = PlayQueue().replace(refs(3), 2).move(2, 0)
        assertEquals(0, moved.index)
        assertEquals(MediaRef.Audio(3), moved.current)
    }

    @Test fun appendKeepsCurrent() {
        val q = PlayQueue().replace(refs(2), 1).append(refs(2))
        assertEquals(4, q.items.size)
        assertEquals(1, q.index)
    }

    @Test fun appendToEmptyStartsAtFirst() {
        val q = PlayQueue().append(refs(2))
        assertEquals(0, q.index)
    }

    @Test fun removeBeforeCurrentShiftsIndexDown() {
        val r = PlayQueue().replace(refs(4), 2).remove(0)
        assertEquals(1, r.queue.index)
        assertEquals(MediaRef.Audio(3), r.queue.current)
        assertFalse(r.removedCurrent)
    }

    @Test fun removeCurrentPicksTheSongThatTakesItsPlace() {
        val r = PlayQueue().replace(refs(3), 1).remove(1)
        assertTrue(r.removedCurrent)
        assertEquals(MediaRef.Audio(3), r.queue.current)
        val last = PlayQueue().replace(refs(3), 2).remove(2)
        assertEquals(MediaRef.Audio(2), last.queue.current)
    }

    @Test fun removeLastLeavesEmptyQueueWithSettings() {
        val r = PlayQueue().replace(refs(1), 0).withRepeat(RepeatMode.ALL).remove(0)
        assertTrue(r.queue.items.isEmpty())
        assertEquals(-1, r.queue.index)
        assertEquals(RepeatMode.ALL, r.queue.repeat)
    }

    @Test fun removeOutOfRangeChangesNothing() {
        val q = PlayQueue().replace(refs(2), 0)
        assertEquals(q, q.remove(5).queue)
    }

    @Test fun removeInShuffleKeepsEverySongPlayableOnce() {
        var q = PlayQueue().replace(refs(5), 0).withShuffle(true, Random(5)).remove(3).queue
        val seen = mutableSetOf(q.index)
        while (true) {
            q = q.jumpTo(q.nextIndex(true) ?: break)
            seen += q.index
        }
        assertEquals(4, seen.size)
    }

    @Test fun moveKeepsPlayingSong() {
        val q = PlayQueue().replace(refs(4), 1).move(0, 3)
        assertEquals(MediaRef.Audio(2), q.current)
        assertEquals(listOf(2L, 3L, 4L, 1L), q.items.map { it.id })
    }

    @Test fun replaceClampsStartAndClearKeepsModes() {
        val q = PlayQueue().withRepeat(RepeatMode.ONE).replace(refs(3), 99)
        assertEquals(2, q.index)
        val cleared = q.clear()
        assertTrue(cleared.items.isEmpty())
        assertEquals(RepeatMode.ONE, cleared.repeat)
    }
}
