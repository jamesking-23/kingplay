package com.kingplay.data.library.repo

import com.kingplay.core.database.dao.TrackDao
import com.kingplay.core.model.EqPreset
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.Track
import com.kingplay.core.model.backup.BackupCodec
import com.kingplay.core.model.backup.BackupData
import com.kingplay.core.model.backup.BackupFormatException
import com.kingplay.core.model.backup.BackupPlaylist
import com.kingplay.core.model.backup.BackupPreset
import com.kingplay.core.model.backup.BackupTrack
import com.kingplay.core.model.backup.RestoreResult
import com.kingplay.core.model.backup.RestoreSummary
import com.kingplay.core.model.playlist.PathRow
import com.kingplay.core.model.playlist.TrackPathIndex
import com.kingplay.core.model.repository.BackupRepository
import com.kingplay.core.model.repository.EqRepository
import com.kingplay.core.model.repository.LibraryFolderSettings
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.LyricsStore
import com.kingplay.core.model.repository.PlaylistRepository
import com.kingplay.core.model.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backs up what the user made: playlists, favorites, saved EQ presets, excluded folders, imported lyrics and settings.
 * Songs are written as file paths, not library ids, and matched again by path on restore (the same looser matching
 * the M3U import uses), so a backup survives a rebuilt media index or a new phone.
 */
@Singleton
class DefaultBackupRepository @Inject constructor(
    private val trackDao: TrackDao,
    private val library: LibraryRepository,
    private val playlists: PlaylistRepository,
    private val eq: EqRepository,
    private val folders: LibraryFolderSettings,
    private val lyrics: LyricsStore,
    private val settings: SettingsRepository,
) : BackupRepository {

    override suspend fun create(): String {
        val paths = trackDao.allPaths().associate { it.id to "${it.folderPath.trimEnd('/')}/${it.fileName}" }

        fun backupOf(track: Track): BackupTrack? =
            paths[track.id]?.let { BackupTrack(it, track.title, track.artistName) }

        val savedPlaylists = playlists.playlists().first().map { playlist ->
            val ids = playlists.entries(playlist.id).first().mapNotNull { (it.ref as? MediaRef.Audio)?.id }
            BackupPlaylist(playlist.name, playlist.createdAtMs, library.getTracks(ids).mapNotNull(::backupOf))
        }
        val data = BackupData(
            createdAtMs = System.currentTimeMillis(),
            settings = settings.settings.first(),
            playlists = savedPlaylists,
            favorites = library.favorites().first().mapNotNull(::backupOf),
            presets = eq.presets().first().filter { !it.isBuiltIn }
                .map { BackupPreset(it.name, it.preampDb, it.bandGainsDb) },
            excludedFolders = folders.excludedFolders.first(),
            lyrics = lyrics.all(),
        )
        return BackupCodec.encode(data)
    }

    override suspend fun restore(text: String): RestoreResult {
        val data = try {
            BackupCodec.decode(text)
        } catch (e: BackupFormatException) {
            return RestoreResult.Invalid(e.message ?: "The file is not a KingPlay backup")
        }
        val index = TrackPathIndex(trackDao.allPaths().map { PathRow(it.id, it.folderPath, it.fileName) })

        val existingNames = playlists.playlists().first().map { it.name.lowercase() }.toMutableSet()
        var added = 0
        var skipped = 0
        var matched = 0
        var total = 0
        for (playlist in data.playlists.map { it.copy(name = it.name.ifBlank { "Restored playlist" }) }) {
            if (!existingNames.add(playlist.name.lowercase())) {
                skipped++
                continue
            }
            val ids = playlist.tracks.mapNotNull { index.match(it.path) }
            total += playlist.tracks.size
            matched += ids.size
            playlists.createFrom(playlist.name, ids.map { MediaRef.Audio(it) })
            added++
        }

        val favoriteIds = data.favorites.mapNotNull { index.match(it.path) }
        favoriteIds.forEach { library.setFavorite(it, true) }

        val knownPresets = eq.presets().first().map { it.name.lowercase() }.toMutableSet()
        var presetsAdded = 0
        for (preset in data.presets) {
            if (!knownPresets.add(preset.name.lowercase())) continue
            eq.savePreset(EqPreset(0, preset.name, preset.preampDb, preset.bandGainsDb, isBuiltIn = false))
            presetsAdded++
        }

        data.excludedFolders.forEach { folders.exclude(it) }

        val haveLyrics = lyrics.all().keys
        var lyricsAdded = 0
        for ((key, lrc) in data.lyrics) {
            if (key in haveLyrics) continue
            lyrics.save(key, lrc)
            lyricsAdded++
        }

        settings.update { data.settings }
        return RestoreResult.Done(
            RestoreSummary(added, skipped, matched, total, favoriteIds.size, presetsAdded, lyricsAdded),
        )
    }
}
