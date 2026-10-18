package com.kingplay.data.library.repo

import androidx.room.withTransaction
import com.kingplay.core.database.KingPlayDatabase
import com.kingplay.core.database.dao.SyncRow
import com.kingplay.core.database.dao.TrackDao
import com.kingplay.core.database.dao.VideoDao
import com.kingplay.core.database.entity.TrackEntity
import com.kingplay.core.database.entity.VideoEntity
import com.kingplay.data.library.scan.DetailsResult
import com.kingplay.data.library.scan.Fingerprint
import com.kingplay.data.library.scan.LibraryStore
import com.kingplay.data.library.scan.PendingDetails
import com.kingplay.data.library.scan.ScannedAudio
import com.kingplay.data.library.scan.ScannedVideo
import com.kingplay.data.library.scan.StableIds
import com.kingplay.data.library.scan.albumId
import com.kingplay.data.library.scan.audioFormat
import com.kingplay.data.library.scan.cleanTag
import com.kingplay.data.library.scan.resolveAlbum
import com.kingplay.data.library.scan.resolveArtist
import com.kingplay.data.library.scan.titleOrFileName
import com.kingplay.data.library.scan.titleSortKey
import com.kingplay.data.library.scan.videoContainer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomLibraryStore @Inject constructor(
    private val db: KingPlayDatabase,
    private val trackDao: TrackDao,
    private val videoDao: VideoDao,
) : LibraryStore {

    override suspend fun audioFingerprints(): Map<Long, Fingerprint> = trackDao.syncSnapshot().toFingerprints()

    override suspend fun videoFingerprints(): Map<Long, Fingerprint> = videoDao.syncSnapshot().toFingerprints()

    override suspend fun upsertAudio(items: List<ScannedAudio>) = trackDao.upsertAll(items.map { it.toEntity() })

    override suspend fun upsertVideo(items: List<ScannedVideo>) = videoDao.upsertAll(items.map { it.toEntity() })

    override suspend fun deleteAudio(ids: List<Long>) {
        ids.chunked(SQL_CHUNK).forEach { trackDao.deleteByIds(it) }
    }

    override suspend fun deleteVideo(ids: List<Long>) {
        ids.chunked(SQL_CHUNK).forEach { videoDao.deleteByIds(it) }
    }

    override suspend fun pendingDetails(limit: Int): List<PendingDetails> =
        trackDao.pendingDetails(limit).map {
            PendingDetails(it.id, it.uri, it.sizeBytes, it.durationMs, it.bitrateKbps)
        }

    override suspend fun pendingDetailsCount(): Int = trackDao.pendingDetailsCount()

    override suspend fun saveDetails(results: List<DetailsResult>) {
        db.withTransaction {
            results.forEach { trackDao.updateDetails(it.id, it.bitrateKbps, it.sampleRateHz) }
        }
    }

    private fun List<SyncRow>.toFingerprints(): Map<Long, Fingerprint> =
        associate { it.id to Fingerprint(it.dateModifiedMs, it.sizeBytes, it.folderPath) }

    private companion object {
        const val SQL_CHUNK = 500
    }
}

internal fun ScannedAudio.toEntity(): TrackEntity {
    val resolvedTitle = titleOrFileName(title, fileName)
    val artistName = resolveArtist(artist)
    val albumTitle = resolveAlbum(album)
    val albumArtistName = albumArtist.cleanTag()
    return TrackEntity(
        id = id,
        uri = uri,
        title = resolvedTitle,
        titleSort = titleSortKey(resolvedTitle),
        artistId = StableIds.artist(artistName),
        artistName = artistName,
        albumId = albumId(albumTitle, albumArtistName, folderPath),
        albumTitle = albumTitle,
        albumArtistId = albumArtistName?.let(StableIds::artist),
        albumArtist = albumArtistName,
        genre = genre.cleanTag(),
        durationMs = durationMs,
        bitrateKbps = bitrateKbps,
        sampleRateHz = 0,
        format = audioFormat(fileName, mimeType),
        trackNumber = trackNumber,
        discNumber = discNumber,
        year = year,
        folderPath = folderPath,
        fileName = fileName,
        sizeBytes = sizeBytes,
        dateAddedMs = dateAddedMs,
        dateModifiedMs = dateModifiedMs,
        needsDetails = true,
        rgTrackGainDb = null,
        rgAlbumGainDb = null,
        rgTrackPeak = null,
        rgAlbumPeak = null,
    )
}

internal fun ScannedVideo.toEntity(): VideoEntity {
    val resolvedTitle = titleOrFileName(title, fileName)
    return VideoEntity(
        id = id,
        uri = uri,
        title = resolvedTitle,
        titleSort = titleSortKey(resolvedTitle),
        durationMs = durationMs,
        width = width,
        height = height,
        videoCodec = null,
        container = videoContainer(fileName, mimeType),
        folderPath = folderPath,
        fileName = fileName,
        sizeBytes = sizeBytes,
        dateAddedMs = dateAddedMs,
        dateModifiedMs = dateModifiedMs,
    )
}
