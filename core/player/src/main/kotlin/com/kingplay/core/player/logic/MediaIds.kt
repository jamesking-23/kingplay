package com.kingplay.core.player.logic

import com.kingplay.core.model.MediaRef

/**
 * Media ids shared between the UI and the playback service. Controllers send only ids (Media3 does not let a
 * controller choose arbitrary URIs); the service looks the file up in the library.
 */
object MediaIds {
    private const val AUDIO_PREFIX = "a:"
    private const val VIDEO_PREFIX = "v:"

    fun of(ref: MediaRef): String = when (ref) {
        is MediaRef.Audio -> AUDIO_PREFIX + ref.id
        is MediaRef.Video -> VIDEO_PREFIX + ref.id
    }

    fun parse(mediaId: String?): MediaRef? {
        if (mediaId == null) return null
        val id = mediaId.drop(PREFIX_LENGTH).toLongOrNull() ?: return null
        return when {
            mediaId.startsWith(AUDIO_PREFIX) -> MediaRef.Audio(id)
            mediaId.startsWith(VIDEO_PREFIX) -> MediaRef.Video(id)
            else -> null
        }
    }

    private const val PREFIX_LENGTH = 2
}
