@file:Suppress("DEPRECATION")

package com.kingplay.data.library.platform

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.kingplay.core.common.Dispatcher
import com.kingplay.core.common.KpDispatchers
import com.kingplay.data.library.scan.Fingerprint
import com.kingplay.data.library.scan.MediaSource
import com.kingplay.data.library.scan.ScannedAudio
import com.kingplay.data.library.scan.ScannedVideo
import com.kingplay.data.library.scan.cleanTag
import com.kingplay.data.library.scan.decodeLegacyTrackNumber
import com.kingplay.data.library.scan.parseLeadingInt
import com.kingplay.data.library.scan.resolveFolder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Reads the MediaStore index. Fingerprints (id, modified time, size, folder) are cheap enough to read for the whole
 * library every scan; full rows are only requested for the ids that changed.
 */
@Singleton
class MediaStoreSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(KpDispatchers.IO) private val io: CoroutineDispatcher,
) : MediaSource {

    private val resolver: ContentResolver get() = context.contentResolver

    /** Genre per audio id on Android 10 and older, where MediaStore has no genre column. Reset every scan. */
    @Volatile
    private var legacyGenres: Map<Long, String>? = null

    override suspend fun audioFingerprints(): Map<Long, Fingerprint> = withContext(io) {
        legacyGenres = null
        val out = HashMap<Long, Fingerprint>()
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DATA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Audio.Media.RELATIVE_PATH)
        }.toTypedArray()
        query(AUDIO_URI, projection, AUDIO_SELECTION, arrayOf(MIN_AUDIO_DURATION_MS.toString())) { c ->
            out[c.long(MediaStore.Audio.Media._ID)] = fingerprint(c)
        }
        out
    }

    override suspend fun videoFingerprints(): Map<Long, Fingerprint> = withContext(io) {
        val out = HashMap<Long, Fingerprint>()
        val projection = buildList {
            add(MediaStore.Video.Media._ID)
            add(MediaStore.Video.Media.DATE_MODIFIED)
            add(MediaStore.Video.Media.SIZE)
            add(MediaStore.Video.Media.DATA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Video.Media.RELATIVE_PATH)
        }.toTypedArray()
        query(VIDEO_URI, projection, null, null) { c ->
            out[c.long(MediaStore.Video.Media._ID)] = fingerprint(c)
        }
        out
    }

    override suspend fun loadAudio(ids: List<Long>): List<ScannedAudio> = withContext(io) {
        if (ids.isEmpty()) return@withContext emptyList()
        val genres = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            legacyGenres ?: loadLegacyGenres().also { legacyGenres = it }
        } else {
            emptyMap()
        }
        val out = ArrayList<ScannedAudio>(ids.size)
        for (chunk in ids.chunked(QUERY_CHUNK)) {
            val selection = "${MediaStore.Audio.Media._ID} IN (${chunk.joinToString(",")})"
            query(AUDIO_URI, audioProjection(), selection, null) { c -> out += readAudio(c, genres) }
        }
        out
    }

    override suspend fun loadVideo(ids: List<Long>): List<ScannedVideo> = withContext(io) {
        if (ids.isEmpty()) return@withContext emptyList()
        val out = ArrayList<ScannedVideo>(ids.size)
        for (chunk in ids.chunked(QUERY_CHUNK)) {
            val selection = "${MediaStore.Video.Media._ID} IN (${chunk.joinToString(",")})"
            query(VIDEO_URI, videoProjection(), selection, null) { c -> out += readVideo(c) }
        }
        out
    }

    private fun readAudio(c: Cursor, legacyGenres: Map<Long, String>): ScannedAudio {
        val id = c.long(MediaStore.Audio.Media._ID)
        val data = c.stringOrNull(MediaStore.Audio.Media.DATA)
        val fileName = c.stringOrNull(MediaStore.Audio.Media.DISPLAY_NAME)
            ?: data?.substringAfterLast('/').orEmpty()

        val legacyTrack = c.intOrZero(MediaStore.Audio.Media.TRACK)
        val (legacyDisc, legacyTrackNumber) = decodeLegacyTrackNumber(legacyTrack)
        var trackNumber = legacyTrackNumber
        var discNumber = legacyDisc
        var albumArtist: String? = null
        var genre: String? = legacyGenres[id]
        var bitrateKbps = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            parseLeadingInt(c.stringOrNull(COL_CD_TRACK_NUMBER))?.let { trackNumber = it }
            parseLeadingInt(c.stringOrNull(COL_DISC_NUMBER))?.let { discNumber = it }
            albumArtist = c.stringOrNull(COL_ALBUM_ARTIST)
            genre = c.stringOrNull(COL_GENRE) ?: genre
            bitrateKbps = c.intOrZero(COL_BITRATE) / BITS_PER_KILOBIT
        }

        return ScannedAudio(
            id = id,
            uri = ContentUris.withAppendedId(AUDIO_URI, id).toString(),
            title = c.stringOrNull(MediaStore.Audio.Media.TITLE),
            artist = c.stringOrNull(MediaStore.Audio.Media.ARTIST),
            album = c.stringOrNull(MediaStore.Audio.Media.ALBUM),
            albumArtist = albumArtist,
            genre = genre,
            durationMs = c.longOrZero(MediaStore.Audio.Media.DURATION),
            bitrateKbps = bitrateKbps,
            trackNumber = trackNumber,
            discNumber = discNumber.coerceAtLeast(1),
            year = c.intOrZero(MediaStore.Audio.Media.YEAR).takeIf { it > 0 },
            folderPath = folderOf(c),
            fileName = fileName,
            mimeType = c.stringOrNull(MediaStore.Audio.Media.MIME_TYPE),
            sizeBytes = c.longOrZero(MediaStore.Audio.Media.SIZE),
            dateAddedMs = c.longOrZero(MediaStore.Audio.Media.DATE_ADDED) * MS_PER_SECOND,
            dateModifiedMs = c.longOrZero(MediaStore.Audio.Media.DATE_MODIFIED) * MS_PER_SECOND,
        )
    }

    private fun readVideo(c: Cursor): ScannedVideo {
        val id = c.long(MediaStore.Video.Media._ID)
        val data = c.stringOrNull(MediaStore.Video.Media.DATA)
        return ScannedVideo(
            id = id,
            uri = ContentUris.withAppendedId(VIDEO_URI, id).toString(),
            title = c.stringOrNull(MediaStore.Video.Media.TITLE),
            durationMs = c.longOrZero(MediaStore.Video.Media.DURATION),
            width = c.intOrZero(MediaStore.Video.Media.WIDTH),
            height = c.intOrZero(MediaStore.Video.Media.HEIGHT),
            folderPath = folderOf(c),
            fileName = c.stringOrNull(MediaStore.Video.Media.DISPLAY_NAME) ?: data?.substringAfterLast('/').orEmpty(),
            mimeType = c.stringOrNull(MediaStore.Video.Media.MIME_TYPE),
            sizeBytes = c.longOrZero(MediaStore.Video.Media.SIZE),
            dateAddedMs = c.longOrZero(MediaStore.Video.Media.DATE_ADDED) * MS_PER_SECOND,
            dateModifiedMs = c.longOrZero(MediaStore.Video.Media.DATE_MODIFIED) * MS_PER_SECOND,
        )
    }

    private fun fingerprint(c: Cursor) = Fingerprint(
        modifiedMs = c.longOrZero(MediaStore.MediaColumns.DATE_MODIFIED) * MS_PER_SECOND,
        sizeBytes = c.longOrZero(MediaStore.MediaColumns.SIZE),
        folderPath = folderOf(c),
    )

    private fun folderOf(c: Cursor): String {
        val relative = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            c.stringOrNull(MediaStore.MediaColumns.RELATIVE_PATH)
        } else {
            null
        }
        return resolveFolder(c.stringOrNull(MediaStore.MediaColumns.DATA), relative)
    }

    private fun audioProjection(): Array<String> = buildList {
        add(MediaStore.Audio.Media._ID)
        add(MediaStore.Audio.Media.TITLE)
        add(MediaStore.Audio.Media.ARTIST)
        add(MediaStore.Audio.Media.ALBUM)
        add(MediaStore.Audio.Media.DURATION)
        add(MediaStore.Audio.Media.TRACK)
        add(MediaStore.Audio.Media.YEAR)
        add(MediaStore.Audio.Media.DATE_ADDED)
        add(MediaStore.Audio.Media.DATE_MODIFIED)
        add(MediaStore.Audio.Media.SIZE)
        add(MediaStore.Audio.Media.DISPLAY_NAME)
        add(MediaStore.Audio.Media.MIME_TYPE)
        add(MediaStore.Audio.Media.DATA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Audio.Media.RELATIVE_PATH)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(COL_ALBUM_ARTIST)
            add(COL_GENRE)
            add(COL_BITRATE)
            add(COL_CD_TRACK_NUMBER)
            add(COL_DISC_NUMBER)
        }
    }.toTypedArray()

    private fun videoProjection(): Array<String> = buildList {
        add(MediaStore.Video.Media._ID)
        add(MediaStore.Video.Media.TITLE)
        add(MediaStore.Video.Media.DISPLAY_NAME)
        add(MediaStore.Video.Media.DURATION)
        add(MediaStore.Video.Media.WIDTH)
        add(MediaStore.Video.Media.HEIGHT)
        add(MediaStore.Video.Media.SIZE)
        add(MediaStore.Video.Media.DATE_ADDED)
        add(MediaStore.Video.Media.DATE_MODIFIED)
        add(MediaStore.Video.Media.MIME_TYPE)
        add(MediaStore.Video.Media.DATA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Video.Media.RELATIVE_PATH)
    }.toTypedArray()

    /** Android 10 and older: genres live in a separate table that links back to audio ids. Failures are non-fatal. */
    @Suppress("TooGenericExceptionCaught")
    private fun loadLegacyGenres(): Map<Long, String> {
        val out = HashMap<Long, String>()
        try {
            val projection = arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME)
            query(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, projection, null, null) { genre ->
                val genreId = genre.long(MediaStore.Audio.Genres._ID)
                val name = genre.stringOrNull(MediaStore.Audio.Genres.NAME).cleanTag()
                if (name != null) {
                    val members = MediaStore.Audio.Genres.Members.getContentUri(VOLUME_EXTERNAL, genreId)
                    query(members, arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID), null, null) { member ->
                        out.putIfAbsent(member.long(MediaStore.Audio.Genres.Members.AUDIO_ID), name)
                    }
                }
            }
        } catch (ignored: RuntimeException) {
            // Some vendor builds break the genres provider. Genre is optional metadata, so carry on without it.
        }
        return out
    }

    private inline fun query(
        uri: Uri,
        projection: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?,
        onRow: (Cursor) -> Unit,
    ) {
        resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            while (cursor.moveToNext()) onRow(cursor)
        }
    }

    private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))

    private fun Cursor.longOrZero(column: String): Long {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) 0L else getLong(index)
    }

    private fun Cursor.intOrZero(column: String): Int {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) 0 else getInt(index)
    }

    private fun Cursor.stringOrNull(column: String): String? {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) null else getString(index)
    }

    private companion object {
        val AUDIO_URI: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val VIDEO_URI: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        const val VOLUME_EXTERNAL = "external"

        /** Skips sound effects and clips. Music only, and at least this long. */
        const val MIN_AUDIO_DURATION_MS = 5_000L
        val AUDIO_SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"

        /** Ids are inlined as numbers, so this only bounds statement size. */
        const val QUERY_CHUNK = 500
        const val MS_PER_SECOND = 1000L
        const val BITS_PER_KILOBIT = 1000

        // Column names added in Android 11 (API 30). Literals keep this file compiling against any compileSdk.
        const val COL_ALBUM_ARTIST = "album_artist"
        const val COL_GENRE = "genre"
        const val COL_BITRATE = "bitrate"
        const val COL_CD_TRACK_NUMBER = "cd_track_number"
        const val COL_DISC_NUMBER = "disc_number"
    }
}
