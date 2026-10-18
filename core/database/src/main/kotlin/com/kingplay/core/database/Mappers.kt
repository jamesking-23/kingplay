package com.kingplay.core.database

import com.kingplay.core.database.dao.AlbumRow
import com.kingplay.core.database.dao.ArtistRow
import com.kingplay.core.database.dao.FolderCountRow
import com.kingplay.core.database.dao.GenreRow
import com.kingplay.core.database.dao.PlaylistRow
import com.kingplay.core.database.dao.TrackRow
import com.kingplay.core.database.dao.VideoRow
import com.kingplay.core.database.entity.DeviceProfileEntity
import com.kingplay.core.database.entity.EqPresetEntity
import com.kingplay.core.database.entity.PlaylistEntryEntity
import com.kingplay.core.database.entity.QueueItemEntity
import com.kingplay.core.database.entity.QueueStateEntity
import com.kingplay.core.database.entity.RefType
import com.kingplay.core.model.Album
import com.kingplay.core.model.Artist
import com.kingplay.core.model.DeviceProfile
import com.kingplay.core.model.EffectSettings
import com.kingplay.core.model.EqPreset
import com.kingplay.core.model.Genre
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.OutputDeviceType
import com.kingplay.core.model.Playlist
import com.kingplay.core.model.PlaylistEntry
import com.kingplay.core.model.QueueState
import com.kingplay.core.model.RepeatMode
import com.kingplay.core.model.ReplayGain
import com.kingplay.core.model.ReplayGainMode
import com.kingplay.core.model.Track
import com.kingplay.core.model.VideoItem

fun TrackRow.toModel(): Track {
    val t = track
    val gain = if (t.rgTrackGainDb == null && t.rgAlbumGainDb == null) {
        null
    } else {
        ReplayGain(t.rgTrackGainDb, t.rgAlbumGainDb, t.rgTrackPeak, t.rgAlbumPeak)
    }
    return Track(
        id = t.id,
        uri = t.uri,
        title = t.title,
        artistId = t.artistId,
        artistName = t.artistName,
        albumId = t.albumId,
        albumTitle = t.albumTitle,
        albumArtist = t.albumArtist,
        genre = t.genre,
        durationMs = t.durationMs,
        bitrateKbps = t.bitrateKbps,
        sampleRateHz = t.sampleRateHz,
        format = t.format,
        trackNumber = t.trackNumber,
        discNumber = t.discNumber,
        year = t.year,
        folderPath = t.folderPath,
        dateAddedMs = t.dateAddedMs,
        dateModifiedMs = t.dateModifiedMs,
        replayGain = gain,
        playCount = playCount,
        lastPlayedMs = lastPlayedMs,
        isFavorite = isFavorite,
    )
}

fun VideoRow.toModel(): VideoItem = VideoItem(
    id = video.id,
    uri = video.uri,
    title = video.title,
    durationMs = video.durationMs,
    width = video.width,
    height = video.height,
    videoCodec = video.videoCodec,
    container = video.container,
    folderPath = video.folderPath,
    sizeBytes = video.sizeBytes,
    dateAddedMs = video.dateAddedMs,
    resumePositionMs = resumePositionMs,
    lastPlayedMs = lastPlayedMs,
)

fun AlbumRow.toModel(): Album = Album(
    id = id,
    title = title,
    artistName = artistName,
    artworkUri = artworkUri,
    year = year?.takeIf { it > 0 },
    trackCount = trackCount,
    totalDurationMs = totalDurationMs,
)

fun ArtistRow.toModel(): Artist = Artist(
    id = id,
    name = name,
    albumCount = albumCount,
    trackCount = trackCount,
    isAlbumArtist = isAlbumArtist,
)

fun GenreRow.toModel(): Genre = Genre(name = name, trackCount = trackCount)

fun PlaylistRow.toModel(): Playlist = Playlist(
    id = id,
    name = name,
    createdAtMs = createdAtMs,
    updatedAtMs = updatedAtMs,
    entryCount = entryCount,
)

