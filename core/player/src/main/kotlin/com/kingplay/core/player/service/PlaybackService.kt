package com.kingplay.core.player.service

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.QueueRepository
import com.kingplay.core.model.repository.VideoRepository
import com.kingplay.core.player.logic.MediaIds
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Owns the one player. Media3 turns it into a media session, which gives us the notification, lock-screen controls,
 * Bluetooth and headset buttons, and (from step 2h) the Android Auto browse tree.
 */
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject lateinit var player: Player

    @Inject lateinit var library: LibraryRepository

    @Inject lateinit var videos: VideoRepository

    @Inject lateinit var queueRepository: QueueRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var session: MediaLibrarySession? = null
    private var persistence: QueuePersistence? = null

    override fun onCreate() {
        super.onCreate()
        session = MediaLibrarySession.Builder(this, player, LibraryCallback()).setId(SESSION_ID).build()
        val persistence = QueuePersistence(player, queueRepository, library, videos, scope)
        this.persistence = persistence
        scope.launch {
            persistence.restore()
            persistence.start()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    /** Swiping the app away stops a paused player; a playing one keeps going like any music app. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        persistence?.stop()
        session?.release()
        session = null
        player.release()
        scope.cancel()
        super.onDestroy()
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build(),
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        // The browse tree for Android Auto and other media browsers arrives in step 2h.
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
            Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params))

        /** Controllers send ids only. Turn them into playable items; anything that is not a known id is dropped. */
        @Suppress("TooGenericExceptionCaught")
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val result = SettableFuture.create<MutableList<MediaItem>>()
            scope.launch {
                try {
                    result.set(resolve(mediaItems))
                } catch (e: CancellationException) {
                    result.cancel(false)
                    throw e
                } catch (e: Exception) {
                    result.setException(e)
                }
            }
            return result
        }
    }

    private suspend fun resolve(items: List<MediaItem>): MutableList<MediaItem> {
        val refs = items.map { MediaIds.parse(it.mediaId) }
        val audio = library.getTracks(refs.filterIsInstance<MediaRef.Audio>().map { it.id }).associateBy { it.id }
        val video = videos.getVideos(refs.filterIsInstance<MediaRef.Video>().map { it.id }).associateBy { it.id }
        return refs.mapNotNull { ref ->
            when (ref) {
                is MediaRef.Audio -> audio[ref.id]?.toMediaItem()
                is MediaRef.Video -> video[ref.id]?.toMediaItem()
                null -> null
            }
        }.toMutableList()
    }

    private companion object {
        const val SESSION_ID = "kingplay"
        const val ROOT_ID = "kingplay_root"
    }
}
