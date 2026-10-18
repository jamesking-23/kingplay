package com.kingplay.core.model.future

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.Track
import kotlinx.coroutines.flow.Flow

// Domain contracts for later phases. No implementations exist in Phase 1.

data class TrackAnalysis(val bpm: Float?, val musicalKey: String?, val moods: List<String>)

/** :feature:ai (Phase 3) */
interface AiAnalysisService {
    suspend fun analyze(track: Track): TrackAnalysis

    fun smartPlaylist(prompt: String): Flow<List<MediaRef>>
}

enum class StemType { VOCALS, DRUMS, BASS, OTHER }

data class StemSet(val trackId: Long, val stemUris: Map<StemType, String>)

/** :feature:stems (Phase 4) */
interface StemSeparator {
    suspend fun separate(track: Track): StemSet
}

/** :feature:social (Phase 4) */
interface ListenTogetherSession {
    val participants: Flow<List<String>>

    suspend fun join(code: String)

    suspend fun leave()
}

enum class SyncState { IDLE, SYNCING, OFFLINE }

/** :core:sync (Phase 5) */
interface SyncEngine {
    val state: Flow<SyncState>

    suspend fun syncNow()
}

data class BillingProduct(val id: String, val title: String, val priceLabel: String)

/** :core:billing (Phase 5) */
interface BillingGateway {
    suspend fun products(): List<BillingProduct>

    suspend fun purchase(productId: String): Boolean
}
