package com.kingplay.core.player.logic

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.QueueState
import com.kingplay.core.model.RepeatMode

/**
 * Builds the persisted queue from the player's media ids. Ids that cannot be parsed are dropped and the current
 * index is moved so it still points at the same item. The shuffle order is not stored: the player reshuffles on
 * restore, which is what users expect after restarting the app.
 */
fun snapshotOf(
    mediaIds: List<String>,
    currentIndex: Int,
    positionMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
): QueueState {
    val refs = ArrayList<MediaRef>(mediaIds.size)
    var mapped = -1
    mediaIds.forEachIndexed { index, id ->
        val ref = MediaIds.parse(id) ?: return@forEachIndexed
        if (index == currentIndex) mapped = refs.size
        refs += ref
    }
    return QueueState(
        items = refs,
        currentIndex = mapped,
        positionMs = if (mapped >= 0) positionMs.coerceAtLeast(0) else 0,
        shuffleEnabled = shuffleEnabled,
        shuffleOrder = emptyList(),
        repeatMode = repeatMode,
    )
}

/**
 * Drops items whose file is gone (deleted, card removed) before restoring. The current index follows its item;
 * if the current item itself is gone, playback resumes at the item that took its place, from the start.
 */
fun pruneQueue(state: QueueState, exists: (MediaRef) -> Boolean): QueueState {
    if (state.items.isEmpty()) return state
    val kept = ArrayList<MediaRef>(state.items.size)
    var newIndex = -1
    var currentSurvived = false
    state.items.forEachIndexed { index, ref ->
        val keep = exists(ref)
        if (index == state.currentIndex) {
            newIndex = kept.size
            currentSurvived = keep
        }
        if (keep) kept += ref
    }
    if (kept.isEmpty()) return QueueState(shuffleEnabled = state.shuffleEnabled, repeatMode = state.repeatMode)
    val index = when {
        state.currentIndex !in state.items.indices -> -1
        newIndex >= kept.size -> kept.lastIndex
        else -> newIndex
    }
    return state.copy(
        items = kept,
        currentIndex = index,
        positionMs = if (currentSurvived) state.positionMs else 0,
        shuffleOrder = emptyList(),
    )
}

/** Index to insert at for "play next": right after the current item, or the end when nothing is playing. */
fun playNextInsertIndex(currentIndex: Int, queueSize: Int): Int =
    if (currentIndex in 0 until queueSize) currentIndex + 1 else queueSize
