package com.kingplay.core.model.repository

import com.kingplay.core.model.ScanState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Controls the background media scan. The implementation lives in :data:library. */
interface LibraryScanner {
    val state: StateFlow<ScanState>

    /**
     * Sets up background machinery (media change observer, periodic scan) and requests a scan.
     * Safe to call repeatedly, for example again right after the user grants media permission.
     */
    fun start()

    /** Queues a scan. [full] re-reads every file instead of only new and changed ones. */
    fun requestScan(full: Boolean = false)
}

/** Folders the user excluded from scanning. A scan is queued automatically after a change. */
interface LibraryFolderSettings {
    val excludedFolders: Flow<List<String>>

    suspend fun exclude(path: String)

    suspend fun include(path: String)
}
