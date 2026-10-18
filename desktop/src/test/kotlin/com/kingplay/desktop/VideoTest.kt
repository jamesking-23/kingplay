package com.kingplay.desktop

import com.kingplay.desktop.userdata.UserData
import com.kingplay.desktop.userdata.UserDataStore
import com.kingplay.desktop.video.VideoProgress
import com.kingplay.desktop.video.VideoScanner
import com.kingplay.desktop.video.formatSize
import com.kingplay.desktop.video.isVideoFile
import com.kingplay.desktop.video.resumePositionMs
import com.kingplay.desktop.video.videoTitle
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoLibraryTest {
    @Test fun recognisesVideoFilesByExtension() {
        assertTrue(isVideoFile("a.MP4"))
        assertTrue(isVideoFile("movie.final.mkv"))
        assertFalse(isVideoFile("song.mp3"))
        assertFalse(isVideoFile("mp4"))
        assertFalse(isVideoFile("noextension"))
    }

    @Test fun titleDropsOnlyTheLastExtension() {
        assertEquals("My.Holiday_2024", videoTitle("My.Holiday_2024.mp4"))
        assertEquals("plain", videoTitle("plain"))
        assertEquals(".hidden", videoTitle(".hidden"))
    }

    @Test fun scanFindsVideosInSubfoldersSortedAndStable() {
        val root = Files.createTempDirectory("kp-vid").toFile()
        File(root, "sub").mkdirs()
        File(root, "b.mp4").writeText("12345")
        File(root, "sub/A.mkv").writeText("1")
        File(root, "song.mp3").writeText("x")
        File(root, "notes.txt").writeText("x")
        val first = VideoScanner.scan(listOf(root.path, root.path, File(root, "missing").path))
        assertEquals(listOf("A", "b"), first.map { it.title })
        assertEquals(5L, first.last().sizeBytes)
        assertEquals(first.map { it.id }, VideoScanner.scan(listOf(root.path)).map { it.id })
        assertEquals(2, first.map { it.id }.toSet().size)
    }

    @Test fun resumeSkipsTheStartAndTheEnd() {
        assertEquals(0L, resumePositionMs(null))
        assertEquals(0L, resumePositionMs(VideoProgress(3_000, 100_000)))
        assertEquals(40_000L, resumePositionMs(VideoProgress(40_000, 100_000)))
        assertEquals(0L, resumePositionMs(VideoProgress(95_000, 100_000)))
        assertEquals(0L, resumePositionMs(VideoProgress(40_000, 0)))
    }

    @Test fun progressIsKeptOnlyWhenWorthResumingAndSurvivesTheFile() {
        val kept = UserData().withVideoProgress(7, 40_000, 100_000)
        assertEquals(VideoProgress(40_000, 100_000), kept.videoProgress[7L])
        assertTrue(kept.withVideoProgress(7, 99_000, 100_000).videoProgress.isEmpty())
        assertTrue(kept.withVideoProgress(7, 1_000, 100_000).videoProgress.isEmpty())
        val file = File(Files.createTempDirectory("kp-vp").toFile(), "u.json")
        UserDataStore(file).save(kept)
        assertEquals(kept.videoProgress, UserDataStore(file).load().videoProgress)
        val good = mapOf("id" to 2, "positionMs" to 9, "durationMs" to 50)
        val damaged = mapOf("videoProgress" to listOf(mapOf("id" to 1), "x", good))
        assertEquals(setOf(2L), UserDataStore.decode(damaged).videoProgress.keys)
    }

    @Test fun sizesAreReadable() {
        assertEquals("0 KB", formatSize(-5))
        assertEquals("37 KB", formatSize(37 * 1024))
        assertEquals("812 MB", formatSize(812L * 1024 * 1024))
        assertEquals("1.4 GB", formatSize((1.4 * 1024 * 1024 * 1024).toLong()))
    }
}
