package com.kingplay.feature.library.logic

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.PlaylistEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistPositionsTest {
    private fun audio(position: Int, id: Long) = PlaylistEntry(1, position, MediaRef.Audio(id))

    @Test
    fun missingEntriesShiftRowIndexes() {
        // Entry 1 (track 99) has no file, so rows are tracks 10, 20, 30 at positions 0, 2, 3.
        val entries = listOf(audio(0, 10), audio(1, 99), audio(2, 20), audio(3, 30))
        val rows = listOf(10L, 20L, 30L)
        assertEquals(0, entryPositionFor(entries, rows, 0))
        assertEquals(2, entryPositionFor(entries, rows, 1))
        assertEquals(3, entryPositionFor(entries, rows, 2))
    }

    @Test
    fun duplicateTracksMapToTheRightOccurrence() {
        val entries = listOf(audio(0, 10), audio(1, 20), audio(2, 10))
        val rows = listOf(10L, 20L, 10L)
        assertEquals(0, entryPositionFor(entries, rows, 0))
        assertEquals(2, entryPositionFor(entries, rows, 2))
    }

    @Test
    fun outOfRangeIsNull() {
        assertNull(entryPositionFor(listOf(audio(0, 10)), listOf(10L), 5))
    }

    @Test
    fun videoEntriesAreIgnored() {
        val entries = listOf(PlaylistEntry(1, 0, MediaRef.Video(10)), audio(1, 10))
        assertEquals(1, entryPositionFor(entries, listOf(10L), 0))
    }
}
