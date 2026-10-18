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
