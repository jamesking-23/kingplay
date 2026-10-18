package com.kingplay.desktop.queue

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.RepeatMode
import kotlin.random.Random

/**
 * The play queue and the rules for moving through it. Immutable: every change returns a new queue, so the
 * controller can publish it as state and tests can compare results.
 *
 * With shuffle on, [order] is a permutation of item indexes in play order and the current item is at [cursor].
 */
data class PlayQueue(
    val items: List<MediaRef> = emptyList(),
    val index: Int = -1,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    private val order: List<Int> = emptyList(),
) {
    val current: MediaRef? get() = items.getOrNull(index)

    /** Position of the current item in play order. */
    private val cursor: Int get() = if (shuffle) order.indexOf(index) else index

    private fun at(position: Int): Int = if (shuffle) order[position] else position

    /** Where playback goes next. [auto] is true when the song ended by itself, false when the person pressed next. */
    fun nextIndex(auto: Boolean): Int? {
        if (items.isEmpty() || index !in items.indices) return null
        if (auto && repeat == RepeatMode.ONE) return index
        val next = cursor + 1
        return when {
            next < items.size -> at(next)
            repeat == RepeatMode.OFF -> null
            else -> at(0)
        }
    }

    /** The song before the current one; null means "restart this one" (nothing earlier and repeat is off). */
    fun previousIndex(): Int? {
        if (items.isEmpty() || index !in items.indices) return null
        val prev = cursor - 1
        return when {
            prev >= 0 -> at(prev)
            repeat == RepeatMode.ALL -> at(items.size - 1)
            else -> null
        }
    }

    fun jumpTo(target: Int): PlayQueue = if (target in items.indices) copy(index = target) else this

    fun replace(newItems: List<MediaRef>, start: Int, random: Random = Random.Default): PlayQueue {
        if (newItems.isEmpty()) return PlayQueue(shuffle = shuffle, repeat = repeat)
        val first = start.coerceIn(newItems.indices)
        return PlayQueue(
            items = newItems,
            index = first,
            shuffle = shuffle,
            repeat = repeat,
            order = if (shuffle) shuffledOrder(newItems.size, first, random) else emptyList(),
        )
    }

    fun withShuffle(enabled: Boolean, random: Random = Random.Default): PlayQueue = when {
        enabled == shuffle -> this
        !enabled -> copy(shuffle = false, order = emptyList())
        items.isEmpty() -> copy(shuffle = true)
        else -> copy(shuffle = true, order = shuffledOrder(items.size, index.coerceAtLeast(0), random))
    }

    fun withRepeat(mode: RepeatMode): PlayQueue = copy(repeat = mode)

    /** Inserts right after the current item (also in shuffle order, so it really is next). */
    fun insertNext(refs: List<MediaRef>): PlayQueue {
        if (refs.isEmpty()) return this
        if (items.isEmpty()) return replace(refs, 0)
        return insertAt(index + 1, refs, afterPosition = cursor + 1)
    }

    fun append(refs: List<MediaRef>): PlayQueue {
        if (refs.isEmpty()) return this
        if (items.isEmpty()) return replace(refs, 0)
        return insertAt(items.size, refs, afterPosition = items.size)
    }

    private fun insertAt(itemIndex: Int, refs: List<MediaRef>, afterPosition: Int): PlayQueue {
        val newItems = items.toMutableList().apply { addAll(itemIndex, refs) }
        val newIndex = if (index >= itemIndex) index + refs.size else index
        if (!shuffle) return copy(items = newItems, index = newIndex)
        val shifted = order.map { if (it >= itemIndex) it + refs.size else it }.toMutableList()
        shifted.addAll(afterPosition, List(refs.size) { itemIndex + it })
        return copy(items = newItems, index = newIndex, order = shifted)
    }

    /**
     * Removes one item. Returns the new queue; [removedCurrent] tells the controller the playing song is gone and
     * [index] already points at the one that takes its place (or -1 when the queue became empty).
     */
    fun remove(target: Int): Removal {
        if (target !in items.indices) return Removal(this, removedCurrent = false)
        val removedCurrent = target == index
        val position = cursor
        val newItems = items.toMutableList().apply { removeAt(target) }
        if (newItems.isEmpty()) return Removal(PlayQueue(shuffle = shuffle, repeat = repeat), removedCurrent)
        val newOrder = if (shuffle) {
            order.filter { it != target }.map { if (it > target) it - 1 else it }
        } else {
            emptyList()
        }
        val newIndex = when {
            !removedCurrent -> if (index > target) index - 1 else index
            shuffle -> newOrder[position.coerceAtMost(newOrder.lastIndex)]
            else -> target.coerceAtMost(newItems.lastIndex)
        }
        return Removal(copy(items = newItems, index = newIndex, order = newOrder), removedCurrent)
    }

    fun move(from: Int, to: Int): PlayQueue {
        if (from !in items.indices || to !in items.indices || from == to) return this
        val newItems = items.toMutableList().apply { add(to, removeAt(from)) }
        fun map(i: Int): Int = when {
            i == from -> to
            from < to && i in (from + 1)..to -> i - 1
            from > to && i in to until from -> i + 1
            else -> i
        }
        return copy(items = newItems, index = map(index), order = order.map(::map))
    }

    fun clear(): PlayQueue = PlayQueue(shuffle = shuffle, repeat = repeat)

    data class Removal(val queue: PlayQueue, val removedCurrent: Boolean)

    private companion object {
        /** A random order that starts with [first], so turning shuffle on never changes the playing song. */
        fun shuffledOrder(size: Int, first: Int, random: Random): List<Int> {
            val rest = (0 until size).filter { it != first }.shuffled(random)
            return listOf(first) + rest
        }
    }
}
