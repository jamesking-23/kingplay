package com.kingplay.feature.library.logic

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.Track
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.PlaybackController
import javax.inject.Inject

/** What a song row can do. Shared by the songs list and every detail screen. */
class TrackActions @Inject constructor(
    private val playback: PlaybackController,
    private val library: LibraryRepository,
) {
    fun playFrom(ids: List<Long>, tappedId: Long) {
        val plan = planPlayFrom(ids, tappedId)
        playback.playMedia(plan.refs, plan.startIndex)
    }

    fun shuffle(ids: List<Long>) {
        val plan = planShuffle(ids) ?: return
        playback.setShuffleEnabled(true)
        playback.playMedia(plan.refs, plan.startIndex)
    }

    fun playNext(track: Track) = playback.playNext(listOf(MediaRef.Audio(track.id)))

    fun addToQueue(track: Track) = playback.addToQueue(listOf(MediaRef.Audio(track.id)))

    fun playNext(tracks: List<Track>) = playback.playNext(tracks.map { MediaRef.Audio(it.id) })

    fun addToQueue(tracks: List<Track>) = playback.addToQueue(tracks.map { MediaRef.Audio(it.id) })

    /** Adds all to favorites, or removes them all when every one already is a favorite. */
    suspend fun toggleFavorites(tracks: List<Track>) {
        val makeFavorite = tracks.any { !it.isFavorite }
        tracks.forEach { library.setFavorite(it.id, makeFavorite) }
    }

    suspend fun toggleFavorite(track: Track) = library.setFavorite(track.id, !track.isFavorite)
}
