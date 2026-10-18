package com.kingplay.feature.nowplaying.stage

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LrcFileTest {
    @Test
    fun readsUtf8Text() {
        val text = "[00:01.00]héllo 😀"
        assertEquals(text, readLimited(ByteArrayInputStream(text.toByteArray()), 1_000))
    }

    @Test
    fun refusesFilesThatAreTooBigToBeLyrics() {
        assertNull(readLimited(ByteArrayInputStream(ByteArray(2_000)), 1_000))
    }

    @Test
    fun aFileExactlyAtTheLimitIsAccepted() {
        assertEquals("a".repeat(1_000), readLimited(ByteArrayInputStream("a".repeat(1_000).toByteArray()), 1_000))
    }
}
