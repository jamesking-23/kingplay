package com.kingplay.desktop.userdata

import com.kingplay.core.model.backup.BackupCodec
import com.kingplay.core.model.backup.BackupFormatException
import com.kingplay.core.model.playlist.PathRow
import com.kingplay.core.model.playlist.TrackPathIndex
import com.kingplay.desktop.library.LibraryIndex
import com.kingplay.desktop.library.stableId
import com.kingplay.desktop.settings.clampGain
import java.io.File

data class ImportSummary(
    val playlistsAdded: Int,
    val playlistsSkipped: Int,
    val tracksMatched: Int,
    val tracksTotal: Int,
    val favoritesMatched: Int,
    val favoritesTotal: Int,
    val lyricsAdded: Int = 0,
    val presetsAdded: Int = 0,
    val statsMatched: Int = 0,
    val videosMatched: Int = 0,
)

sealed interface ImportOutcome {
    data class Done(val data: UserData, val summary: ImportSummary) : ImportOutcome

    data class Invalid(val reason: String) : ImportOutcome
}

/**
 * Brings playlists and favorites from an Android KingPlay backup into this PC's data. Songs are found again by
 * file name (the phone's paths never exist here), and only an unambiguous match is accepted.
 * It only adds: playlists whose name already exists are skipped, favorites are merged, equalizer presets and lyrics
 * already here are kept, and play counts keep the larger number.
 */
object BackupImport {
    fun import(
        text: String,
        library: LibraryIndex,
        current: UserData,
        now: Long,
        videoExists: (String) -> Boolean = { File(it).isFile },
    ): ImportOutcome {
        val backup = try {
            BackupCodec.decode(text)
        } catch (e: BackupFormatException) {
            return ImportOutcome.Invalid(e.message ?: "This is not a KingPlay backup")
        }
        val paths = TrackPathIndex(
            library.tracks.map {
                PathRow(it.id, it.folderPath.replace('\\', '/'), it.uri.replace('\\', '/').substringAfterLast('/'))
            },
        )
        var data = current
        var added = 0
        var skipped = 0
        var matched = 0
        var total = 0
        val taken = current.playlists.map { it.name.lowercase() }.toMutableSet()
        for (playlist in backup.playlists) {
            val name = playlist.name.trim().ifEmpty { "Restored playlist" }
            if (!taken.add(name.lowercase())) {
                skipped++
                continue
            }
            val ids = playlist.tracks.mapNotNull { paths.match(it.path) }
            total += playlist.tracks.size
            matched += ids.size
            data = data.createPlaylist(name, playlist.createdAtMs.takeIf { it > 0 } ?: now, ids).first
            added++
        }
        val favoriteIds = backup.favorites.mapNotNull { paths.match(it.path) }
        data = data.copy(favorites = data.favorites + favoriteIds)
        val newLyrics = backup.lyrics.filterKeys { it !in data.lyrics }
        data = data.copy(lyrics = data.lyrics + newLyrics)
        val presetsBefore = data.eqPresets.size
        for (preset in backup.presets) {
            if (data.eqPresets.any { it.name.equals(preset.name.trim(), ignoreCase = true) }) continue
            val gains = preset.bandGainsDb.map(::clampGain)
            data = data.withEqPreset(UserEqPreset(preset.name, clampGain(preset.preampDb), gains))
        }
        var statsMatched = 0
        for (stat in backup.playStats) {
            val id = paths.match(stat.track.path) ?: continue
            val old = data.stats[id]
            // Largest wins, so importing the same file twice (or phone and PC both) never double counts.
            val merged = PlayStat(
                count = maxOf(old?.count ?: 0, stat.count),
                lastPlayedMs = maxOf(old?.lastPlayedMs ?: 0, stat.lastPlayedMs),
            )
            data = data.copy(stats = data.stats + (id to merged))
            statsMatched++
        }
        var videosMatched = 0
        for (v in backup.videoProgress) {
            // Only for videos that are really on this PC, and never over a position that is already here.
            if (!videoExists(v.path)) continue
            val id = stableId("video:${v.path}")
            if (id in data.videoProgress) continue
            data = data.withVideoProgress(id, v.positionMs, v.durationMs)
            videosMatched++
        }
        return ImportOutcome.Done(
            data,
            ImportSummary(
                added, skipped, matched, total, favoriteIds.size, backup.favorites.size, newLyrics.size,
                presetsAdded = data.eqPresets.size - presetsBefore,
                statsMatched = statsMatched,
                videosMatched = videosMatched,
            ),
        )
    }
}
