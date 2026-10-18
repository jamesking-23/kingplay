package com.kingplay.core.player.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.annotation.OptIn
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.io.IOException
import java.util.concurrent.Executors

private const val ARTWORK_PX = 512

/**
 * Loads cover art for the notification and lock screen. A song's cover lives inside the audio file, so the song's own
 * URI is used as the artwork address and the cover is read out of the file here.
 */
@OptIn(UnstableApi::class)
internal class ArtworkBitmapLoader(private val context: Context) : BitmapLoader {
    private val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        executor.submit<Bitmap> {
            BitmapFactory.decodeByteArray(data, 0, data.size) ?: throw IOException("Not an image")
        }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = executor.submit<Bitmap> { load(uri) }

    private fun load(uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return context.contentResolver.loadThumbnail(uri, Size(ARTWORK_PX, ARTWORK_PX), null)
        }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: throw IOException("No artwork in file")
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw IOException("Unreadable artwork")
        } finally {
            retriever.release()
        }
    }
}
