package com.kingplay.core.ui.artwork

import android.content.Context
import coil.ImageLoader
import coil.key.Keyer

/** One image loader for the app. The application class returns it from `ImageLoaderFactory`. */
fun createArtworkImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
    .components {
        add(Keyer<ArtworkModel> { data, _ -> data.memoryKey })
        add(ArtworkFetcher.Factory())
    }
    .crossfade(true)
    .build()
