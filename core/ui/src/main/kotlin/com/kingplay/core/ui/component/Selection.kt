package com.kingplay.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

/** The ids a person has ticked in a list. Selecting starts with the first tick and ends when the last one is cleared. */
@Stable
class SelectionState {
    private val chosen = mutableStateListOf<Long>()

    val ids: List<Long> get() = chosen
    val active: Boolean get() = chosen.isNotEmpty()
    val count: Int get() = chosen.size

    operator fun contains(id: Long): Boolean = id in chosen

    fun toggle(id: Long) {
        if (!chosen.remove(id)) chosen.add(id)
    }

    fun clear() = chosen.clear()

    /** Ticks everything in [all], keeping the order of the list. */
    fun selectAll(all: List<Long>) {
        chosen.clear()
        chosen.addAll(all.distinct())
    }

    /** Drops ticks for items that are no longer in the list, for example after they were deleted. */
    fun retainOnly(existing: Set<Long>) {
        chosen.retainAll(existing)
    }
}

@Composable
fun rememberSelection(): SelectionState = rememberSaveable(
    saver = listSaver(
        save = { it.ids.toList() },
        restore = { saved -> SelectionState().apply { selectAll(saved) } },
    ),
) { SelectionState() }
