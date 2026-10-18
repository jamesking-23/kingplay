package com.kingplay.desktop

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.RepeatMode
import com.kingplay.core.model.Track
import com.kingplay.desktop.engine.AudioEngine
import com.kingplay.desktop.engine.EngineEvent
import com.kingplay.desktop.playback.DesktopPlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeEngine : AudioEngine {
    val bus = MutableSharedFlow<EngineEvent>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val events: Flow<EngineEvent> = bus
    val loaded = mutableListOf<String>()
    var paused = 0
    var played = 0
    var lastSeek = -1L

    override fun load(path: String) {
        loaded += path
    }

    override fun play() {
        played++
    }

    override fun pause() {
        paused++
    }

    override fun seekTo(positionMs: Long) {
        lastSeek = positionMs
    }

    override fun setVolume(percent: Int) = Unit

    override fun release() = Unit
}

private fun track(id: Long, durationMs: Long = 200_000) = Track(
    id = id, uri = "C:/music/$id.mp3", title = "Song $id", artistId = 1, artistName = "Artist",
    albumId = 1, albumTitle = "Album", albumArtist = null, genre = null, durationMs = durationMs,
    bitrateKbps = 0, sampleRateHz = 0, format = "mp3", trackNumber = 0, discNumber = 0, year = null,
    folderPath = "C:/music", dateAddedMs = 0, dateModifiedMs = 0,
)

class ControllerTest {
    private val engine = FakeEngine()
    private val library = (1L..4L).associateWith { track(it) }.toMutableMap()
    private val finished = mutableListOf<Long>()
    private val controller = DesktopPlaybackController(
        engine = engine,
        lookup = { library[it] },
        scope = CoroutineScope(Dispatchers.Unconfined + Job()),
        onFinished = { finished += it.id },
    )

    private fun refs(vararg ids: Long) = ids.map { MediaRef.Audio(it) }

    @Test fun playMediaLoadsTheChosenSongAndPublishesInfo() {
        controller.playMedia(refs(1, 2, 3), startIndex = 1)
        assertEquals(listOf("C:/music/2.mp3"), engine.loaded)
        val s = controller.state.value
        assertEquals("Song 2", s.title)
        assertEquals(1, s.currentIndex)
        assertEquals(200_000, s.durationMs)
        assertTrue(s.isConnected)
    }

    @Test fun videoItemsAreLeftOutAndStartPointFollowsTheChosenItem() {
        controller.playMedia(listOf(MediaRef.Video(9), MediaRef.Audio(3), MediaRef.Audio(4)), startIndex = 2)
        assertEquals(listOf("C:/music/4.mp3"), engine.loaded)
        assertEquals(2, controller.state.value.queue.size)
    }

    @Test fun onlyVideoMeansNothingHappens() {
        controller.playMedia(listOf(MediaRef.Video(9)))
        assertTrue(engine.loaded.isEmpty())
    }

    @Test fun songEndingAdvancesAndCountsAsFinished() {
        controller.playMedia(refs(1, 2))
        engine.bus.tryEmit(EngineEvent.Ended)
        assertEquals(listOf("C:/music/1.mp3", "C:/music/2.mp3"), engine.loaded)
        assertEquals(listOf(1L), finished)
    }

    @Test fun endOfQueueStopsAtTheLastSong() {
        controller.playMedia(refs(1))
        engine.bus.tryEmit(EngineEvent.State(isPlaying = true, isBuffering = false))
        engine.bus.tryEmit(EngineEvent.Ended)
        assertEquals(1, engine.loaded.size)
        assertFalse(controller.state.value.isPlaying)
        assertEquals(0, controller.state.value.currentIndex)
    }

    @Test fun repeatOneReplaysTheSameFile() {
        controller.playMedia(refs(1, 2))
        controller.setRepeatMode(RepeatMode.ONE)
        engine.bus.tryEmit(EngineEvent.Ended)
        assertEquals(listOf("C:/music/1.mp3", "C:/music/1.mp3"), engine.loaded)
    }

