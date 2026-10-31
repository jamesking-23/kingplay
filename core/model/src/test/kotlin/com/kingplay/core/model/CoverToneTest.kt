package com.kingplay.core.model

import com.kingplay.core.model.theme.CoverTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverToneTest {
    private fun argb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    private fun hue(c: Int) = CoverTone.toHsl((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF)[0]

    @Test fun aMostlyBluePictureGivesBlue() {
        val pixels = IntArray(100) { if (it < 70) argb(20, 60, 220) else argb(230, 230, 230) }
        val c = CoverTone.dominant(pixels)
        assertNotNull(c)
        assertTrue("hue ${hue(c!!)}", hue(c) in 200f..250f)
    }

    @Test fun greyBlackAndWhiteGiveNoColour() {
        assertNull(CoverTone.dominant(IntArray(50) { argb(128, 128, 128) }))
        assertNull(CoverTone.dominant(IntArray(50) { argb(0, 0, 0) }))
        assertNull(CoverTone.dominant(IntArray(50) { argb(255, 255, 255) }))
    }

    @Test fun tuningKeepsTheHueAndMovesLightness() {
        val blue = argb(20, 60, 220)
        val dark = CoverTone.tune(blue, dark = true)
        val light = CoverTone.tune(blue, dark = false)
        assertEquals(hue(blue), hue(dark), 3f)
        assertEquals(hue(blue), hue(light), 3f)
        val ld = CoverTone.toHsl((dark shr 16) and 0xFF, (dark shr 8) and 0xFF, dark and 0xFF)[2]
        val ll = CoverTone.toHsl((light shr 16) and 0xFF, (light shr 8) and 0xFF, light and 0xFF)[2]
        assertTrue(ld > ll)
    }

    @Test fun greyishColoursGetEnoughSaturationToShow() {
        val dull = argb(120, 130, 140)
        val tuned = CoverTone.tune(dull, dark = true)
        assertTrue(CoverTone.toHsl((tuned shr 16) and 0xFF, (tuned shr 8) and 0xFF, tuned and 0xFF)[1] >= 0.44f)
    }
}
