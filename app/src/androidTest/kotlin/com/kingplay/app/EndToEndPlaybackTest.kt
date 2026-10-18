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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Launch the real UI, let the real scanner find a MediaStore song, then play it via the service. */
@RunWith(AndroidJUnit4::class)
class EndToEndPlaybackTest {
    @get:Rule
    val perms: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_VIDEO,
    )

    private val files = listOf(
        "smoke_mp3.mp3" to "audio/mpeg",
        "smoke_m4a.m4a" to "audio/mp4",
        "smoke_flac.flac" to "audio/flac",
        "smoke_ogg.ogg" to "audio/ogg",
        "smoke_wav.wav" to "audio/x-wav",
    )

    @Test
    fun scannedSongsPlay() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        for ((name, mime) in files) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, name)
                put(MediaStore.Audio.Media.MIME_TYPE, mime)
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/KingPlaySmoke")
            }
            val uri = ctx.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)!!
            ctx.contentResolver.openOutputStream(uri)!!.use { out ->
                inst.context.assets.open(name).use { it.copyTo(out) }
            }
        }

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val db = Room.databaseBuilder(ctx, KingPlayDatabase::class.java, KingPlayDatabase.NAME).build()
        val ids = LinkedHashMap<String, Long>()
        val deadline = System.currentTimeMillis() + 90_000
        while (ids.size < files.size && System.currentTimeMillis() < deadline) {
            Thread.sleep(1500)
            for ((name, _) in files) {
                if (name in ids) continue
                db.openHelper.readableDatabase.query("SELECT id FROM tracks WHERE fileName = ?", arrayOf(name)).use { c ->
                    if (c.moveToFirst()) ids[name] = c.getLong(0)
                }
            }
        }
        db.close()
        val missing = files.map { it.first }.filter { it !in ids }
        Thread.sleep(3000) // let the UI settle; a crash here shows up in logcat

        val token = SessionToken(ctx, ComponentName(ctx, "com.kingplay.core.player.service.PlaybackService"))
        val c = MediaController.Builder(ctx, token).buildAsync().get(30, TimeUnit.SECONDS)
        val report = StringBuilder()
        for ((name, id) in ids) {
            var error: PlaybackException? = null
            val playing = CountDownLatch(1)
            val listener = object : Player.Listener {
                override fun onPlayerError(e: PlaybackException) { error = e; playing.countDown() }
                override fun onIsPlayingChanged(isPlaying: Boolean) { if (isPlaying) playing.countDown() }
            }
            inst.runOnMainSync {
                c.addListener(listener)
                c.setMediaItem(MediaItem.Builder().setMediaId("a:$id").build())
                c.prepare()
                c.play()
            }
            val ok = playing.await(20, TimeUnit.SECONDS)
            val err = error
            if (err != null) report.append("$name: ERROR ${err.errorCodeName} ${err.message} cause=${err.cause}\n")
            else if (!ok) report.append("$name: never started\n")
            Thread.sleep(1500)
            inst.runOnMainSync { c.stop(); c.removeListener(listener) }
        }
        inst.runOnMainSync { c.release() }
        scenario.close()
        assertTrue("not scanned: $missing", missing.isEmpty())
        assertTrue("playback failures:\n$report", report.isEmpty())
    }
}
