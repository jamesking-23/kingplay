package com.kingplay.core.ui.artwork

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads cover art for an audio file. Android 10+ asks the media store for its thumbnail (which also handles
 * folder art); older versions, and files the media store has no thumbnail for, read the embedded picture.
 * Returns null when there is no art, which makes Coil show the placeholder.
 */
internal class ArtworkFetcher(
    private val model: ArtworkModel,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val context = options.context
        val uri = Uri.parse(model.audioUri)
        val bitmap = thumbnail(context, uri, model.sizePx) ?: embedded(context, uri, model.sizePx)
        bitmap?.let {
            DrawableResult(
                drawable = BitmapDrawable(context.resources, it),
                isSampled = false,
                dataSource = DataSource.DISK,
            )
        }
    }

    class Factory : Fetcher.Factory<ArtworkModel> {
        override fun create(data: ArtworkModel, options: Options, imageLoader: ImageLoader): Fetcher =
            ArtworkFetcher(data, options)
    }

    private companion object {
        @Suppress("SwallowedException")
        fun thumbnail(context: Context, uri: Uri, sizePx: Int): Bitmap? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
            return try {
                context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
            } catch (e: IOException) {
                null
            } catch (e: IllegalArgumentException) {
                null
            } catch (e: SecurityException) {
                null
            }
        }

        @Suppress("TooGenericExceptionCaught")
        fun embedded(context: Context, uri: Uri, sizePx: Int): Bitmap? {
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(context, uri)
                val bytes = retriever.embeddedPicture ?: return null
                decodeSampled(bytes, sizePx)
            } catch (e: RuntimeException) {
                // MediaMetadataRetriever throws plain RuntimeException for unreadable or unsupported files.
                null
            } finally {
                retriever.release()
            }
        }

        fun decodeSampled(bytes: ByteArray, sizePx: Int): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val sample = sampleSize(bounds.outWidth, bounds.outHeight, sizePx)
            val decode = BitmapFactory.Options().apply { inSampleSize = sample }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decode)
        }
    }
}
