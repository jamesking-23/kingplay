package com.kingplay.desktop.cover

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.kingplay.core.model.Track
import com.kingplay.core.model.UnknownNames
import org.jaudiotagger.audio.AudioFileIO
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Finds, decodes and caches album covers. One cover per album (the first song that has one wins, and a miss is
 * remembered too, so folders without art are not searched again for every row).
 */
class CoverRepository(private val thumbPx: Int = THUMB_PX) {
    private val cache = LruCache<Long, ImageBitmap>(CACHE_SIZE)
    private val misses = LruCache<Long, Boolean>(CACHE_SIZE * 2)

    init {
        Logger.getLogger("org.jaudiotagger").level = Level.OFF
    }

    /** Blocking: call from a background dispatcher. */
    fun load(track: Track): ImageBitmap? {
        val key = coverKey(track)
        cache[key]?.let { return it }
        if (misses.contains(key)) return null
        val bitmap = runCatching { bytesFor(track)?.let(::decode) }.getOrNull()
        if (bitmap != null) cache[key] = bitmap else misses[key] = true
        return bitmap
    }

    /** Songs without an album tag all share one album id, so they are told apart by song instead. */
    private fun coverKey(track: Track): Long = if (track.albumTitle == UnknownNames.ALBUM) track.id else track.albumId

    private fun bytesFor(track: Track): ByteArray? {
        embedded(File(track.uri))?.let { return it }
        return findFolderCover(File(track.folderPath))?.readBytes()
    }

    private fun embedded(file: File): ByteArray? = runCatching {
        AudioFileIO.read(file).tag?.firstArtwork?.binaryData?.takeIf { it.isNotEmpty() }
    }.getOrNull()

    private fun decode(bytes: ByteArray): ImageBitmap {
        val image = Image.makeFromEncoded(bytes)
        val (w, h) = scaledSize(image.width, image.height, thumbPx)
        if (w == image.width && h == image.height) return image.toComposeImageBitmap()
        // Draw into a small surface so hundreds of covers do not each keep a full-size bitmap in memory.
        val surface = Surface.makeRasterN32Premul(w, h)
        surface.canvas.drawImageRect(image, Rect.makeWH(w.toFloat(), h.toFloat()))
        return surface.makeImageSnapshot().toComposeImageBitmap()
    }

    private companion object {
        const val THUMB_PX = 320
        const val CACHE_SIZE = 200
    }
}
