package com.kingplay.desktop

import com.kingplay.core.model.RepeatMode
import com.kingplay.core.model.Track
import com.kingplay.core.model.backup.BackupCodec
import com.kingplay.core.model.backup.BackupData
import com.kingplay.core.model.backup.BackupPlaylist
import com.kingplay.core.model.backup.BackupTrack
import com.kingplay.core.model.settings.AppSettings
import com.kingplay.desktop.library.LibraryIndex
import com.kingplay.desktop.userdata.BackupImport
import com.kingplay.desktop.userdata.ImportOutcome
import com.kingplay.desktop.userdata.SavedQueue
import com.kingplay.desktop.userdata.UserData
import com.kingplay.desktop.userdata.UserDataStore
import com.kingplay.desktop.userdata.favoriteTracks
import com.kingplay.desktop.userdata.mostPlayed
import com.kingplay.desktop.userdata.playlistTracks
import com.kingplay.desktop.userdata.recentlyAdded
import com.kingplay.desktop.userdata.recentlyPlayed
import com.kingplay.desktop.userdata.restorableQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

private fun song(id: Long, title: String, path: String, added: Long = 0) = Track(
    id = id, uri = path, title = title, artistId = 1, artistName = "A", albumId = 1, albumTitle = "B",
    albumArtist = null, genre = null, durationMs = 1000, bitrateKbps = 0, sampleRateHz = 0, format = "mp3",
    trackNumber = 0, discNumber = 0, year = null, folderPath = path.substringBeforeLast('\\'),
    dateAddedMs = added, dateModifiedMs = 0,
)

class UserDataTest {
    @Test fun playlistsGetUniqueNamesAndIds() {
        var (d, a) = UserData().createPlaylist("Gym", 1)
        val (d2, b) = d.createPlaylist(" gym ", 2)
        d = d2
        val (d3, c) = d.createPlaylist("   ", 3)
        assertEquals(listOf("Gym", "gym (2)", "New playlist"), d3.playlists.map { it.name })
        assertEquals(setOf(1L, 2L, 3L), setOf(a, b, c))
    }

    @Test fun renameKeepsNamesUniqueAndIgnoresBlank() {
        val d = UserData().createPlaylist("A", 0).first.createPlaylist("B", 0).first
        assertEquals("a (2)", d.renamePlaylist(2, "a").playlist(2)!!.name)
        assertEquals("B", d.renamePlaylist(2, "  ").playlist(2)!!.name)
        assertEquals("A", d.renamePlaylist(1, "A").playlist(1)!!.name)
    }

    @Test fun addRemoveMoveEntries() {
        var d = UserData().createPlaylist("P", 0).first.addToPlaylist(1, listOf(10, 20, 30))
        d = d.moveInPlaylist(1, 0, 2)
        assertEquals(listOf(20L, 30L, 10L), d.playlist(1)!!.trackIds)
        d = d.removeFromPlaylist(1, 1)
        assertEquals(listOf(20L, 10L), d.playlist(1)!!.trackIds)
        assertEquals(d, d.removeFromPlaylist(1, 9).moveInPlaylist(1, 0, 9).addToPlaylist(99, listOf(1)))
        assertTrue(d.deletePlaylist(1).playlists.isEmpty())
    }

    @Test fun favoritesToggleAndPlaysAccumulate() {
        var d = UserData().toggleFavorite(5)
        assertTrue(d.isFavorite(5))
        assertTrue(!d.toggleFavorite(5).isFavorite(5))
        d = d.recordPlay(5, 100).recordPlay(5, 200)
        assertEquals(2, d.stats.getValue(5).count)
        assertEquals(200, d.stats.getValue(5).lastPlayedMs)
    }

    @Test fun storeRoundTripsEverything() {
        val dir = Files.createTempDirectory("kp-ud").toFile()
        val store = UserDataStore(File(dir, "user.json"))
        val data = UserData().createPlaylist("Mix", 5, listOf(1, 2)).first
            .toggleFavorite(7).recordPlay(7, 9)
            .withQueue(SavedQueue(listOf(1, 2, 3), 1, 4_000, shuffle = true, repeat = RepeatMode.ALL))
        store.save(data)
        assertEquals(data, store.load())
    }

    @Test fun damagedOrMissingFilesGiveEmptyData() {
        val dir = Files.createTempDirectory("kp-ud").toFile()
        assertEquals(UserData(), UserDataStore(File(dir, "none.json")).load())
        File(dir, "bad.json").writeText("[1,2")
        assertEquals(UserData(), UserDataStore(File(dir, "bad.json")).load())
        File(dir, "odd.json").writeText(
            """{"playlists": [1, {"id": "x"}, {"id": 4, "name": ""}], "queue": {"tracks": []}}""",
        )
        val odd = UserDataStore(File(dir, "odd.json")).load()
        assertEquals(listOf("New playlist"), odd.playlists.map { it.name })
        assertEquals(null, odd.queue)
        assertEquals(5, odd.nextPlaylistId)
    }

    private val library = LibraryIndex(
        listOf(
            song(1, "One", "C:\\Music\\Rock\\one.mp3", added = 30),
            song(2, "Two", "C:\\Music\\Rock\\two.mp3", added = 20),
            song(3, "Three", "C:\\Music\\Pop\\three.mp3", added = 10),
        ),
    )

