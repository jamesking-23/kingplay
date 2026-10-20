package com.kingplay.desktop.userdata

import com.kingplay.core.model.EqPreset
import com.kingplay.core.model.RepeatMode
import com.kingplay.desktop.video.VideoProgress
import com.kingplay.desktop.video.resumePositionMs

data class DesktopPlaylist(
    val id: Long,
    val name: String,
    val createdAtMs: Long,
    /** Track ids in order. An id can be missing from the library (file moved); it is kept and skipped when shown. */
    val trackIds: List<Long>,
)

/** An equalizer curve the person saved under a name. Always ten band gains. */
data class UserEqPreset(val name: String, val preampDb: Float, val gainsDb: List<Float>)

data class PlayStat(val count: Int, val lastPlayedMs: Long)

/** One counted play, kept so the monthly report can tell which month it belongs to. */
data class PlayEvent(val trackId: Long, val atMs: Long)

data class SavedQueue(
    val trackIds: List<Long>,
    val index: Int,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeat: RepeatMode,
)

/**
 * Everything the person made inside KingPlay on this PC. Kept apart from the scanned library,
 * so a rescan never touches it.
 */
data class UserData(
    val playlists: List<DesktopPlaylist> = emptyList(),
    val favorites: Set<Long> = emptySet(),
    val stats: Map<Long, PlayStat> = emptyMap(),
    val queue: SavedQueue? = null,
    val nextPlaylistId: Long = 1,
    /** Lyrics the person imported, by lyrics key (artist and title). They win over embedded and sidecar lyrics. */
    val lyrics: Map<String, String> = emptyMap(),
    val eqPresets: List<UserEqPreset> = emptyList(),
    /** Where each video was left, by video id. Finished or barely started videos are not kept. */
    val videoProgress: Map<Long, VideoProgress> = emptyMap(),
    /** Counted plays, oldest first, capped at [MAX_HISTORY]. */
    val history: List<PlayEvent> = emptyList(),
    /** The month ("yyyy-MM") whose "report is ready" notice was already shown. */
    val reportShownFor: String? = null,
) {
    /** Saves (or replaces, ignoring letter case) a named equalizer curve. Blank names and the limit are refused. */
    fun withEqPreset(preset: UserEqPreset): UserData {
        val name = preset.name.trim()
        if (name.isEmpty() || preset.gainsDb.size != EQ_BANDS) return this
        val rest = eqPresets.filterNot { it.name.equals(name, ignoreCase = true) }
        if (rest.size >= MAX_EQ_PRESETS) return this
        return copy(eqPresets = rest + preset.copy(name = name))
    }

    fun withVideoProgress(id: Long, positionMs: Long, durationMs: Long): UserData {
        val progress = VideoProgress(positionMs.coerceAtLeast(0), durationMs.coerceAtLeast(0))
        return if (resumePositionMs(progress) > 0) {
            copy(videoProgress = videoProgress + (id to progress))
        } else {
            copy(videoProgress = videoProgress - id)
        }
    }

    fun withoutEqPreset(name: String): UserData = copy(eqPresets = eqPresets.filterNot { it.name.equals(name, true) })

    fun playlist(id: Long): DesktopPlaylist? = playlists.firstOrNull { it.id == id }

    fun isFavorite(trackId: Long): Boolean = trackId in favorites

    /** Creates a playlist; a blank name becomes "New playlist" and a taken name gets " (2)", " (3)"… */
    fun createPlaylist(name: String, now: Long, trackIds: List<Long> = emptyList()): Pair<UserData, Long> {
        val unique = uniqueName(name.trim().ifEmpty { DEFAULT_NAME }, taken = playlists.map { it.name })
        val playlist = DesktopPlaylist(nextPlaylistId, unique, now, trackIds)
        return copy(playlists = playlists + playlist, nextPlaylistId = nextPlaylistId + 1) to playlist.id
    }

    fun renamePlaylist(id: Long, name: String): UserData {
        val clean = name.trim()
        if (clean.isEmpty()) return this
        val others = playlists.filter { it.id != id }.map { it.name }
        return updatePlaylist(id) { it.copy(name = uniqueName(clean, others)) }
    }

    fun deletePlaylist(id: Long): UserData = copy(playlists = playlists.filter { it.id != id })

    fun addToPlaylist(id: Long, trackIds: List<Long>): UserData =
        updatePlaylist(id) { it.copy(trackIds = it.trackIds + trackIds) }

    fun removeFromPlaylist(id: Long, position: Int): UserData = updatePlaylist(id) {
        if (position !in it.trackIds.indices) {
            it
        } else {
            it.copy(trackIds = it.trackIds.filterIndexed { i, _ -> i != position })
        }
    }

    fun moveInPlaylist(id: Long, from: Int, to: Int): UserData = updatePlaylist(id) {
        if (from !in it.trackIds.indices || to !in it.trackIds.indices) {
            it
        } else {
            it.copy(trackIds = it.trackIds.toMutableList().apply { add(to, removeAt(from)) })
        }
    }

    fun toggleFavorite(trackId: Long): UserData =
        copy(favorites = if (trackId in favorites) favorites - trackId else favorites + trackId)

    fun recordPlay(trackId: Long, now: Long): UserData {
        val old = stats[trackId]
        return copy(
            stats = stats + (trackId to PlayStat((old?.count ?: 0) + 1, now)),
            history = (history + PlayEvent(trackId, now)).takeLast(MAX_HISTORY),
        )
    }

    fun withReportShown(month: String): UserData = copy(reportShownFor = month)

    fun withQueue(queue: SavedQueue?): UserData = copy(queue = queue)

    fun withLyrics(key: String, lrc: String): UserData = copy(lyrics = lyrics + (key to lrc))

    fun withoutLyrics(key: String): UserData = copy(lyrics = lyrics - key)

    private fun updatePlaylist(id: Long, change: (DesktopPlaylist) -> DesktopPlaylist): UserData =
        copy(playlists = playlists.map { if (it.id == id) change(it) else it })

    companion object {
        const val DEFAULT_NAME = "New playlist"
        const val MAX_EQ_PRESETS = 30
        const val MAX_HISTORY = 50_000
        private const val EQ_BANDS = EqPreset.BAND_COUNT

        fun uniqueName(wanted: String, taken: List<String>): String {
            val lower = taken.map { it.lowercase() }.toSet()
            if (wanted.lowercase() !in lower) return wanted
            var n = 2
            while ("$wanted ($n)".lowercase() in lower) n++
            return "$wanted ($n)"
        }
    }
}
