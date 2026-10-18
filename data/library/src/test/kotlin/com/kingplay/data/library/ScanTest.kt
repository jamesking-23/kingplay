package com.kingplay.data.library

import com.kingplay.core.model.ScanPhase
import com.kingplay.core.model.ScanState
import com.kingplay.data.library.scan.AudioDetails
import com.kingplay.data.library.scan.AudioMetadataReader
import com.kingplay.data.library.scan.DetailsEnricher
import com.kingplay.data.library.scan.DetailsResult
import com.kingplay.data.library.scan.Fingerprint
import com.kingplay.data.library.scan.LibraryStore
import com.kingplay.data.library.scan.MediaSource
import com.kingplay.data.library.scan.PendingDetails
import com.kingplay.data.library.scan.ScanEngine
import com.kingplay.data.library.scan.ScanRequest
import com.kingplay.data.library.scan.ScannedAudio
import com.kingplay.data.library.scan.ScannedVideo
import com.kingplay.data.library.scan.diffFingerprints
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun ScannedAudio.fingerprint() = fp(dateModifiedMs, sizeBytes, folderPath)

private fun ScannedVideo.fingerprint() = fp(dateModifiedMs, sizeBytes, folderPath)

private fun fp(modified: Long = 1, size: Long = 10, folder: String = "/m") = Fingerprint(modified, size, folder)

private fun audio(id: Long, modified: Long = 1, folder: String = "/m") = ScannedAudio(
    id = id, uri = "content://a/$id", title = "t$id", artist = null, album = null, albumArtist = null, genre = null,
    durationMs = 180_000, bitrateKbps = 0, trackNumber = 1, discNumber = 1, year = null, folderPath = folder,
    fileName = "f$id.mp3", mimeType = "audio/mpeg", sizeBytes = 10, dateAddedMs = 1, dateModifiedMs = modified,
)

private fun video(id: Long, modified: Long = 1, folder: String = "/v") = ScannedVideo(
    id = id, uri = "content://v/$id", title = "v$id", durationMs = 1000, width = 1920, height = 1080,
    folderPath = folder, fileName = "v$id.mp4", mimeType = "video/mp4", sizeBytes = 10, dateAddedMs = 1,
    dateModifiedMs = modified,
)

class FakeMediaSource(
    var audio: Map<Long, ScannedAudio> = emptyMap(),
    var videos: Map<Long, ScannedVideo> = emptyMap(),
) : MediaSource {
    val audioLoads = mutableListOf<List<Long>>()

    override suspend fun audioFingerprints() = audio.mapValues { it.value.fingerprint() }

    override suspend fun loadAudio(ids: List<Long>): List<ScannedAudio> {
        audioLoads += ids
        return ids.mapNotNull { audio[it] }
    }

    override suspend fun videoFingerprints() = videos.mapValues { it.value.fingerprint() }

    override suspend fun loadVideo(ids: List<Long>) = ids.mapNotNull { videos[it] }
}

class FakeLibraryStore : LibraryStore {
    val audio = linkedMapOf<Long, ScannedAudio>()
    val videos = linkedMapOf<Long, ScannedVideo>()
    val pending = linkedMapOf<Long, PendingDetails>()
    val saved = mutableListOf<DetailsResult>()

    override suspend fun audioFingerprints() = audio.mapValues { it.value.fingerprint() }

    override suspend fun videoFingerprints() = videos.mapValues { it.value.fingerprint() }

    override suspend fun upsertAudio(items: List<ScannedAudio>) {
        items.forEach {
            audio[it.id] = it
            pending[it.id] = PendingDetails(it.id, it.uri, it.sizeBytes, it.durationMs, it.bitrateKbps)
        }
    }

    override suspend fun upsertVideo(items: List<ScannedVideo>) {
        items.forEach { videos[it.id] = it }
    }

    override suspend fun deleteAudio(ids: List<Long>) {
        ids.forEach { audio.remove(it); pending.remove(it) }
    }

    override suspend fun deleteVideo(ids: List<Long>) {
        ids.forEach { videos.remove(it) }
    }

    override suspend fun pendingDetails(limit: Int) = pending.values.take(limit)

    override suspend fun pendingDetailsCount() = pending.size

