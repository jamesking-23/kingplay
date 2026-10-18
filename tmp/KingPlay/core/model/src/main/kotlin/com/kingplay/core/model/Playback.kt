package com.kingplay.core.model

enum class RepeatMode { OFF, ALL, ONE }

/** Persisted queue snapshot. Restored on app start by the player module. */
data class QueueState(
    val items: List<MediaRef> = emptyList(),
    val currentIndex: Int = -1,
    val positionMs: Long = 0,
    val shuffleEnabled: Boolean = false,
    val shuffleOrder: List<Int> = emptyList(),
    val repeatMode: RepeatMode = RepeatMode.OFF,
) {
    val current: MediaRef? get() = items.getOrNull(currentIndex)
}
