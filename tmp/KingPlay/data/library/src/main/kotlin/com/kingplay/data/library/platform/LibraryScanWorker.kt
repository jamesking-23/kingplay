package com.kingplay.data.library.platform

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kingplay.core.database.dao.ExcludedFolderDao
import com.kingplay.core.model.ScanFailure
import com.kingplay.core.model.ScanPhase
import com.kingplay.core.model.ScanState
import com.kingplay.data.library.scan.AudioMetadataReader
import com.kingplay.data.library.scan.DetailsEnricher
import com.kingplay.data.library.scan.LibraryStore
import com.kingplay.data.library.scan.MediaSource
import com.kingplay.data.library.scan.ScanEngine
import com.kingplay.data.library.scan.ScanRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class LibraryScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val source: MediaSource,
    private val store: LibraryStore,
    private val reader: AudioMetadataReader,
    private val excludedFolders: ExcludedFolderDao,
    private val stateHolder: ScanStateHolder,
) : CoroutineWorker(appContext, params) {

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun doWork(): Result {
        val audioAllowed = MediaPermissions.hasAudio(applicationContext)
        val videoAllowed = MediaPermissions.hasVideo(applicationContext)
        if (!audioAllowed && !videoAllowed) {
            // Nothing is read, and nothing is deleted: a revoked permission must never empty the library.
            stateHolder.set(ScanState.Failed(ScanFailure.PERMISSION_DENIED))
            return Result.success()
        }
        return try {
            val request = ScanRequest(
                full = inputData.getBoolean(KEY_FULL, false),
                audio = audioAllowed,
                video = videoAllowed,
            )
            ScanEngine(source, store).scan(request, excludedFolders.getPaths()) { stateHolder.set(it) }
            if (audioAllowed) {
                DetailsEnricher(store, reader).run { done, total ->
                    stateHolder.set(ScanState.Scanning(ScanPhase.READING_DETAILS, done, total))
                }
            }
            stateHolder.set(ScanState.Idle)
            Result.success()
        } catch (e: CancellationException) {
            stateHolder.set(ScanState.Idle)
            throw e
        } catch (e: Exception) {
            stateHolder.set(ScanState.Failed(ScanFailure.UNKNOWN))
            if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val KEY_FULL = "full"
        private const val MAX_ATTEMPTS = 3
    }
}
