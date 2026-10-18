package com.kingplay.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingplay.core.database.dao.TrackDao
import com.kingplay.core.database.entity.PlaylistEntity
import com.kingplay.core.database.entity.PlaylistEntryEntity
import com.kingplay.core.database.entity.RefType
import com.kingplay.core.database.entity.TrackEntity
import com.kingplay.core.database.entity.TrackUserDataEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real SQL against a real SQLite on a device or emulator. This is what validates the queries, FTS
 * triggers and joins; the JVM unit tests cannot.
 */
@RunWith(AndroidJUnit4::class)
class KingPlayDatabaseTest {
    private lateinit var db: KingPlayDatabase
    private lateinit var dao: TrackDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KingPlayDatabase::class.java).build()
        dao = db.trackDao()
    }

    @After
    fun tearDown() = db.close()

    private fun track(
        id: Long,
        title: String,
        artist: String = "Fela",
        album: String = "Zombie",
        albumId: Long = 1,
        artistId: Long = 10,
        genre: String? = "Afrobeat",
        folder: String = "/m/Fela",
    ) = TrackEntity(
        id = id, uri = "content://t/$id", title = title, titleSort = title.lowercase(),
        artistId = artistId, artistName = artist, albumId = albumId, albumTitle = album,
        albumArtistId = null, albumArtist = null, genre = genre, durationMs = 200_000,
        bitrateKbps = 320, sampleRateHz = 44_100, format = "MP3", trackNumber = id.toInt(), discNumber = 1,
        year = 1976, folderPath = folder, fileName = "$title.mp3", sizeBytes = 1000, dateAddedMs = id,
        dateModifiedMs = id, needsDetails = false, rgTrackGainDb = null, rgAlbumGainDb = null,
        rgTrackPeak = null, rgAlbumPeak = null,
    )

    @Test
    fun albumsAndArtistsAreDerivedFromTracks() = runBlocking {
        dao.upsertAll(
            listOf(
                track(1, "Zombie"),
                track(2, "Mr Follow Follow"),
                track(3, "Water", artist = "Asa", album = "Bed", albumId = 2, artistId = 11),
            ),
        )
        val albums = dao.observeAlbums().first()
        assertEquals(listOf("Bed", "Zombie"), albums.map { it.title })
        assertEquals(2, albums.first { it.title == "Zombie" }.trackCount)
        assertEquals(2, dao.observeArtists().first().size)
        assertEquals(listOf("Afrobeat"), dao.observeGenres().first().map { it.name })
    }

    @Test
    fun prefixSearchFindsTracksAndTracksFollowUpdatesAndDeletes() = runBlocking {
        dao.upsertAll(listOf(track(1, "Zombie"), track(2, "Water No Get Enemy")))
        assertEquals(listOf(1L), dao.search("\"zomb*\"", 10).first().map { it.track.id })
        assertEquals(listOf(2L), dao.search("\"water*\" \"enemy*\"", 10).first().map { it.track.id })
        assertEquals(listOf("Zombie"), dao.searchAlbums("\"zomb*\"", 10).first().map { it.title })

        dao.upsertAll(listOf(track(1, "Colonial Mentality")))
        assertEquals(0, dao.search("\"zomb*\"", 10).first().size)
        assertEquals(1, dao.search("\"colon*\"", 10).first().size)

        dao.deleteByIds(listOf(1L))
        assertEquals(0, dao.search("\"colon*\"", 10).first().size)
    }

    @Test
    fun userDataSurvivesRescanReplacingTheTrackRow() = runBlocking {
        dao.upsertAll(listOf(track(1, "Zombie")))
        dao.insertUserDataIfAbsent(TrackUserDataEntity(1, 0, null, false))
        dao.updateFavorite(1, true)
        dao.incrementPlayCount(1, 5_000)
        dao.incrementPlayCount(1, 6_000)

        dao.upsertAll(listOf(track(1, "Zombie").copy(dateModifiedMs = 99)))

        val row = dao.observeTrack(1).first()!!
        assertEquals(2, row.playCount)
        assertEquals(6_000L, row.lastPlayedMs)
        assertEquals(true, row.isFavorite)
        assertEquals(listOf(1L), dao.observeMostPlayed(5).first().map { it.track.id })
        assertEquals(listOf(1L), dao.observeFavorites().first().map { it.track.id })
    }

    @Test
    fun playlistJoinKeepsOrderAndDuplicatesAndSkipsMissingFiles() = runBlocking {
        dao.upsertAll(listOf(track(1, "A"), track(2, "B")))
        val playlists = db.playlistDao()
        val id = playlists.insertPlaylist(PlaylistEntity(name = "Mix", createdAtMs = 1, updatedAtMs = 1))
        playlists.insertEntries(
            listOf(2L, 1L, 2L, 99L).mapIndexed { i, ref ->
                PlaylistEntryEntity(playlistId = id, position = i, refType = RefType.AUDIO, refId = ref)
            },
        )
        assertEquals(listOf(2L, 1L, 2L), playlists.observeTracks(id).first().map { it.track.id })
        assertEquals(4, playlists.observePlaylist(id).first()!!.entryCount)

        playlists.delete(id)
        assertEquals(0, playlists.getEntries(id).size)
    }

    @Test
    fun songsPagingQueryAcceptsFiltersAndNullFormat() {
        // Compiles and binds: a null format means "any". Row loading is exercised by the Paging UI tests later.
        dao.songsByTitleAsc(null, 0, 0, Long.MAX_VALUE)
        dao.songsByPlayCountDesc("MP3", 128, 1000, 600_000)
    }
}
