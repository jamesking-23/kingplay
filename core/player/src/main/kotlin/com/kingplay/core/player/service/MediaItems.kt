package com.kingplay.core.player.service

import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.Track
import com.kingplay.core.model.VideoItem
import com.kingplay.core.player.logic.MediaIds
import com.kingplay.core.player.video.ExternalSubtitle

/** Full item the player can play: id for the UI, URI for the decoder, metadata for notification and lock screen. */
internal fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(MediaIds.of(MediaRef.Audio(id)))
    .setUri(uri.toUri())
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            // The cover is inside the file; ArtworkBitmapLoader reads it from the song's own address.
            .setArtworkUri(uri.toUri())
            .setArtist(artistName)
            .setAlbumTitle(albumTitle)
            .setAlbumArtist(albumArtist)
            .setGenre(genre)
            .setTrackNumber(trackNumber.takeIf { it > 0 })
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .build(),
    )
    .build()

internal fun VideoItem.toMediaItem(subtitle: ExternalSubtitle? = null): MediaItem = MediaItem.Builder()
    .setMediaId(MediaIds.of(MediaRef.Video(id)))
    .setUri(uri.toUri())
    .setSubtitleConfigurations(listOfNotNull(subtitle?.toConfiguration()))
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtworkUri(uri.toUri())
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
            .build(),
    )
    .build()

internal fun ExternalSubtitle.toConfiguration(): MediaItem.SubtitleConfiguration =
    MediaItem.SubtitleConfiguration.Builder(uri.toUri())
        .setMimeType(mimeType)
        .setLabel(label)
        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
        .build()
