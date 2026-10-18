package com.kingplay.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.kingplay.core.database.KingPlayDatabase
import com.kingplay.core.database.entity.TrackEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.sin

/** Reproduces "tap a song -> nothing plays / app closes" on a real emulator. */
@RunWith(AndroidJUnit4::class)
class SongPlaybackSmokeTest {
    @get:Rule
    val perms: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_VIDEO,
    )

    private fun wav(file: File) {
        val rate = 22050
        val n = rate * 3
        val data = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until n) data.putShort((sin(2 * PI * 440 * i / rate) * 8000).toInt().toShort())
        val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        h.put("RIFF".toByteArray()).putInt(36 + n * 2).put("WAVE".toByteArray()).put("fmt ".toByteArray())
        h.putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
        h.put("data".toByteArray()).putInt(n * 2)
        file.writeBytes(ByteArrayOutputStream().apply { write(h.array()); write(data.array()) }.toByteArray())
    }

    @Test
    fun songPlaysThroughService() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val file = File(ctx.cacheDir, "smoke.wav").also(::wav)
        val id = 424242L
        val db = Room.databaseBuilder(ctx, KingPlayDatabase::class.java, KingPlayDatabase.NAME).build()
        runBlocking {
            db.trackDao().upsertAll(
                listOf(
                    TrackEntity(
                        id = id, uri = file.toURI().toString(), title = "Smoke", titleSort = "smoke",
                        artistId = 1, artistName = "Tester", albumId = 1, albumTitle = "Album",
                        albumArtistId = null, albumArtist = null, genre = null, durationMs = 3000,
                        bitrateKbps = 352, sampleRateHz = 22050, format = "wav", trackNumber = 1,
                        discNumber = 1, year = null, folderPath = file.parent!!, fileName = file.name,
                        sizeBytes = file.length(), dateAddedMs = 1, dateModifiedMs = 1, needsDetails = false,
                        rgTrackGainDb = null, rgAlbumGainDb = null, rgTrackPeak = null, rgAlbumPeak = null,
                    ),
                ),
            )
        }
        db.close()

        val token = SessionToken(ctx, ComponentName(ctx, "com.kingplay.core.player.service.PlaybackService"))
        var controller: MediaController? = null
        var error: PlaybackException? = null
        val playing = CountDownLatch(1)
        val inst = InstrumentationRegistry.getInstrumentation()
        val future = MediaController.Builder(ctx, token).buildAsync()
        val c = future.get(30, TimeUnit.SECONDS)
        controller = c
        inst.runOnMainSync {
            c.addListener(object : Player.Listener {
                override fun onPlayerError(e: PlaybackException) { error = e; playing.countDown() }
                override fun onIsPlayingChanged(isPlaying: Boolean) { if (isPlaying) playing.countDown() }
            })
            c.setMediaItem(MediaItem.Builder().setMediaId("a:$id").build())
            c.prepare()
            c.play()
        }
        val ok = playing.await(30, TimeUnit.SECONDS)
        val err = error
        inst.runOnMainSync { controller.release() }
        assertNull("player error: ${err?.errorCodeName} ${err?.message} cause=${err?.cause}", err)
        assertTrue("never started playing", ok)
    }
}