    @Test fun previousRestartsAfterThreeSecondsOtherwiseGoesBack() {
        controller.playMedia(refs(1, 2), startIndex = 1)
        engine.bus.tryEmit(EngineEvent.Position(5_000))
        controller.skipToPrevious()
        assertEquals(0L, engine.lastSeek)
        assertEquals(1, engine.loaded.size)
        engine.bus.tryEmit(EngineEvent.Position(1_000))
        controller.skipToPrevious()
        assertEquals("C:/music/1.mp3", engine.loaded.last())
    }

    @Test fun seekIsClampedToTheSongLength() {
        controller.playMedia(refs(1))
        controller.seekTo(999_999)
        assertEquals(200_000, engine.lastSeek)
        controller.seekTo(-5)
        assertEquals(0, engine.lastSeek)
    }

    @Test fun songsMissingFromTheLibraryAreSkipped() {
        library.remove(2)
        controller.playMedia(refs(1, 2, 3))
        engine.bus.tryEmit(EngineEvent.Ended)
        assertEquals("C:/music/3.mp3", engine.loaded.last())
    }

    @Test fun allSongsMissingGivesAMessageNotALoop() {
        library.clear()
        controller.playMedia(refs(1, 2, 3))
        assertTrue(engine.loaded.isEmpty())
        assertNotNull(controller.state.value.error)
        assertNull(controller.state.value.title)
    }

    @Test fun aBrokenFileIsReportedAndSkipped() {
        controller.playMedia(refs(1, 2))
        engine.bus.tryEmit(EngineEvent.Failed("bad"))
        assertEquals("C:/music/2.mp3", engine.loaded.last())
    }

    @Test fun aQueueOfBrokenFilesStopsInsteadOfLoopingForever() {
        controller.playMedia(refs(1, 2, 3), startIndex = 0)
        repeat(20) { engine.bus.tryEmit(EngineEvent.Failed("bad")) }
        assertTrue(engine.loaded.size <= 4)
    }

    @Test fun removingThePlayingSongLoadsTheNextOne() {
        controller.playMedia(refs(1, 2, 3), startIndex = 1)
        controller.removeFromQueue(1)
        assertEquals("C:/music/3.mp3", engine.loaded.last())
        assertEquals(2, controller.state.value.queue.size)
    }

    @Test fun clearQueueStopsAndEmptiesState() {
        controller.playMedia(refs(1, 2))
        controller.clearQueue()
        assertFalse(controller.state.value.hasMedia)
        assertTrue(engine.paused > 0)
    }

    @Test fun playNextOnAnEmptyQueueStartsPlaying() {
        controller.playNext(refs(2))
        assertEquals(listOf("C:/music/2.mp3"), engine.loaded)
    }

    @Test fun addToQueueDoesNotInterruptPlayback() {
        controller.playMedia(refs(1))
        controller.addToQueue(refs(2, 3))
        assertEquals(1, engine.loaded.size)
        assertEquals(3, controller.state.value.queue.size)
    }

    @Test fun togglePlayPauseFollowsEngineState() {
        controller.playMedia(refs(1))
        engine.bus.tryEmit(EngineEvent.State(isPlaying = true, isBuffering = false))
        controller.togglePlayPause()
        assertEquals(1, engine.paused)
        engine.bus.tryEmit(EngineEvent.State(isPlaying = false, isBuffering = false))
        controller.togglePlayPause()
        assertEquals(1, engine.played)
    }

    @Test fun shuffleAndRepeatShowInState() {
        controller.playMedia(refs(1, 2, 3))
        controller.setShuffleEnabled(true)
        controller.setRepeatMode(RepeatMode.ALL)
        assertTrue(controller.state.value.shuffleEnabled)
        assertEquals(RepeatMode.ALL, controller.state.value.repeatMode)
        assertEquals("Song 1", controller.state.value.title)
    }
}
