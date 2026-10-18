package com.kingplay.core.player.browse

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.Track
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.PlaylistRepository
import com.kingplay.core.model.repository.SearchRepository
import com.kingplay.core.player.R
import com.kingplay.core.player.logic.BROWSE_FOLDER_LIMIT
import com.kingplay.core.player.logic.BROWSE_SONG_LIMIT
import com.kingplay.core.player.logic.BrowseIds
import com.kingplay.core.player.logic.BrowseNode
import com.kingplay.core.player.logic.MediaIds
import com.kingplay.core.player.logic.SEARCH_LIMIT
import com.kingplay.core.player.service.toMediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * The library as Android Auto and other media browsers see it: four tabs (recent, playlists, albums, artists), with
 * favourites, most played and recently added at the top of Playlists. Only music; video is not offered to cars.
 */
@Singleton
class BrowseTree @Inject constructor(
    @ApplicationContext private val context: Context,
    private val library: LibraryRepository,
    private val playlists: PlaylistRepository,
    private val search: SearchRepository,
) {
    /** What is inside [parentId], or null when it is not a folder of this tree. */
    suspend fun children(parentId: String): List<MediaItem>? {
        val node = BrowseIds.parse(parentId) ?: return null
        return when (node) {
            BrowseNode.Root -> BrowseIds.rootChildren.map { folder(it, title(it)) }
            BrowseNode.Playlists ->
                BrowseIds.smartLists.map { folder(it, title(it)) } +
                    playlists.playlists().first().take(BROWSE_FOLDER_LIMIT).map {
                        folder(BrowseNode.Playlist(it.id), it.name, subtitle = it.entryCount.toString())
                    }
            BrowseNode.Albums -> library.albums().first().take(BROWSE_FOLDER_LIMIT).map {
                folder(BrowseNode.Album(it.id), it.title, subtitle = it.artistName)
            }
            BrowseNode.Artists -> library.artists().first().take(BROWSE_FOLDER_LIMIT).map {
                folder(BrowseNode.Artist(it.id), it.name)
            }
            else -> songs(node).map { it.toMediaItem() }
        }
    }

    /** A single item by id: a song, or one of the folders. */
    suspend fun item(mediaId: String): MediaItem? {
        val node = BrowseIds.parse(mediaId)
        if (node == null) {
            val id = MediaIds.parse(mediaId) as? MediaRef.Audio
            return id?.let { library.getTracks(listOf(it.id)).firstOrNull()?.toMediaItem() }
        }
        return when (node) {
            is BrowseNode.Playlist -> playlists.playlist(node.id).first()?.let { folder(node, it.name) }
            is BrowseNode.Album -> library.album(node.id).first()?.let { folder(node, it.title, it.artistName) }
            is BrowseNode.Artist -> library.artist(node.id).first()?.let { folder(node, it.name) }
            else -> folder(node, title(node))
        }
    }

    /** Songs matching a spoken or typed query, best matches first. */
    suspend fun searchSongs(query: String): List<Track> =
        if (query.isBlank()) emptyList() else search.search(query).first().tracks.take(SEARCH_LIMIT)

    private suspend fun songs(node: BrowseNode): List<Track> = when (node) {
        BrowseNode.Recent -> library.recentlyPlayed(BROWSE_SONG_LIMIT).first()
        BrowseNode.Favorites -> library.favorites().first()
        BrowseNode.MostPlayed -> library.mostPlayed(BROWSE_SONG_LIMIT).first()
        BrowseNode.RecentlyAdded -> library.recentlyAdded(BROWSE_SONG_LIMIT).first()
        is BrowseNode.Playlist -> playlists.tracks(node.id).first()
        is BrowseNode.Album -> library.albumTracks(node.id).first()
        is BrowseNode.Artist -> library.artistTracks(node.id).first()
        else -> emptyList()
    }.take(BROWSE_SONG_LIMIT)

    private fun title(node: BrowseNode): String = context.getString(
        when (node) {
            BrowseNode.Recent -> R.string.player_browse_recent
            BrowseNode.Playlists -> R.string.player_browse_playlists
            BrowseNode.Albums -> R.string.player_browse_albums
            BrowseNode.Artists -> R.string.player_browse_artists
            BrowseNode.Favorites -> R.string.player_browse_favorites
            BrowseNode.MostPlayed -> R.string.player_browse_most_played
            BrowseNode.RecentlyAdded -> R.string.player_browse_added
            else -> R.string.player_browse_library
        },
    )

    private fun folder(node: BrowseNode, title: String, subtitle: String? = null): MediaItem =
        MediaItem.Builder()
            .setMediaId(BrowseIds.of(node))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(mediaTypeOf(node))
                    .build(),
            )
            .build()

    private fun mediaTypeOf(node: BrowseNode): Int = when (node) {
        BrowseNode.Playlists -> MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS
        BrowseNode.Albums -> MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS
        BrowseNode.Artists -> MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS
        is BrowseNode.Playlist -> MediaMetadata.MEDIA_TYPE_PLAYLIST
        is BrowseNode.Album -> MediaMetadata.MEDIA_TYPE_ALBUM
        is BrowseNode.Artist -> MediaMetadata.MEDIA_TYPE_ARTIST
        else -> MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
    }
}
