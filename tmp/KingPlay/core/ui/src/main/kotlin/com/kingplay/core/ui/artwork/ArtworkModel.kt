package com.kingplay.core.ui.artwork

/**
 * Coil model for cover art. [audioUri] is any audio file of the album. The art is read from that file (or from the
 * platform's thumbnail cache), so one decoded bitmap serves every track that shares [cacheKey].
 */
data class ArtworkModel(
    val audioUri: String,
    val cacheKey: String,
    val sizePx: Int,
) {
    val memoryKey: String get() = "art:$cacheKey:$sizePx"
}
