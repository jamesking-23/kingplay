package com.kingplay.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.kingplay.core.model.repository.LibraryScanner
import com.kingplay.core.ui.artwork.createArtworkImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KingPlayApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var libraryScanner: LibraryScanner

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    /** Cover art loader shared by every screen. */
    override fun newImageLoader(): ImageLoader = createArtworkImageLoader(this)

    override fun onCreate() {
        super.onCreate()
        // Registers the media observer and daily scan, and scans now if media permission is already granted.
        libraryScanner.start()
    }
}
