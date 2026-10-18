package com.kingplay.core.ui.artwork

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkSamplingTest {
    @Test
    fun smallImagesAreNotDownsampled() {
        assertEquals(1, sampleSize(300, 300, 384))
        assertEquals(1, sampleSize(384, 384, 384))
    }

    @Test
    fun largeImagesAreHalvedWhileStillBigEnough() {
        assertEquals(2, sampleSize(1_000, 1_000, 384))
        assertEquals(4, sampleSize(1_600, 1_600, 384))
        assertEquals(4, sampleSize(3_000, 3_000, 384))
        assertEquals(8, sampleSize(3_100, 3_100, 384))
    }

    @Test
    fun theShorterSideDecides() {
        assertEquals(1, sampleSize(4_000, 500, 384))
    }

    @Test
    fun invalidSizesFallBackToOne() {
        assertEquals(1, sampleSize(0, 100, 50))
        assertEquals(1, sampleSize(100, 100, 0))
    }
}
