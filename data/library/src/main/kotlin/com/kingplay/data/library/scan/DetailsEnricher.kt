package com.kingplay.data.library.scan

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Second pass of a scan. The fast pass makes songs appear immediately; this one opens each file to learn the
 * real sample rate and bitrate. It is resumable: tracks stay flagged until their details are saved, and a file
 * that cannot be read is still marked done (with an estimated bitrate) so the loop always terminates.
 */
class DetailsEnricher(
    private val store: LibraryStore,
    private val reader: AudioMetadataReader,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
    private val parallelism: Int = DEFAULT_PARALLELISM,
) {
    init {
        require(batchSize > 0 && parallelism > 0) { "batchSize and parallelism must be positive" }
    }

    /** Returns how many tracks were processed. */
    suspend fun run(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): Int {
        val total = store.pendingDetailsCount()
        var done = 0
        onProgress(done, total)
        while (true) {
            coroutineContext.ensureActive()
            val batch = store.pendingDetails(batchSize)
            if (batch.isEmpty()) break
            val results = batch.chunked(parallelism).flatMap { group ->
                coroutineScope { group.map { item -> async { resolve(item) } }.awaitAll() }
            }
            store.saveDetails(results)
            done += results.size
            onProgress(done, maxOf(total, done))
        }
        return done
    }

    private suspend fun resolve(item: PendingDetails): DetailsResult {
        val details = reader.read(item.uri)
        val bitrate = details?.bitrateKbps?.takeIf { it > 0 }
            ?: item.bitrateKbps.takeIf { it > 0 }
            ?: estimateBitrateKbps(item.sizeBytes, item.durationMs)
        return DetailsResult(item.id, bitrate, details?.sampleRateHz ?: 0)
    }

    companion object {
        const val DEFAULT_BATCH_SIZE = 64
        const val DEFAULT_PARALLELISM = 4
    }
}
