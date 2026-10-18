package com.kingplay.desktop.userdata

import com.kingplay.core.model.Track
import com.kingplay.core.model.backup.BackupCodec
import com.kingplay.core.model.backup.BackupData
import com.kingplay.core.model.backup.BackupPlayStat
import com.kingplay.core.model.backup.BackupPlaylist
import com.kingplay.core.model.backup.BackupPreset
import com.kingplay.core.model.backup.BackupTrack
import com.kingplay.core.model.settings.AppSettings
import com.kingplay.desktop.library.LibraryIndex

/**
 * Writes playlists and favorites in the same backup format the Android app reads, so they can go phone-ward.
 * Paths use '/' because the phone matches by folder and file name. Songs no longer in the library are left out
 * (their paths are unknown). Imported lyrics, saved equalizer presets and play counts go along. Marked as a
 * desktop backup so restoring it does not touch the phone's settings.
 */
object BackupExport {
    fun build(library: LibraryIndex, data: UserData, now: Long): String {
        fun entry(track: Track) = BackupTrack(track.uri.replace('\\', '/'), track.title, track.artistName)
        return BackupCodec.encode(
            BackupData(
                createdAtMs = now,
                settings = AppSettings(),
                playlists = data.playlists.map { playlist ->
                    BackupPlaylist(playlist.name, playlist.createdAtMs, playlistTracks(playlist, library).map(::entry))
                },
                favorites = favoriteTracks(library, data).map(::entry),
                presets = data.eqPresets.map { BackupPreset(it.name, it.preampDb, it.gainsDb) },
                excludedFolders = emptyList(),
                lyrics = data.lyrics,
                source = BackupCodec.SOURCE_DESKTOP,
                playStats = data.stats.mapNotNull { (id, stat) ->
                    val track = library[id]?.takeIf { stat.count > 0 } ?: return@mapNotNull null
                    BackupPlayStat(entry(track), stat.count, stat.lastPlayedMs)
                },
            ),
        )
    }
}
