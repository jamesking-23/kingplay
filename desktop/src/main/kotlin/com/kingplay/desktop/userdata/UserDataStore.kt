package com.kingplay.desktop.userdata

import com.kingplay.core.model.RepeatMode
import com.kingplay.desktop.library.JsonFile
import java.io.File

class UserDataStore(private val file: File) {

    fun load(): UserData {
        val root = JsonFile.read(file) ?: return UserData()
        return runCatching { decode(root) }.getOrDefault(UserData()) // damaged data must not stop the app
    }

    fun save(data: UserData) = JsonFile.write(file, encode(data))

    companion object {
        private const val VERSION = 1L

        internal fun encode(data: UserData): Map<String, Any?> = mapOf(
            "version" to VERSION,
            "nextPlaylistId" to data.nextPlaylistId,
            "playlists" to data.playlists.map {
                mapOf("id" to it.id, "name" to it.name, "createdAtMs" to it.createdAtMs, "tracks" to it.trackIds)
            },
            "favorites" to data.favorites.toList(),
            "stats" to data.stats.map { (id, s) ->
                mapOf("id" to id, "count" to s.count.toLong(), "lastPlayedMs" to s.lastPlayedMs)
            },
            "queue" to data.queue?.let {
                mapOf(
                    "tracks" to it.trackIds,
                    "index" to it.index.toLong(),
                    "positionMs" to it.positionMs,
                    "shuffle" to it.shuffle,
                    "repeat" to it.repeat.name,
                )
            },
        )

        internal fun decode(root: Map<*, *>): UserData {
            val playlists = list(root["playlists"]).mapNotNull { raw ->
                val m = raw as? Map<*, *> ?: return@mapNotNull null
                val id = (m["id"] as? Number)?.toLong() ?: return@mapNotNull null
                DesktopPlaylist(
                    id = id,
                    name = (m["name"] as? String)?.ifBlank { null } ?: UserData.DEFAULT_NAME,
                    createdAtMs = (m["createdAtMs"] as? Number)?.toLong() ?: 0,
                    trackIds = longs(m["tracks"]),
                )
            }.distinctBy { it.id }
            val stats = list(root["stats"]).mapNotNull { raw ->
                val m = raw as? Map<*, *> ?: return@mapNotNull null
                val id = (m["id"] as? Number)?.toLong() ?: return@mapNotNull null
                id to PlayStat(
                    count = ((m["count"] as? Number)?.toInt() ?: 0).coerceAtLeast(0),
                    lastPlayedMs = (m["lastPlayedMs"] as? Number)?.toLong() ?: 0,
                )
            }.toMap()
            val queue = (root["queue"] as? Map<*, *>)?.let { q ->
                SavedQueue(
                    trackIds = longs(q["tracks"]),
                    index = (q["index"] as? Number)?.toInt() ?: 0,
                    positionMs = ((q["positionMs"] as? Number)?.toLong() ?: 0).coerceAtLeast(0),
                    shuffle = q["shuffle"] as? Boolean ?: false,
                    repeat = runCatching { RepeatMode.valueOf(q["repeat"] as? String ?: "") }
                        .getOrDefault(RepeatMode.OFF),
                ).takeIf { it.trackIds.isNotEmpty() }
            }
            val highest = playlists.maxOfOrNull { it.id } ?: 0
            return UserData(
                playlists = playlists,
                favorites = longs(root["favorites"]).toSet(),
                stats = stats,
                queue = queue,
                // Never reuse an id even if the saved counter is missing or stale.
                nextPlaylistId = maxOf((root["nextPlaylistId"] as? Number)?.toLong() ?: 1, highest + 1),
            )
        }

        private fun list(value: Any?): List<Any?> = (value as? List<*>).orEmpty()

        private fun longs(value: Any?): List<Long> = list(value).mapNotNull { (it as? Number)?.toLong() }
    }
}
