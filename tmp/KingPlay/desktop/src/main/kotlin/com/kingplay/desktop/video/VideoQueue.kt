package com.kingplay.desktop.video

/** The videos lined up to play one after another. [index] is the one on screen. */
data class VideoQueue(val items: List<VideoFile>, val index: Int = 0) {
    val current: VideoFile? get() = items.getOrNull(index)
    val hasNext: Boolean get() = index + 1 < items.size
    val hasPrevious: Boolean get() = index > 0

    fun next(): VideoQueue? = if (hasNext) copy(index = index + 1) else null

    fun previous(): VideoQueue? = if (hasPrevious) copy(index = index - 1) else null

    companion object {
        /** A queue of [all] that starts at [first]; a video that is not in the list plays alone. */
        fun startingAt(all: List<VideoFile>, first: VideoFile): VideoQueue {
            val at = all.indexOfFirst { it.id == first.id }
            return if (at >= 0) VideoQueue(all, at) else VideoQueue(listOf(first), 0)
        }
    }
}
