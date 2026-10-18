package com.kingplay.desktop.playback

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.RepeatMode
import com.kingplay.core.model.Track
import com.kingplay.core.model.repository.PlaybackController
import com.kingplay.core.model.repository.PlaybackState
import com.kingplay.desktop.engine.AudioEngine
import com.kingplay.desktop.engine.EngineEvent
import com.kingplay.desktop.queue.PlayQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The desktop implementation of the same [PlaybackController] the Android app uses, so shared screens and
 * logic can talk to either. Only audio is handled here for now; video items are skipped.
 */
class DesktopPlaybackController(
    private val engine: AudioEngine,
    private val lookup: (Long) -> Track?,
    scope: CoroutineScope,
    /** Called once per play of a song: after half of it (or 4 minutes) has played, or when it ends. */
    private val onCounted: (Track) -> Unit = {},
) : PlaybackController {

    private val _state = MutableStateFlow(PlaybackState(isConnected = true))
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var queue = PlayQueue()

    /** False after [restore] until the person presses play: nothing is loaded into the engine before that. */
    private var engineLoaded = false
    private var resumeAtMs = 0L
    private var counted = false

    init {
        scope.launch { engine.events.collect(::onEngineEvent) }
    }

    override fun connect() = Unit

    override fun playMedia(refs: List<MediaRef>, startIndex: Int) {
        val audio = refs.filterIsInstance<MediaRef.Audio>()
        if (audio.isEmpty()) return
        // Dropping video items shifts the start position: find the requested item again by identity.
        val wanted = refs.getOrNull(startIndex)
        val start = audio.indexOf(wanted).takeIf { it >= 0 } ?: 0
        queue = queue.replace(audio, start)
        loadCurrent(direction = 1)
    }

    override fun play() {
        if (queue.current == null) return
        if (!engineLoaded) {
            val resume = resumeAtMs
            loadCurrent(direction = 1, resumeAtMs = resume)
        } else {
            engine.play()
        }
    }

    /** Puts a saved queue back without starting it. The first play loads the song and seeks to [positionMs]. */
    fun restore(refs: List<MediaRef>, index: Int, positionMs: Long, shuffle: Boolean, repeat: RepeatMode) {
        val audio = refs.filterIsInstance<MediaRef.Audio>()
        if (audio.isEmpty()) return
        queue = PlayQueue(shuffle = false, repeat = repeat).replace(audio, index).withShuffle(shuffle)
        engineLoaded = false
        resumeAtMs = positionMs.coerceAtLeast(0)
        val track = currentTrack()
        publishQueue(track)
        _state.update { it.copy(positionMs = resumeAtMs, durationMs = track?.durationMs ?: 0) }
    }

    /** What to save so [restore] can bring the queue back. */
    fun snapshot(): QueueSnapshot? {
        val ids = queue.items.filterIsInstance<MediaRef.Audio>().map { it.id }
        if (ids.isEmpty()) return null
        val position = if (engineLoaded) _state.value.positionMs else resumeAtMs
        return QueueSnapshot(ids, queue.index, position, queue.shuffle, queue.repeat)
    }

    data class QueueSnapshot(
        val trackIds: List<Long>,
        val index: Int,
        val positionMs: Long,
        val shuffle: Boolean,
        val repeat: RepeatMode,
    )

    override fun pause() = engine.pause()

    override fun togglePlayPause() {
        if (_state.value.isPlaying) pause() else play()
    }

    override fun skipToNext() {
        val next = queue.nextIndex(auto = false) ?: return
        queue = queue.jumpTo(next)
        loadCurrent(direction = 1)
    }

    override fun skipToPrevious() {
        val previous = queue.previousIndex()
        if (_state.value.positionMs > RESTART_AFTER_MS || previous == null) {
            engine.seekTo(0)
            _state.update { it.copy(positionMs = 0) }
            return
        }
        queue = queue.jumpTo(previous)
        loadCurrent(direction = -1)
    }

    override fun seekTo(positionMs: Long) {
        val limit = _state.value.durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE
        val target = positionMs.coerceIn(0, limit)
        engine.seekTo(target)
        _state.update { it.copy(positionMs = target) }
    }

    override fun skipToQueueIndex(index: Int) {
        if (index !in queue.items.indices) return
        queue = queue.jumpTo(index)
        loadCurrent(direction = 1)
    }

    override fun playNext(refs: List<MediaRef>) {
        val wasEmpty = queue.items.isEmpty()
        queue = queue.insertNext(refs.filterIsInstance<MediaRef.Audio>())
        if (wasEmpty) loadCurrent(direction = 1) else publishQueue()
    }

    override fun addToQueue(refs: List<MediaRef>) {
        val wasEmpty = queue.items.isEmpty()
        queue = queue.append(refs.filterIsInstance<MediaRef.Audio>())
        if (wasEmpty) loadCurrent(direction = 1) else publishQueue()
    }

    override fun removeFromQueue(index: Int) {
        val removal = queue.remove(index)
        queue = removal.queue
        when {
            queue.items.isEmpty() -> {
                engine.pause()
                _state.value = idleState()
            }
            removal.removedCurrent -> loadCurrent(direction = 1)
            else -> publishQueue()
        }
    }

    override fun moveInQueue(from: Int, to: Int) {
        queue = queue.move(from, to)
        publishQueue()
    }

    override fun clearQueue() {
        queue = queue.clear()
        engine.pause()
        _state.value = idleState()
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        queue = queue.withShuffle(enabled)
        publishQueue()
    }

    override fun setRepeatMode(mode: RepeatMode) {
        queue = queue.withRepeat(mode)
        publishQueue()
    }

    fun setVolume(percent: Int) = engine.setVolume(percent)

    private fun onEngineEvent(event: EngineEvent) {
        when (event) {
            is EngineEvent.State -> {
                if (event.isPlaying) {
                    failuresInARow = 0
                    if (pendingSeekMs > 0) {
                        engine.seekTo(pendingSeekMs)
                        pendingSeekMs = 0
                    }
                }
                _state.update { it.copy(isPlaying = event.isPlaying, isBuffering = event.isBuffering) }
            }
            is EngineEvent.Position -> {
                _state.update { it.copy(positionMs = event.ms) }
                countIfDue(event.ms)
            }
            is EngineEvent.Duration -> _state.update { it.copy(durationMs = event.ms) }
            is EngineEvent.Failed -> onFailed(event.message)
            EngineEvent.Ended -> onEnded()
        }
    }

    private fun countIfDue(positionMs: Long) {
        val duration = _state.value.durationMs
        if (!counted && duration > 0 && positionMs >= minOf(duration / 2, COUNT_AFTER_MS)) countNow()
    }

    private fun countNow() {
        if (counted) return
        counted = true
        currentTrack()?.let(onCounted)
    }

    private var pendingSeekMs = 0L

    private fun onEnded() {
        countNow()
        val next = queue.nextIndex(auto = true)
        if (next == null) {
            _state.update { it.copy(isPlaying = false, positionMs = 0) }
            return
        }
        queue = queue.jumpTo(next)
        loadCurrent(direction = 1)
    }

    /** A file that will not play must not stop the music: say so, then move on (but never loop over a bad queue). */
    private fun onFailed(message: String) {
        _state.update { it.copy(error = message, isPlaying = false) }
        val next = queue.nextIndex(auto = false) ?: return
        if (failuresInARow++ >= queue.items.size) return
        queue = queue.jumpTo(next)
        loadCurrent(direction = 1)
    }

    private var failuresInARow = 0

    /** Loads the current item; items that are gone from the library are skipped in [direction]. */
    private fun loadCurrent(direction: Int, resumeAtMs: Long = 0) {
        var attempts = 0
        while (attempts < queue.items.size) {
            val track = currentTrack()
            if (track != null) {
                publishQueue(track)
                engineLoaded = true
                counted = false
                pendingSeekMs = resumeAtMs
                this.resumeAtMs = 0
                engine.load(track.uri)
                return
            }
            val next = if (direction >= 0) queue.nextIndex(auto = false) else queue.previousIndex()
            if (next == null) break
            queue = queue.jumpTo(next)
            attempts++
        }
        engine.pause()
        _state.value = idleState().copy(error = MISSING_MESSAGE)
    }

    private fun currentTrack(): Track? = (queue.current as? MediaRef.Audio)?.let { lookup(it.id) }

    private fun publishQueue(track: Track? = currentTrack()) {
        _state.update {
            it.copy(
                queue = queue.items,
                currentIndex = queue.index,
                title = track?.title,
                artist = track?.artistName,
                album = track?.albumTitle,
                shuffleEnabled = queue.shuffle,
                repeatMode = queue.repeat,
                positionMs = if (track != null && track.id != (it.current as? MediaRef.Audio)?.id) 0 else it.positionMs,
                durationMs = if (track != null && track.id != (it.current as? MediaRef.Audio)?.id) {
                    track.durationMs
                } else {
                    it.durationMs
                },
                error = null,
            )
        }
    }

    private fun idleState() = PlaybackState(
        isConnected = true,
        shuffleEnabled = queue.shuffle,
        repeatMode = queue.repeat,
    )

    private companion object {
        const val RESTART_AFTER_MS = 3_000L
        const val COUNT_AFTER_MS = 240_000L
        const val MISSING_MESSAGE = "These songs are no longer in your library"
    }
}