    @Test fun smartLists() {
        val d = UserData().toggleFavorite(3).toggleFavorite(1)
            .recordPlay(2, 10).recordPlay(2, 20).recordPlay(1, 30)
        assertEquals(listOf("One", "Three"), favoriteTracks(library, d).map { it.title })
        assertEquals(listOf("Two", "One"), mostPlayed(library, d).map { it.title })
        assertEquals(listOf("One", "Two"), recentlyPlayed(library, d).map { it.title })
        assertEquals(listOf("One", "Two", "Three"), recentlyAdded(library).map { it.title })
        assertEquals(listOf("One"), recentlyAdded(library, limit = 1).map { it.title })
        val list = UserData().createPlaylist("P", 0, listOf(3, 99, 1)).first.playlists.single()
        assertEquals(listOf("Three", "One"), playlistTracks(list, library).map { it.title })
    }

    private fun backup(playlists: List<BackupPlaylist>, favorites: List<BackupTrack>) = BackupCodec.encode(
        BackupData(0, AppSettings(), playlists, favorites, emptyList(), emptyList(), emptyMap()),
    )

    @Test fun importMatchesPhonePathsByFolderAndFileName() {
        val text = backup(
            listOf(
                BackupPlaylist(
                    "Road trip",
                    1_000,
                    listOf(
                        BackupTrack("/storage/emulated/0/Music/Rock/one.mp3", "One", "A"),
                        BackupTrack("/storage/emulated/0/Music/Pop/three.mp3", "Three", "A"),
                        BackupTrack("/storage/emulated/0/Music/gone.mp3", "Gone", "A"),
                    ),
                ),
            ),
            listOf(BackupTrack("/sdcard/Music/Rock/two.mp3", "Two", "A")),
        )
        val outcome = BackupImport.import(text, library, UserData(), now = 5) as ImportOutcome.Done
        assertEquals(listOf(1L, 3L), outcome.data.playlists.single().trackIds)
        assertEquals("Road trip", outcome.data.playlists.single().name)
        assertEquals(1_000, outcome.data.playlists.single().createdAtMs)
        assertEquals(setOf(2L), outcome.data.favorites)
        val s = outcome.summary
        assertEquals(1, s.playlistsAdded)
        assertEquals(2, s.tracksMatched)
        assertEquals(3, s.tracksTotal)
        assertEquals(1, s.favoritesMatched)
        assertEquals(1, s.favoritesTotal)
    }

    @Test fun importSkipsExistingNamesAndKeepsExistingFavorites() {
        val text = backup(listOf(BackupPlaylist("Mine", 0, emptyList())), emptyList())
        val existing = UserData().createPlaylist("MINE", 0).first.toggleFavorite(9)
        val done = BackupImport.import(text, library, existing, now = 1) as ImportOutcome.Done
        assertEquals(1, done.data.playlists.size)
        assertEquals(1, done.summary.playlistsSkipped)
        assertTrue(9L in done.data.favorites)
    }

    @Test fun importOfAmbiguousNamesMatchesNothing() {
        val twins = LibraryIndex(
            listOf(song(1, "x", "C:\\a\\same.mp3"), song(2, "x", "C:\\b\\same.mp3")),
        )
        val text = backup(emptyList(), listOf(BackupTrack("/phone/other/same.mp3", "x", "A")))
        val done = BackupImport.import(text, twins, UserData(), now = 1) as ImportOutcome.Done
        assertTrue(done.data.favorites.isEmpty())
    }

    @Test fun importRejectsOtherFiles() {
        val result = BackupImport.import("{\"hello\": 1}", library, UserData(), now = 1)
        assertNotNull((result as ImportOutcome.Invalid).reason)
        assertTrue(BackupImport.import("nope", library, UserData(), now = 1) is ImportOutcome.Invalid)
    }
}

class QueueRestoreTest {
    private val library = LibraryIndex(
        listOf(
            song(1, "One", "C:\\m\\1.mp3"),
            song(2, "Two", "C:\\m\\2.mp3"),
            song(4, "Four", "C:\\m\\4.mp3"),
        ),
    )

    private fun saved(ids: List<Long>, index: Int) =
        SavedQueue(ids, index, 5_000, shuffle = true, repeat = RepeatMode.ONE)

    @Test fun nothingSavedMeansNothingToRestore() {
        assertEquals(null, restorableQueue(null, library))
        assertEquals(null, restorableQueue(saved(listOf(8, 9), 0), library))
    }

    @Test fun missingSongsAreDroppedAndCurrentFollowsItsSong() {
        val r = restorableQueue(saved(listOf(1, 3, 2, 4), 2), library)!!
        assertEquals(listOf(1L, 2L, 4L), r.trackIds)
        assertEquals(1, r.index)
        assertEquals(5_000, r.positionMs)
        assertTrue(r.shuffle)
        assertEquals(RepeatMode.ONE, r.repeat)
    }

    @Test fun ifTheCurrentSongIsGoneTheNextOneTakesOverFromTheStart() {
        val r = restorableQueue(saved(listOf(1, 3, 2), 1), library)!!
        assertEquals(listOf(1L, 2L), r.trackIds)
        assertEquals(1, r.index)
        assertEquals(0, r.positionMs)
    }

    @Test fun indexPastTheEndIsClamped() {
        val r = restorableQueue(saved(listOf(1, 9), 1), library)!!
        assertEquals(0, r.index)
    }
}
