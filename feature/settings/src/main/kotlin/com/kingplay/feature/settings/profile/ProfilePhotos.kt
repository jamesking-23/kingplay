package com.kingplay.feature.settings.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Turns a picture the person picked into a small private copy, so it keeps working if the original moves. */
object ProfilePhotos {
    private const val MAX_SIDE = 512
    private const val QUALITY = 88

    /** Returns the path of the saved copy, or null when the picture could not be read. Removes older copies. */
    suspend fun save(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decode(context, uri) ?: return@runCatching null
            val file = File(context.filesDir, "profile_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
            removeOld(context, keep = file)
            file.absolutePath
        }.getOrNull()
    }

    suspend fun delete(context: Context) = withContext(Dispatchers.IO) { removeOld(context, keep = null) }

    private fun removeOld(context: Context, keep: File?) {
        context.filesDir.listFiles { f -> f.name.startsWith("profile_") && f != keep }?.forEach { it.delete() }
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > MAX_SIDE) {
                    val scale = MAX_SIDE.toFloat() / longest
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
                decoder.isMutableRequired = false
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }
}