    override suspend fun saveDetails(results: List<DetailsResult>) {
        saved += results
        results.forEach { pending.remove(it.id) }
    }
}

class LibraryDiffTest {
    @Test
    fun newChangedAndRemovedFilesAreDetected() {
        val existing = mapOf(1L to fp(1), 2L to fp(1), 3L to fp(1))
        val scanned = mapOf(2L to fp(1), 3L to fp(2), 4L to fp(1))
        val diff = diffFingerprints(existing, scanned)
        assertEquals(listOf(3L, 4L), diff.toLoad)
        assertEquals(listOf(1L), diff.toDelete)
    }

    @Test
    fun sizeChangeAloneCountsAsChanged() {
        val diff = diffFingerprints(mapOf(1L to fp(1, 10)), mapOf(1L to fp(1, 11)))
        assertEquals(listOf(1L), diff.toLoad)
    }

    @Test
    fun forceAllReloadsEverything() {
        val same = mapOf(1L to fp(), 2L to fp())
        assertEquals(listOf(1L, 2L), diffFingerprints(same, same, forceAll = true).toLoad)
        assertTrue(diffFingerprints(same, same).toLoad.isEmpty())
    }

    @Test
    fun emptyScanNeverWipesAPopulatedLibrary() {
        val diff = diffFingerprints(mapOf(1L to fp(), 2L to fp()), emptyMap())
        assertTrue(diff.toDelete.isEmpty())
        assertTrue(diff.toLoad.isEmpty())
    }
}

class ScanEngineTest {
    private val request = ScanRequest()

    @Test
    fun firstScanLoadsInChunksAndReportsProgress() = runBlocking {
        val source = FakeMediaSource(audio = (1L..5L).associateWith { audio(it) }, videos = mapOf(9L to video(9)))
        val store = FakeLibraryStore()
        val progress = mutableListOf<ScanState.Scanning>()
        val summary = ScanEngine(source, store, chunkSize = 2).scan(request, emptyList()) { progress += it }

        assertEquals(5, store.audio.size)
        assertEquals(1, store.videos.size)
        assertEquals(listOf(listOf(1L, 2L), listOf(3L, 4L), listOf(5L)), source.audioLoads)
        assertEquals(5, summary.audioChanged)
        assertEquals(1, summary.videoChanged)
        val audioProgress = progress.filter { it.phase == ScanPhase.INDEXING_AUDIO }
        assertEquals(listOf(0, 2, 4, 5), audioProgress.map { it.done })
        assertTrue(audioProgress.all { it.total == 5 })
        assertEquals(1f, audioProgress.last().fraction, 0f)
    }

    @Test
    fun secondScanOnlyLoadsWhatChanged() = runBlocking {
        val source = FakeMediaSource(audio = (1L..4L).associateWith { audio(it) })
        val store = FakeLibraryStore()
        val engine = ScanEngine(source, store)
        engine.scan(request, emptyList())
        source.audioLoads.clear()

        source.audio = source.audio - 2L + (3L to audio(3, modified = 5)) + (7L to audio(7))
        val summary = engine.scan(request, emptyList())

        assertEquals(listOf(listOf(3L, 7L)), source.audioLoads)
        assertEquals(setOf(1L, 3L, 4L, 7L), store.audio.keys)
        assertEquals(5L, store.audio.getValue(3L).dateModifiedMs)
        assertEquals(2, summary.audioChanged)
        assertEquals(1, summary.audioRemoved)
    }

    @Test
    fun unchangedLibraryDoesNoWork() = runBlocking {
        val source = FakeMediaSource(audio = mapOf(1L to audio(1)))
        val store = FakeLibraryStore()
        val engine = ScanEngine(source, store)
        engine.scan(request, emptyList())
        source.audioLoads.clear()
        val summary = engine.scan(request, emptyList())
        assertTrue(source.audioLoads.isEmpty())
        assertEquals(0, summary.audioChanged + summary.audioRemoved)
    }

