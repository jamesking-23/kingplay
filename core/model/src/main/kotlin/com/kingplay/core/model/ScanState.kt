package com.kingplay.core.model

enum class ScanPhase { INDEXING_AUDIO, INDEXING_VIDEO, READING_DETAILS }

enum class ScanFailure { PERMISSION_DENIED, UNKNOWN }

/** Progress of the background library scan, observable from any screen. */
sealed interface ScanState {
    data object Idle : ScanState

    data class Scanning(
        val phase: ScanPhase,
        val done: Int,
        val total: Int,
    ) : ScanState {
        val fraction: Float get() = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
    }

    data class Failed(val reason: ScanFailure) : ScanState
}
