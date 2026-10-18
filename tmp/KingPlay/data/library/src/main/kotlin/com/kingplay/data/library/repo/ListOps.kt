package com.kingplay.data.library.repo

/** Moves the item at [from] so it ends up at index [to]. Out-of-range indices return the list unchanged. */
fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> {
    if (from !in list.indices || to !in list.indices || from == to) return list
    val mutable = list.toMutableList()
    mutable.add(to, mutable.removeAt(from))
    return mutable
}

fun <T> removeItem(list: List<T>, index: Int): List<T> =
    if (index in list.indices) list.filterIndexed { i, _ -> i != index } else list

private const val RESUME_END_THRESHOLD_MS = 5_000L

/** Position to store as the resume point. Near the end (or unknown duration at zero) means "finished": 0. */
fun normalizedResumePosition(positionMs: Long, durationMs: Long): Long = when {
    positionMs <= 0 -> 0
    durationMs > 0 && positionMs >= durationMs - RESUME_END_THRESHOLD_MS -> 0
    else -> positionMs
}
