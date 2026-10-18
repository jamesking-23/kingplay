package com.kingplay.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelTest {
    @Test(expected = IllegalArgumentException::class)
    fun eqPreset_requiresTenBands() {
        EqPreset(id = 1, name = "Bad", preampDb = 0f, bandGainsDb = listOf(0f), isBuiltIn = false)
    }

    @Test
    fun flatPreset_hasTenZeroBands() {
        assertEquals(EqPreset.BAND_COUNT, EqPreset.Flat.bandGainsDb.size)
        assertEquals(0f, EqPreset.Flat.bandGainsDb.sum(), 0f)
    }

    @Test
    fun queueState_currentIsNullWhenIndexInvalid() {
        assertNull(QueueState().current)
        val state = QueueState(items = listOf(MediaRef.Audio(7)), currentIndex = 0)
        assertEquals(MediaRef.Audio(7), state.current)
    }
}

class LibraryModelTest {
    @Test
    fun builtInPresetsAreValid() {
        val ids = BuiltInEqPresets.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        for (p in BuiltInEqPresets.all) {
            assertEquals(EqPreset.BAND_COUNT, p.bandGainsDb.size)
            assertEquals(true, p.isBuiltIn)
            assertEquals(true, p.id <= 0)
            assertEquals(true, p.bandGainsDb.all { it in EqPreset.MIN_GAIN_DB..EqPreset.MAX_GAIN_DB })
        }
        assertEquals(EqPreset.Flat, BuiltInEqPresets.byId(0))
    }

    @Test
    fun scanFractionIsClamped() {
        assertEquals(0f, ScanState.Scanning(ScanPhase.INDEXING_AUDIO, 0, 0).fraction, 0f)
        assertEquals(0.5f, ScanState.Scanning(ScanPhase.INDEXING_AUDIO, 5, 10).fraction, 0f)
        assertEquals(1f, ScanState.Scanning(ScanPhase.INDEXING_AUDIO, 12, 10).fraction, 0f)
    }

    @Test
    fun folderNameIsLastSegment() {
        assertEquals("Fela", MediaFolder("/storage/emulated/0/Music/Fela/", 3, 0).name)
    }

    @Test
    fun searchResultsEmptyFlag() {
        assertEquals(true, SearchResults.Empty.isEmpty)
    }
}

class QueueWindowTest {
    @Test
    fun shortListIsUsedWhole() {
        val w = com.kingplay.core.model.queue.queueWindow(listOf(1, 2, 3), 2)
        org.junit.Assert.assertEquals(listOf(1, 2, 3), w.items)
        org.junit.Assert.assertEquals(2, w.startIndex)
    }

    @Test
    fun emptyList() {
        val w = com.kingplay.core.model.queue.queueWindow(emptyList<Int>(), 5)
        org.junit.Assert.assertTrue(w.items.isEmpty())
        org.junit.Assert.assertEquals(0, w.startIndex)
    }

    @Test
    fun longListIsWindowedAroundTappedItem() {
        val all = (0 until 10_000).toList()
        val w = com.kingplay.core.model.queue.queueWindow(all, 5_000, before = 10, after = 20)
        org.junit.Assert.assertEquals(30, w.items.size)
        org.junit.Assert.assertEquals(5_000, w.items[w.startIndex])
        org.junit.Assert.assertEquals(10, w.startIndex)
    }

    @Test
    fun windowNearTheStartAndEnd() {
        val all = (0 until 10_000).toList()
        val start = com.kingplay.core.model.queue.queueWindow(all, 3, before = 10, after = 20)
        org.junit.Assert.assertEquals(3, start.items[start.startIndex])
        org.junit.Assert.assertEquals(0, start.items.first())
        val end = com.kingplay.core.model.queue.queueWindow(all, 9_995, before = 10, after = 20)
        org.junit.Assert.assertEquals(9_995, end.items[end.startIndex])
        org.junit.Assert.assertEquals(9_999, end.items.last())
    }

    @Test
    fun tappedIndexIsClamped() {
        val w = com.kingplay.core.model.queue.queueWindow(listOf(1, 2, 3), 99)
        org.junit.Assert.assertEquals(2, w.startIndex)
    }
}
