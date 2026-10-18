package com.kingplay.data.library.scan

import com.kingplay.core.model.ScanPhase
import com.kingplay.core.model.ScanState
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Incremental library scan. It asks the platform for cheap fingerprints first, diffs them against the
 * database, then loads full rows only for files that are new or changed, in chunks, so the UI can show
 * results while the scan is still running and a cancelled scan loses nothing.
 */
class ScanEngine(
    private val source: MediaSource,
    private val store: LibraryStore,
    private val chunkSize: Int = DEFAULT_CHUNK_SIZE,
) {
    init {
        require(chunkSize > 0) { "chunkSize must be positive" }
    }

    suspend fun scan(
        request: ScanRequest,
        excludedFolders: List<String>,
        onProgress: (ScanState.Scanning) -> Unit = {},
    ): ScanSummary {
        var summary = ScanSummary()
        if (request.audio) {
            val (changed, removed) = sync(
                phase = ScanPhase.INDEXING_AUDIO,
                forceAll = request.full,
                scanned = source.audioFingerprints().visible(excludedFolders),
                existing = store.audioFingerprints(),
                load = source::loadAudio,
                upsert = store::upsertAudio,
                delete = store::deleteAudio,
                onProgress = onProgress,
            )
            summary = summary.copy(audioChanged = changed, audioRemoved = removed)
        }
        if (request.video) {
            val (changed, removed) = sync(
                phase = ScanPhase.INDEXING_VIDEO,
                forceAll = request.full,
                scanned = source.videoFingerprints().visible(excludedFolders),
                existing = store.videoFingerprints(),
                load = source::loadVideo,
                upsert = store::upsertVideo,
                delete = store::deleteVideo,
                onProgress = onProgress,
            )
            summary = summary.copy(videoChanged = changed, videoRemoved = removed)
        }
        return summary
    }

    @Suppress("LongParameterList")
    private suspend fun <T> sync(
        phase: ScanPhase,
        forceAll: Boolean,
        scanned: Map<Long, Fingerprint>,
        existing: Map<Long, Fingerprint>,
        load: suspend (List<Long>) -> List<T>,
        upsert: suspend (List<T>) -> Unit,
        delete: suspend (List<Long>) -> Unit,
        onProgress: (ScanState.Scanning) -> Unit,
    ): Pair<Int, Int> {
        val diff = diffFingerprints(existing, scanned, forceAll)
        onProgress(ScanState.Scanning(phase, 0, diff.toLoad.size))
        var done = 0
        for (chunk in diff.toLoad.chunked(chunkSize)) {
            coroutineContext.ensureActive()
            val rows = load(chunk)
            if (rows.isNotEmpty()) upsert(rows)
            done += chunk.size
            onProgress(ScanState.Scanning(phase, done, diff.toLoad.size))
        }
        for (chunk in diff.toDelete.chunked(chunkSize)) {
            coroutineContext.ensureActive()
            delete(chunk)
        }
        return diff.toLoad.size to diff.toDelete.size
    }

    private fun Map<Long, Fingerprint>.visible(excluded: List<String>): Map<Long, Fingerprint> =
        if (excluded.isEmpty()) this else filterValues { !isExcluded(it.folderPath, excluded) }

    companion object {
        const val DEFAULT_CHUNK_SIZE = 400
    }
}
