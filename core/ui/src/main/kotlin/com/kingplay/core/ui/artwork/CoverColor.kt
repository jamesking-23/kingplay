package com.kingplay.core.ui.artwork

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val SAMPLE_SIZE = 128
private const val MIN_SATURATION = 0.12f

/** The main colour of a song's cover art (ARGB), or null when the song has no art or the art is grey. */
suspend fun coverColor(context: Context, audioUri: String, cacheKey: String): Int? {
    val request = ImageRequest.Builder(context)
        .data(ArtworkModel(audioUri, cacheKey, SAMPLE_SIZE))
        .allowHardware(false)
        .build()
    val result = context.imageLoader.execute(request) as? SuccessResult ?: return null
    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return null
    return withContext(Dispatchers.Default) { dominant(bitmap) }
}

private fun dominant(bitmap: Bitmap): Int? {
    val palette = Palette.from(bitmap).maximumColorCount(PALETTE_COLORS).generate()
    val swatch = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.darkVibrantSwatch ?: palette.dominantSwatch
        ?: return null
    return swatch.rgb.takeIf { saturation(it) >= MIN_SATURATION }
}

private fun saturation(argb: Int): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    return hsv[1]
}

private const val PALETTE_COLORS = 16
