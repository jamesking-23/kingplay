package com.kingplay.feature.library.logic

import com.kingplay.core.model.MediaRef
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayPlanTest {
    @Test
    fun tappedSongStartsInsideTheFullList() {
        val plan = planPlayFrom(listOf(10, 20, 30), 20)
        assertEquals(listOf(10L, 20L, 30L).map(MediaRef::Audio), plan.refs)
        assertEquals(1, plan.startIndex)
    }

    @Test
    fun missingSongPlaysAlone() {
        val plan = planPlayFrom(listOf(10, 20, 30), 99)
        assertEquals(listOf<MediaRef>(MediaRef.Audio(99)), plan.refs)
        assertEquals(0, plan.startIndex)
    }

    @Test
    fun longListIsWindowedButKeepsTappedSong() {
        val ids = (1L..10_000L).toList()
        val plan = planPlayFrom(ids, 5_000)
        assertEquals(2_000, plan.refs.size)
        assertEquals(MediaRef.Audio(5_000), plan.refs[plan.startIndex])
    }

    @Test
    fun shuffleOfEmptyIsNull() {
        assertNull(planShuffle(emptyList()))
    }

    @Test
    fun shuffleStartsOnARealSong() {
        val ids = (1L..50L).toList()
        val plan = planShuffle(ids, Random(7))!!
        assertEquals(50, plan.refs.size)
        assertEquals(true, plan.refs[plan.startIndex].id in ids)
    }
}
