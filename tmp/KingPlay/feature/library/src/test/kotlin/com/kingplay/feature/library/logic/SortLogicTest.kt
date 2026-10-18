package com.kingplay.feature.library.logic

import com.kingplay.core.model.SongSort
import com.kingplay.core.model.SongSortField
import org.junit.Assert.assertEquals
import org.junit.Test

class SortLogicTest {
    @Test
    fun samefieldFlipsDirection() {
        val flipped = SongSort(SongSortField.TITLE, true).chooseField(SongSortField.TITLE)
        assertEquals(SongSort(SongSortField.TITLE, false), flipped)
        assertEquals(SongSort(SongSortField.TITLE, true), flipped.chooseField(SongSortField.TITLE))
    }

    @Test
    fun newFieldStartsAscending() {
        assertEquals(SongSort(SongSortField.ARTIST, true), SongSort().chooseField(SongSortField.ARTIST))
        assertEquals(SongSort(SongSortField.DURATION, true), SongSort().chooseField(SongSortField.DURATION))
    }

    @Test
    fun dateAddedAndPlayCountStartDescending() {
        assertEquals(SongSort(SongSortField.DATE_ADDED, false), SongSort().chooseField(SongSortField.DATE_ADDED))
        assertEquals(SongSort(SongSortField.PLAY_COUNT, false), SongSort().chooseField(SongSortField.PLAY_COUNT))
    }
}
