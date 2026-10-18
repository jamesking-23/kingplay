package com.kingplay.feature.library.logic

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.queue.queueWindow
import kotlin.random.Random

/** What to hand the player: the queue and which item starts. */
data class PlayPlan(val refs: List<MediaRef>, val startIndex: Int)

/**
 * Queue for "the user tapped [tappedId] in a list whose song ids are [ids]". Long lists are windowed around the tapped
 * song. If the song is no longer in the list (the library changed under the tap) it plays alone rather than not at all.
 */
fun planPlayFrom(ids: List<Long>, tappedId: Long): PlayPlan {
    val index = ids.indexOf(tappedId)
    if (index < 0) return PlayPlan(listOf(MediaRef.Audio(tappedId)), 0)
    val window = queueWindow(ids, index)
    return PlayPlan(window.items.map { MediaRef.Audio(it) }, window.startIndex)
}

/** Queue for "shuffle all": starts at a random song and relies on the player's shuffle mode for the order. */
fun planShuffle(ids: List<Long>, random: Random = Random.Default): PlayPlan? {
    if (ids.isEmpty()) return null
    val window = queueWindow(ids, random.nextInt(ids.size))
    return PlayPlan(window.items.map { MediaRef.Audio(it) }, window.startIndex)
}
