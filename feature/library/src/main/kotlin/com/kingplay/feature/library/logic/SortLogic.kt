package com.kingplay.feature.library.logic

import com.kingplay.core.model.SongSort
import com.kingplay.core.model.SongSortField

/**
 * Same field flips the direction. A new field starts ascending, except date added and play count, where
 * newest and most played first is what people want.
 */
fun SongSort.chooseField(field: SongSortField): SongSort =
    if (field == this.field) {
        copy(ascending = !ascending)
    } else {
        SongSort(field = field, ascending = field != SongSortField.DATE_ADDED && field != SongSortField.PLAY_COUNT)
    }
