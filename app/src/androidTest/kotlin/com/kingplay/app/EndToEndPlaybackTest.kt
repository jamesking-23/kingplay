package com.kingplay.app

import android.Manifest
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.kingplay.core.database.KingPlayDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.sin

/** Launch the real UI, let the real scanner find a MediaStore song, then play it via the service. */
@RunWith(AndroidJUnit4::class)
class EndToEndPlaybackTest {
    @get:Rule
    val perms: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_VIDEO,
    )

    private fun wavBytes(): ByteArray {
        val rate = 22050
        val n = rate * 8
        val b = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + n * 2).put("WAVE".toByteArray()).put("fmt ".toByteArray())
        b.putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
        b.put("data".toByteArray()).putInt(n * 2)
        for (i in 0 until n) b.putShort((sin(2 * PI * 440 * i / rate) * 8000).toInt().toShort())
        return b.array()
    }

    @Test
    fun scannedSongPlays() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "KingPlay Smoke.wav")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/x-wav")
            put(MediaStore.Audio.Media.TITLE, "KingPlay Smoke")
            put(MediaStore.Audio.Media.ARTIST, "Tester")
            put(MediaStore.Audio.Media.ALBUM, "Smoke Album")
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/KingPlaySmoke")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)!!
        ctx.contentResolver.openOutputStream(uri)!!.use { it.write(wavBytes()) }

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val db = Room.databaseBuilder(ctx, KingPlayDatabase::class.java, KingPlayDatabase.NAME).build()
        var id: Long? = null
        val deadline = System.currentTimeMillis() + 60_000
        while (id == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(1000)
            id = runBlocking { db.openHelper.readableDatabase.query("SELECT id FROM tracks WHERE title LIKE '%Smoke%' LIMIT 1").use { c -> if (c.moveToFirst()) c.getLong(0) else null } }
        }
        db.close()
        assertTrue("scanner never added the song to the library", id != null)
        Thread.sleep(3000) // let the UI settle; a crash here shows up in logcat

        val token = SessionToken(ctx, ComponentName(ctx, "com.kingplay.core.player.service.PlaybackService"))
        val c = MediaController.Builder(ctx, token).buildAsync().get(30, TimeUnit.SECONDS)
        var error: PlaybackException? = null
        val playing = CountDownLatch(1)
        val inst = InstrumentationRegistry.getInstrumentation()
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
        inst.runOnMainSync { c.release() }
        scenario.close()
        assertNull("player error: ${err?.errorCodeName} ${err?.message} cause=${err?.cause}", err)
        assertTrue("never started playing", ok)
    }
}