fun PlaylistEntryEntity.toModel(): PlaylistEntry = PlaylistEntry(
    playlistId = playlistId,
    position = position,
    ref = toRef(refType, refId),
)

fun MediaRef.refType(): Int = when (this) {
    is MediaRef.Audio -> RefType.AUDIO
    is MediaRef.Video -> RefType.VIDEO
}

fun toRef(refType: Int, refId: Long): MediaRef =
    if (refType == RefType.VIDEO) MediaRef.Video(refId) else MediaRef.Audio(refId)

fun QueueState.toEntities(): Pair<QueueStateEntity, List<QueueItemEntity>> {
    val state = QueueStateEntity(
        currentIndex = currentIndex,
        positionMs = positionMs,
        shuffleEnabled = shuffleEnabled,
        shuffleOrder = shuffleOrder.joinToString(","),
        repeatMode = repeatMode.name,
    )
    val rows = items.mapIndexed { index, ref -> QueueItemEntity(index, ref.refType(), ref.id) }
    return state to rows
}

fun queueStateOf(state: QueueStateEntity?, items: List<QueueItemEntity>): QueueState {
    if (state == null) return QueueState()
    val refs = items.map { toRef(it.refType, it.refId) }
    val index = if (state.currentIndex in refs.indices) state.currentIndex else -1
    return QueueState(
        items = refs,
        currentIndex = index,
        positionMs = if (index >= 0) state.positionMs else 0,
        shuffleEnabled = state.shuffleEnabled,
        shuffleOrder = state.shuffleOrder.parseIntList().filter { it in refs.indices },
        repeatMode = enumOrDefault(state.repeatMode, RepeatMode.OFF),
    )
}

fun EqPresetEntity.toModel(): EqPreset = EqPreset(
    id = id,
    name = name,
    preampDb = preampDb,
    bandGainsDb = parseBandGains(bandGainsDb),
    isBuiltIn = false,
)

fun EqPreset.toEntity(): EqPresetEntity = EqPresetEntity(
    id = if (id > 0) id else 0,
    name = name,
    preampDb = preampDb,
    bandGainsDb = bandGainsDb.joinToString(","),
)

/** Tolerates a damaged value by falling back to a flat curve instead of crashing the EQ screen. */
internal fun parseBandGains(raw: String): List<Float> {
    val parsed = raw.split(',').mapNotNull { it.trim().toFloatOrNull() }
    return if (parsed.size == EqPreset.BAND_COUNT) parsed else List(EqPreset.BAND_COUNT) { 0f }
}

fun DeviceProfileEntity.toModel(): DeviceProfile = DeviceProfile(
    deviceKey = deviceKey,
    deviceType = enumOrDefault(deviceType, OutputDeviceType.OTHER),
    displayName = displayName,
    eqPresetId = eqPresetId,
    effects = EffectSettings(
        bassBoostStrength = bassBoostStrength,
        loudnessGainMb = loudnessGainMb,
        tempo = tempo,
        pitchSemitones = pitchSemitones,
    ),
    replayGainMode = enumOrDefault(replayGainMode, ReplayGainMode.OFF),
)

fun DeviceProfile.toEntity(): DeviceProfileEntity = DeviceProfileEntity(
    deviceKey = deviceKey,
    deviceType = deviceType.name,
    displayName = displayName,
    eqPresetId = eqPresetId,
    bassBoostStrength = effects.bassBoostStrength,
    loudnessGainMb = effects.loudnessGainMb,
    tempo = effects.tempo,
    pitchSemitones = effects.pitchSemitones,
    replayGainMode = replayGainMode.name,
)

internal fun String.parseIntList(): List<Int> =
    if (isBlank()) emptyList() else split(',').mapNotNull { it.trim().toIntOrNull() }

private inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

/** Folder rows from the track and video tables share one shape. */
fun List<FolderCountRow>.asMap(): Map<String, Int> = associate { it.path to it.count }
