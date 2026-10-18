package com.kingplay.core.model.queue

/** A slice of a long list that becomes the play queue, and where the tapped item sits inside it. */
data class QueueWindow<T>(val items: List<T>, val startIndex: Int)

/**
 * Playing from a 50,000-song list must not put 50,000 items in the player. This keeps up to [before] items
 * before the tapped one and [after] items from it on, so previous/next still work around the tapped song.
 * Lists that fit are used whole.
 */
fun <T> queueWindow(
    all: List<T>,
    tappedIndex: Int,
    before: Int = DEFAULT_BEFORE,
    after: Int = DEFAULT_AFTER,
): QueueWindow<T> {
    require(before >= 0 && after > 0) { "window sizes must be positive" }
    if (all.isEmpty()) return QueueWindow(emptyList(), 0)
    val tapped = tappedIndex.coerceIn(0, all.lastIndex)
    if (all.size <= before + after) return QueueWindow(all, tapped)
    val from = (tapped - before).coerceAtLeast(0)
    val to = (tapped + after).coerceAtMost(all.size)
    return QueueWindow(all.subList(from, to), tapped - from)
}

const val DEFAULT_BEFORE = 100
const val DEFAULT_AFTER = 1_900