    @Test
    fun fullScanReloadsEverything() = runBlocking {
        val source = FakeMediaSource(audio = (1L..3L).associateWith { audio(it) })
        val store = FakeLibraryStore()
        val engine = ScanEngine(source, store)
        engine.scan(request, emptyList())
        source.audioLoads.clear()
        engine.scan(ScanRequest(full = true), emptyList())
        assertEquals(listOf(listOf(1L, 2L, 3L)), source.audioLoads)
    }

    @Test
    fun excludedFoldersAreNeverStoredAndAreRemovedWhenExcludedLater() = runBlocking {
        val source = FakeMediaSource(
            audio = mapOf(1L to audio(1, folder = "/m/keep"), 2L to audio(2, folder = "/m/WhatsApp/Audio")),
        )
        val store = FakeLibraryStore()
        val engine = ScanEngine(source, store)

        engine.scan(request, listOf("/m/WhatsApp"))
        assertEquals(setOf(1L), store.audio.keys)

        engine.scan(request, emptyList())
        assertEquals(setOf(1L, 2L), store.audio.keys)

        val summary = engine.scan(request, listOf("/m/WhatsApp"))
        assertEquals(setOf(1L), store.audio.keys)
        assertEquals(1, summary.audioRemoved)
    }

    @Test
    fun emptyPlatformResultKeepsExistingLibrary() = runBlocking {
        val source = FakeMediaSource(audio = (1L..3L).associateWith { audio(it) })
        val store = FakeLibraryStore()
        val engine = ScanEngine(source, store)
        engine.scan(request, emptyList())
        source.audio = emptyMap()
        val summary = engine.scan(request, emptyList())
        assertEquals(3, store.audio.size)
        assertEquals(0, summary.audioRemoved)
    }

    @Test
    fun requestFlagsSkipAudioOrVideo() = runBlocking {
        val source = FakeMediaSource(audio = mapOf(1L to audio(1)), videos = mapOf(2L to video(2)))
        val store = FakeLibraryStore()
        ScanEngine(source, store).scan(ScanRequest(audio = false), emptyList())
        assertTrue(store.audio.isEmpty())
        assertEquals(1, store.videos.size)
    }
}

class DetailsEnricherTest {
    private class MapReader(val details: Map<String, AudioDetails?>) : AudioMetadataReader {
        override suspend fun read(uri: String) = details[uri]
    }

    @Test
    fun readerValuesWinThenStoredThenEstimate() = runBlocking {
        val store = FakeLibraryStore()
        store.pending[1] = PendingDetails(1, "u1", sizeBytes = 4_000_000, durationMs = 200_000, bitrateKbps = 0)
        store.pending[2] = PendingDetails(2, "u2", sizeBytes = 4_000_000, durationMs = 200_000, bitrateKbps = 320)
        store.pending[3] = PendingDetails(3, "u3", sizeBytes = 4_000_000, durationMs = 200_000, bitrateKbps = 0)
        val reader = MapReader(mapOf("u1" to AudioDetails(256, 44_100), "u2" to AudioDetails(0, 48_000), "u3" to null))

        val processed = DetailsEnricher(store, reader).run()

        assertEquals(3, processed)
        assertEquals(DetailsResult(1, 256, 44_100), store.saved[0])
        assertEquals(DetailsResult(2, 320, 48_000), store.saved[1])
        assertEquals(DetailsResult(3, 160, 0), store.saved[2])
    }

    @Test
    fun unreadableFilesStillTerminateTheLoop() = runBlocking {
        val store = FakeLibraryStore()
        (1L..10L).forEach { store.pending[it] = PendingDetails(it, "u$it", 1000, 1000, 0) }
        val processed = DetailsEnricher(store, MapReader(emptyMap()), batchSize = 3, parallelism = 2).run()
        assertEquals(10, processed)
        assertTrue(store.pending.isEmpty())
    }

    @Test
    fun progressReachesTotal() = runBlocking {
        val store = FakeLibraryStore()
        (1L..5L).forEach { store.pending[it] = PendingDetails(it, "u$it", 1000, 1000, 0) }
        val seen = mutableListOf<Pair<Int, Int>>()
        DetailsEnricher(store, MapReader(emptyMap()), batchSize = 2).run { done, total -> seen += done to total }
        assertEquals(0 to 5, seen.first())
        assertEquals(5 to 5, seen.last())
    }
}
