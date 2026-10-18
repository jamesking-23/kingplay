package com.kingplay.core.player.service

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.QueueState
import com.kingplay.core.model.RepeatMode
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.QueueRepository
import com.kingplay.core.model.repository.VideoRepository
import com.kingplay.core.player.logic.MediaIds
import com.kingplay.core.player.logic.PlayCountTracker
import com.kingplay.core.player.logic.pruneQueue
import com.kingplay.core.player.logic.snapshotOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Keeps the saved queue and play counts in step with the player. Lives inside the playback service and runs on its
 * main-thread scope, because a Media3 player may only be touched from the main thread.
 */
internal class QueuePersistence(
    private val player: Player,
    private val queueRepository: QueueRepository,
    private val library: LibraryRepository,
    private val videos: VideoRepository,
    private val scope: CoroutineScope,
) : Player.Listener {

    private val tracker = PlayCountTracker()
    private var saveJob: Job? = null
    private var tickJob: Job? = null

    /** Loads the saved queue into the player, paused. Items whose files vanished are dropped. */
    suspend fun restore() {
        // A play button pressed before this finished may already have restored the queue (see onPlaybackResumption).
        if (player.mediaItemCount > 0) return
        val loaded = loadSavedQueue(queueRepository, library, videos) ?: return
        player.shuffleModeEnabled = loaded.shuffleEnabled
        player.repeatMode = loaded.repeatMode.toPlayerMode()
        player.setMediaItems(loaded.items, loaded.index, loaded.positionMs)
        player.prepare()
    }

    fun start() {
        player.addListener(this)
        tracker.onItemStarted(player.currentMediaItem?.mediaId)
        tickJob = scope.launch {
            var lastTick = System.currentTimeMillis()
            while (true) {
                delay(TICK_MS)
                val now = System.currentTimeMillis()
                if (player.isPlaying) {
                    onTick(now - lastTick)
                }
                lastTick = now
            }
        }
    }

    fun stop() {
        player.removeListener(this)
        tickJob?.cancel()
        saveJob?.cancel()
        // The position was already saved when playback paused; the service scope is cancelled right after this.
    }

    private var ticksSincePositionSave = 0

    private fun onTick(deltaMs: Long) {
        val counted = tracker.addPlayed(deltaMs, player.duration.coerceAtLeast(0))
        counted?.let { mediaId ->
            val ref = MediaIds.parse(mediaId)
            if (ref is MediaRef.Audio) scope.launch { library.recordPlay(ref.id, System.currentTimeMillis()) }
        }
        ticksSincePositionSave++
        if (ticksSincePositionSave >= POSITION_SAVE_EVERY_TICKS) {
            ticksSincePositionSave = 0
            scope.launch { savePositionNow() }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        tracker.onItemStarted(mediaItem?.mediaId)
        scheduleFullSave()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) = scheduleFullSave()

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = scheduleFullSave()

    override fun onRepeatModeChanged(repeatMode: Int) = scheduleFullSave()

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isPlaying) scope.launch { savePositionNow() }
    }

    /** Debounced: dragging items around in the queue fires many timeline changes. */
    private fun scheduleFullSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_DEBOUNCE_MS)
            queueRepository.save(currentSnapshot())
        }
    }

    private suspend fun savePositionNow() {
        if (player.mediaItemCount == 0) return
        queueRepository.savePosition(player.currentMediaItemIndex, player.currentPosition.coerceAtLeast(0))
    }

    private fun currentSnapshot(): QueueState = snapshotOf(
        mediaIds = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId },
        currentIndex = player.currentMediaItemIndex,
        positionMs = player.currentPosition,
        shuffleEnabled = player.shuffleModeEnabled,
        repeatMode = player.repeatMode.toRepeatMode(),
    )

    private companion object {
        const val TICK_MS = 1_000L
        const val POSITION_SAVE_EVERY_TICKS = 5
        const val SAVE_DEBOUNCE_MS = 500L
    }
}

/** The saved queue turned into playable items, ready to hand to the player. */
internal class LoadedQueue(
    val items: List<MediaItem>,
    val index: Int,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: RepeatMode,
)

/** Reads the saved queue and drops items whose files vanished. Null when there is nothing left to play. */
internal suspend fun loadSavedQueue(
    queueRepository: QueueRepository,
    library: LibraryRepository,
    videos: VideoRepository,
): LoadedQueue? {
    val saved = queueRepository.load()
    if (saved.items.isEmpty()) return null
    val audio = library.getTracks(saved.items.filterIsInstance<MediaRef.Audio>().map { it.id }).associateBy { it.id }
    val video = videos.getVideos(saved.items.filterIsInstance<MediaRef.Video>().map { it.id }).associateBy { it.id }
    val pruned = pruneQueue(saved) { ref ->
        when (ref) {
            is MediaRef.Audio -> ref.id in audio
            is MediaRef.Video -> ref.id in video
        }
    }
    val items = pruned.items.mapNotNull { ref ->
        when (ref) {
            is MediaRef.Audio -> audio[ref.id]?.toMediaItem()
            is MediaRef.Video -> video[ref.id]?.toMediaItem()
        }
    }
    if (items.isEmpty()) return null
    return LoadedQueue(
        items = items,
        index = pruned.currentIndex.coerceAtLeast(0),
        positionMs = pruned.positionMs,
        shuffleEnabled = pruned.shuffleEnabled,
        repeatMode = pruned.repeatMode,
    )
}

internal fun RepeatMode.toPlayerMode(): Int = when (this) {
    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
}

internal fun Int.toRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    else -> RepeatMode.OFF
}
