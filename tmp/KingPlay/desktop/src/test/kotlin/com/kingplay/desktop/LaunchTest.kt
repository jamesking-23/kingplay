package com.kingplay.desktop

import com.kingplay.desktop.launch.SingleInstance
import com.kingplay.desktop.launch.parseLaunchFiles
import com.kingplay.desktop.video.videoFileFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private fun freePort(): Int = ServerSocket(0).use { it.localPort }

class LaunchFilesTest {
    private val dir = Files.createTempDirectory("kp-launch").toFile()

    private fun file(name: String) = File(dir, name).also { it.writeText("x") }

    @Test fun keepsOnlyRealAudioAndVideoFilesOnceInOrder() {
        val song = file("a.mp3")
        val clip = file("b.MKV")
        file("c.txt")
        File(dir, "folder.mp3").mkdirs()
        val request = parseLaunchFiles(
            listOf(song.path, "--flag", File(dir, "missing.flac").path, clip.path, song.path, "\"${song.path}\"", ""),
        )
        assertEquals(listOf(song.absolutePath), request.audio)
        assertEquals(listOf(clip.absolutePath), request.videos)
        assertEquals(listOf(song.absolutePath, clip.absolutePath), request.all)
        assertFalse(request.isEmpty)
    }

    @Test fun nothingUsefulMeansEmpty() {
        assertTrue(parseLaunchFiles(emptyList()).isEmpty)
        assertTrue(parseLaunchFiles(listOf("/etc/passwd", dir.path, "x")).isEmpty)
    }

    @Test fun singleVideoFileHasAStableId() {
        val clip = file("My.Holiday.mp4")
        val video = videoFileFor(clip)!!
        assertEquals("My.Holiday", video.title)
        assertEquals(video.id, videoFileFor(clip)!!.id)
        assertNull(videoFileFor(file("song.mp3")))
        assertNull(videoFileFor(File(dir, "gone.mp4")))
    }
}

class SingleInstanceTest {
    @Test fun secondCopyHandsItsFilesToTheFirstAndQuits() {
        val port = freePort()
        val received = CopyOnWriteArrayList<List<String>>()
        val got = CountDownLatch(2)
        SingleInstance(port).use { first ->
            assertTrue(first.acquireOrForward(listOf("ignored")) { received += it; got.countDown() })
            val second = SingleInstance(port)
            assertFalse(second.acquireOrForward(listOf("/music/a.mp3", "/music/b.flac")) { error("must not run") })
            assertFalse(SingleInstance(port).acquireOrForward(emptyList()) { error("must not run") })
            assertTrue(got.await(5, TimeUnit.SECONDS))
        }
        assertEquals(listOf(listOf("/music/a.mp3", "/music/b.flac"), emptyList()), received.toList())
    }

    @Test fun strangersWithoutTheGreetingAreIgnoredAndTheServerKeepsRunning() {
        val port = freePort()
        val received = CopyOnWriteArrayList<List<String>>()
        val got = CountDownLatch(1)
        SingleInstance(port).use { first ->
            first.acquireOrForward(emptyList()) { received += it; got.countDown() }
            Socket(InetAddress.getLoopbackAddress(), port).use { stranger ->
                stranger.getOutputStream().write("GET / HTTP/1.1\n/etc/passwd\n".toByteArray())
            }
            assertFalse(SingleInstance(port).acquireOrForward(listOf("/music/ok.mp3")) { error("must not run") })
            assertTrue(got.await(5, TimeUnit.SECONDS))
        }
        assertEquals(listOf(listOf("/music/ok.mp3")), received.toList())
    }

    @Test fun anUnreachableFirstCopyMeansCarryOn() {
        val port = freePort() // nothing listens here; binding works, so this copy becomes the first
        SingleInstance(port).use { single ->
            assertTrue(single.acquireOrForward(emptyList()) { })
        }
    }
}
