package com.kingplay.core.model.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SketchTest {
    @Test
    fun sameSeedGivesSameOutline() {
        val a = Sketch.roundRect(200f, 80f, 20f, seed = 7, wobble = 1.5f, step = 16f)
        val b = Sketch.roundRect(200f, 80f, 20f, seed = 7, wobble = 1.5f, step = 16f)
        assertTrue(a.contentEquals(b))
    }

    @Test
    fun differentSeedsWobbleDifferently() {
        val a = Sketch.roundRect(200f, 80f, 20f, seed = 1, wobble = 1.5f, step = 16f)
        val b = Sketch.roundRect(200f, 80f, 20f, seed = 2, wobble = 1.5f, step = 16f)
        assertTrue(!a.contentEquals(b))
    }

    @Test
    fun outlineStaysNearTheBox() {
        val w = 120f
        val h = 40f
        val pts = Sketch.roundRect(w, h, 12f, seed = 3, wobble = 2f, step = 10f)
        assertEquals(0, pts.size % 2)
        assertTrue(pts.size >= 6)
        for (i in pts.indices step 2) {
            assertTrue(pts[i] in -4f..(w + 4f))
            assertTrue(pts[i + 1] in -4f..(h + 4f))
        }
    }

    @Test
    fun tinyOrZeroSizesDoNotCrash() {
        assertTrue(Sketch.roundRect(0f, 0f, 50f, 1, 1f, 8f).isNotEmpty())
        assertTrue(Sketch.roundRect(3f, 400f, 50f, 1, 1f, 8f).isNotEmpty())
    }

    @Test
    fun lineEndsStayCloseToTheirTargets() {
        val p = Sketch.line(0f, 0f, 100f, 0f, seed = 5, wobble = 2f, step = 10f)
        assertEquals(0f, p[0], 1f)
        assertEquals(100f, p[p.size - 2], 1f)
    }

    @Test
    fun randomStaysInRange() {
        val r = Sketch.Random(99)
        repeat(1000) { val v = r.next(); assertTrue(v >= 0f && v < 1f) }
    }
}
