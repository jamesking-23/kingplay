package com.kingplay.feature.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size

/** Reads the cover out of the song file, small enough for a widget. Returns null on any failure. */
internal object CoverLoader {
    fun load(context: Context, source: String?): Bitmap? {
        if (source.isNullOrBlank()) return null
        return runCatching {
            val uri = Uri.parse(source)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, Size(WIDGET_COVER_PX, WIDGET_COVER_PX), null)
            } else {
                fromEmbeddedPicture(context, uri)
            }
        }.getOrNull()
    }

    private fun fromEmbeddedPicture(context: Context, uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } finally {
            retriever.release()
        }
    }
}
