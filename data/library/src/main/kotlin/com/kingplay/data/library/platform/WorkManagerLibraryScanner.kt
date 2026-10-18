package com.kingplay.data.library.platform

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kingplay.core.common.ApplicationScope
import com.kingplay.core.model.ScanFailure
import com.kingplay.core.model.ScanState
import com.kingplay.core.model.repository.LibraryScanner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Singleton
class WorkManagerLibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stateHolder: ScanStateHolder,
    @ApplicationScope private val scope: CoroutineScope,
) : LibraryScanner {

    override val state: StateFlow<ScanState> = stateHolder.state

    private val lock = Any()
    private var observersRegistered = false
    private var debounceJob: Job? = null

    override fun start() {
        registerObserversOnce()
        schedulePeriodic()
        if (MediaPermissions.hasAny(context)) {
            requestScan()
        } else {
            stateHolder.set(ScanState.Failed(ScanFailure.PERMISSION_DENIED))
        }
    }

    override fun requestScan(full: Boolean) {
        val request = OneTimeWorkRequestBuilder<LibraryScanWorker>()
            .setInputData(workDataOf(LibraryScanWorker.KEY_FULL to full))
            .build()
        // If a scan is already running, the new one is queued behind it so late changes are still picked up.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_SCAN, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<LibraryScanWorker>(PERIOD_HOURS, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun registerObserversOnce() {
        synchronized(lock) {
            if (observersRegistered) return
            observersRegistered = true
        }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scheduleDebouncedScan()
            }
        }
        val resolver = context.contentResolver
        resolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        resolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
    }

    /** Media changes arrive in bursts (a download touching many files); wait for the burst to settle. */
    private fun scheduleDebouncedScan() {
        synchronized(lock) {
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(DEBOUNCE_MS)
                if (MediaPermissions.hasAny(context)) requestScan()
            }
        }
    }

    private companion object {
        const val UNIQUE_SCAN = "kingplay-library-scan"
        const val UNIQUE_PERIODIC = "kingplay-library-scan-periodic"
        const val PERIOD_HOURS = 24L
        const val DEBOUNCE_MS = 3_000L
    }
}
