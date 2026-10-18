package com.kingplay.data.library.scan

/** Enough to decide whether a file changed. [folderPath] lets the scanner apply excluded folders early. */
data class Fingerprint(
    val modifiedMs: Long,
    val sizeBytes: Long,
    val folderPath: String,
)

/** An audio file as reported by the platform, with tags still raw (nullable, possibly `<unknown>`). */
data class ScannedAudio(
    val id: Long,
    val uri: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val genre: String?,
    val durationMs: Long,
    val bitrateKbps: Int,
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int?,
    val folderPath: String,
    val fileName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val dateAddedMs: Long,
    val dateModifiedMs: Long,
)

data class ScannedVideo(
    val id: Long,
    val uri: String,
    val title: String?,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val folderPath: String,
    val fileName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val dateAddedMs: Long,
    val dateModifiedMs: Long,
)

data class ScanRequest(
    /** Re-read every file instead of only new and changed ones. */
    val full: Boolean = false,
    val audio: Boolean = true,
    val video: Boolean = true,
)

data class ScanSummary(
    val audioChanged: Int = 0,
    val audioRemoved: Int = 0,
    val videoChanged: Int = 0,
    val videoRemoved: Int = 0,
)

data class PendingDetails(
    val id: Long,
    val uri: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val bitrateKbps: Int,
)

data class DetailsResult(
    val id: Long,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
)

data class AudioDetails(
    val bitrateKbps: Int,
    val sampleRateHz: Int,
)

/** Reads the platform media index (MediaStore). Implemented on Android; faked in tests. */
interface MediaSource {
    suspend fun audioFingerprints(): Map<Long, Fingerprint>

    suspend fun loadAudio(ids: List<Long>): List<ScannedAudio>

    suspend fun videoFingerprints(): Map<Long, Fingerprint>

    suspend fun loadVideo(ids: List<Long>): List<ScannedVideo>
}

/** The library as stored in Room, expressed without Room types so the scan logic stays testable. */
interface LibraryStore {
    suspend fun audioFingerprints(): Map<Long, Fingerprint>

    suspend fun videoFingerprints(): Map<Long, Fingerprint>

    suspend fun upsertAudio(items: List<ScannedAudio>)

    suspend fun upsertVideo(items: List<ScannedVideo>)

    suspend fun deleteAudio(ids: List<Long>)

    suspend fun deleteVideo(ids: List<Long>)

    suspend fun pendingDetails(limit: Int): List<PendingDetails>

    suspend fun pendingDetailsCount(): Int

    suspend fun saveDetails(results: List<DetailsResult>)
}

/** Reads sample rate and bitrate by opening the file. Returns null when the file cannot be read. */
interface AudioMetadataReader {
    suspend fun read(uri: String): AudioDetails?
}
