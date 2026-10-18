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
