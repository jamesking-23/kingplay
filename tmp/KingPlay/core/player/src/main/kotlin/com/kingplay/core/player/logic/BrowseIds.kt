package com.kingplay.core.player.logic

/**
 * The folders of the browse tree that Android Auto and other media browsers show. Songs inside them keep their own
 * playable ids (`a:<id>`); these ids are only for the folders.
 */
sealed interface BrowseNode {
    data object Root : BrowseNode

    data object Recent : BrowseNode

    data object Playlists : BrowseNode

    data object Albums : BrowseNode

    data object Artists : BrowseNode

    /** Smart lists shown at the top of [Playlists]. */
    data object Favorites : BrowseNode

    data object MostPlayed : BrowseNode

    data object RecentlyAdded : BrowseNode

    data class Playlist(val id: Long) : BrowseNode

    data class Album(val id: Long) : BrowseNode

    data class Artist(val id: Long) : BrowseNode
}

object BrowseIds {
    const val ROOT_ID = "kingplay_root"
    private const val PREFIX = "browse:"

    /** Android Auto shows at most four tabs, so the root holds exactly four folders. */
    val rootChildren: List<BrowseNode> =
        listOf(BrowseNode.Recent, BrowseNode.Playlists, BrowseNode.Albums, BrowseNode.Artists)

    val smartLists: List<BrowseNode> =
        listOf(BrowseNode.Favorites, BrowseNode.MostPlayed, BrowseNode.RecentlyAdded)

    fun of(node: BrowseNode): String = when (node) {
        BrowseNode.Root -> ROOT_ID
        BrowseNode.Recent -> "${PREFIX}recent"
        BrowseNode.Playlists -> "${PREFIX}playlists"
        BrowseNode.Albums -> "${PREFIX}albums"
        BrowseNode.Artists -> "${PREFIX}artists"
        BrowseNode.Favorites -> "${PREFIX}favorites"
        BrowseNode.MostPlayed -> "${PREFIX}most"
        BrowseNode.RecentlyAdded -> "${PREFIX}added"
        is BrowseNode.Playlist -> "${PREFIX}playlist:${node.id}"
        is BrowseNode.Album -> "${PREFIX}album:${node.id}"
        is BrowseNode.Artist -> "${PREFIX}artist:${node.id}"
    }

    fun parse(id: String?): BrowseNode? {
        if (id == null) return null
        if (id == ROOT_ID) return BrowseNode.Root
        if (!id.startsWith(PREFIX)) return null
        val body = id.removePrefix(PREFIX)
        return when (body) {
            "recent" -> BrowseNode.Recent
            "playlists" -> BrowseNode.Playlists
            "albums" -> BrowseNode.Albums
            "artists" -> BrowseNode.Artists
            "favorites" -> BrowseNode.Favorites
            "most" -> BrowseNode.MostPlayed
            "added" -> BrowseNode.RecentlyAdded
            else -> parseWithId(body)
        }
    }

    private fun parseWithId(body: String): BrowseNode? {
        val kind = body.substringBefore(':', missingDelimiterValue = "")
        val id = body.substringAfter(':', missingDelimiterValue = "").toLongOrNull() ?: return null
        return when (kind) {
            "playlist" -> BrowseNode.Playlist(id)
            "album" -> BrowseNode.Album(id)
            "artist" -> BrowseNode.Artist(id)
            else -> null
        }
    }

    /** Folders that list songs directly. Asking to play one of their songs queues the whole list. */
    fun listsSongs(node: BrowseNode): Boolean = when (node) {
        BrowseNode.Root, BrowseNode.Playlists, BrowseNode.Albums, BrowseNode.Artists -> false
        else -> true
    }
}

/** Android Auto truncates long lists; these keep each answer small and quick. */
const val BROWSE_SONG_LIMIT = 100
const val BROWSE_FOLDER_LIMIT = 200
const val SEARCH_LIMIT = 50

/** The slice of [items] a browser asked for with Media3's zero-based [page] and [pageSize]. */
fun <T> pageOf(items: List<T>, page: Int, pageSize: Int): List<T> {
    if (page < 0 || pageSize <= 0) return emptyList()
    val from = page.toLong() * pageSize
    if (from >= items.size) return emptyList()
    return items.subList(from.toInt(), minOf(items.size.toLong(), from + pageSize).toInt())
}

/**
 * When someone taps one song in a list, the whole list should play from that song. Returns the queue and start index
 * to use, or null when [tappedId] was not in [listIds] (then just the one song plays).
 */
fun <T> expandToList(listIds: List<T>, tappedId: T): Pair<List<T>, Int>? {
    val index = listIds.indexOf(tappedId)
    return if (index < 0) null else listIds to index
}
