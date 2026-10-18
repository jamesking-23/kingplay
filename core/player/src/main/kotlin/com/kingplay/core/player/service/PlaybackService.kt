package com.kingplay.core.player.service

import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.QueueRepository
import com.kingplay.core.model.repository.VideoRepository
import com.kingplay.core.player.browse.BrowseTree
import com.kingplay.core.player.engine.AudioEngineBinder
import com.kingplay.core.player.engine.LyricsBinder
import com.kingplay.core.player.engine.NowPlayingBinder
import com.kingplay.core.player.engine.SleepTimerBinder
import com.kingplay.core.player.logic.BrowseIds
import com.kingplay.core.player.logic.BrowseNode
import com.kingplay.core.player.logic.CallerPolicy
import com.kingplay.core.player.logic.MediaIds
import com.kingplay.core.player.logic.expandToList
import com.kingplay.core.player.logic.pageOf
import com.kingplay.core.player.video.SubtitleExtras
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Owns the one player. Media3 turns it into a media session, which gives us the notification, lock-screen controls,
 * Bluetooth and headset buttons, the Android Auto browse tree and voice search, and resuming the last queue when a
 * play button is pressed while the app is not running.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject lateinit var player: Player

    @Inject lateinit var library: LibraryRepository

    @Inject lateinit var videos: VideoRepository

    @Inject lateinit var queueRepository: QueueRepository

    @Inject lateinit var audioEngine: AudioEngineBinder

    @Inject lateinit var lyricsBinder: LyricsBinder

    @Inject lateinit var sleepBinder: SleepTimerBinder

    @Inject lateinit var nowPlayingBinder: NowPlayingBinder

    @Inject lateinit var browseTree: BrowseTree

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var session: MediaLibrarySession? = null
    private var persistence: QueuePersistence? = null

    /** Media ids of the song list a car or other browser looked at last; tapping one song queues the whole list. */
    @Volatile private var lastBrowsedSongs: List<String> = emptyList()

    override fun onCreate() {
        super.onCreate()
        session = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .setId(SESSION_ID)
            .setBitmapLoader(ArtworkBitmapLoader(this))
            .build()
        audioEngine.start(scope)
        lyricsBinder.start()
        sleepBinder.start(scope)
        nowPlayingBinder.start()
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
        nowPlayingBinder.stop()
        sleepBinder.stop()
        lyricsBinder.stop()
        audioEngine.stop()
        persistence?.stop()
        session?.release()
        session = null
        player.release()
        scope.cancel()
        super.onDestroy()
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {

        /** Only this app, the system and known car, assistant and Bluetooth packages may connect ([CallerPolicy]). */
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val allowed = CallerPolicy.isAllowed(controller.packageName, packageName, controller.isTrusted)
            if (!allowed) {
                Log.w(TAG, "Refused a connection from ${controller.packageName}")
                return MediaSession.ConnectionResult.reject()
            }
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId(BrowseIds.of(BrowseNode.Root))
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

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = future {
            val items = browseTree.children(parentId)
            if (items == null) {
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            } else {
                if (BrowseIds.parse(parentId)?.let(BrowseIds::listsSongs) == true) {
                    lastBrowsedSongs = items.map { it.mediaId }
                }
                LibraryResult.ofItemList(ImmutableList.copyOf(pageOf(items, page, pageSize)), params)
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = future {
            browseTree.item(mediaId)?.let { LibraryResult.ofItem(it, null) }
                ?: LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> = future {
            session.notifySearchResultChanged(browser, query, browseTree.searchSongs(query).size, params)
            LibraryResult.ofVoid()
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = future {
            val songs = browseTree.searchSongs(query).map { it.toMediaItem() }
            LibraryResult.ofItemList(ImmutableList.copyOf(pageOf(songs, page, pageSize)), params)
        }

        /** Controllers send ids only. Turn them into playable items; anything that is not a known id is dropped. */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> = future { resolve(mediaItems) }

        /** One song tapped in a car's list plays that whole list from the tapped song, like a music app would. */
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaItemsWithStartPosition> = future {
            val tapped = mediaItems.singleOrNull()?.mediaId
            val expanded = if (tapped != null && controller.packageName != packageName) {
                expandToList(lastBrowsedSongs, tapped)
            } else {
                null
            }
            if (expanded != null) {
                val (ids, index) = expanded
                val queue = resolve(ids.map { MediaItem.Builder().setMediaId(it).build() })
                // Songs that vanished since the list was shown shift the index; find the tapped one again.
                val start = queue.indexOfFirst { it.mediaId == tapped }.coerceAtLeast(0)
                MediaItemsWithStartPosition(queue, if (queue.size == ids.size) index else start, C.TIME_UNSET)
            } else {
                MediaItemsWithStartPosition(resolve(mediaItems), startIndex, startPositionMs)
            }
        }

        /** A play button pressed while nothing is loaded (headset, Bluetooth, widget) resumes the last queue. */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaItemsWithStartPosition> = future {
            val saved = loadSavedQueue(queueRepository, library, videos)
                ?: throw IllegalStateException("Nothing to resume")
            MediaItemsWithStartPosition(saved.items, saved.index, saved.positionMs)
        }
    }

    /** Runs [block] on the service's main-thread scope and hands its result to Media3 as a future. */
    @Suppress("TooGenericExceptionCaught")
    private fun <T> future(block: suspend () -> T): ListenableFuture<T> {
        val result = SettableFuture.create<T>()
        scope.launch {
            try {
                result.set(block())
            } catch (e: CancellationException) {
                result.cancel(false)
                throw e
            } catch (e: Exception) {
                result.setException(e)
            }
        }
        return result
    }

    private suspend fun resolve(items: List<MediaItem>): MutableList<MediaItem> {
        val refs = items.map { MediaIds.parse(it.mediaId) }
        val audio = library.getTracks(refs.filterIsInstance<MediaRef.Audio>().map { it.id }).associateBy { it.id }
        val video = videos.getVideos(refs.filterIsInstance<MediaRef.Video>().map { it.id }).associateBy { it.id }
        return items.indices.flatMap { index ->
            // "Play <name>" by voice arrives as an item with no id and a search text.
            val spoken = items[index].takeIf { it.mediaId.isBlank() }?.requestMetadata?.searchQuery
            if (!spoken.isNullOrBlank()) {
                return@flatMap browseTree.searchSongs(spoken).map { it.toMediaItem() }
            }
            listOfNotNull(
                when (val ref = refs[index]) {
                    is MediaRef.Audio -> audio[ref.id]?.toMediaItem()
                    is MediaRef.Video -> video[ref.id]?.toMediaItem(SubtitleExtras.read(items[index]))
                    null -> null
                },
            )
        }.toMutableList()
    }

    private companion object {
        const val SESSION_ID = "kingplay"
        const val TAG = "PlaybackService"
    }
}
