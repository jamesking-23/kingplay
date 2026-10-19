package com.kingplay.app

import android.Manifest
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.kingplay.core.database.KingPlayDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Not a pass/fail test: it walks the app in both designs and both light/dark, saving screenshots and a log of what
 * worked to the app's external files dir, so the visuals can be checked from CI without a phone.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ScreenshotTourTest {
    @get:Rule
    val perms: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_VIDEO,
    )

    @get:Rule
    val compose = createEmptyComposeRule()

    private val inst = InstrumentationRegistry.getInstrumentation()
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val out = File(ctx.getExternalFilesDir(null), "shots").apply { mkdirs() }
    private val log = StringBuilder()

    private fun note(s: String) { log.append(s).append('\n') }

    private fun shot(name: String) {
        Thread.sleep(1200)
        val full = inst.uiAutomation.takeScreenshot() ?: return note("no screenshot $name")
        val bmp = Bitmap.createScaledBitmap(full, full.width / 2, full.height / 2, true)
        val bytes = java.io.ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 72, it) }.toByteArray()
        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        val chunks = b64.chunked(3500)
        // The app is uninstalled when the run ends, so the pictures travel through logcat and CI reassembles them.
        chunks.forEachIndexed { i, c -> android.util.Log.i("KPSHOT", "$name ${i + 1} ${chunks.size} $c") }
        note("shot $name ${bytes.size}B")
    }

    private fun step(name: String, block: () -> Unit) {
        try { block() } catch (t: Throwable) { note("FAILED $name: ${t.javaClass.simpleName} ${t.message?.take(200)}") }
    }

    private fun night(on: Boolean) {
        inst.uiAutomation.executeShellCommand("cmd uimode night ${if (on) "yes" else "no"}").close()
        Thread.sleep(1500)
    }

    @Test
    fun tour() {
        try { run() } finally { android.util.Log.i("KPNOTE", log.toString().replace('\n', '|')) }
    }

    private fun run() {
        val songs = listOf("smoke_mp3.mp3" to "audio/mpeg", "smoke_m4a.m4a" to "audio/mp4")
        for ((name, mime) in songs) {
            val v = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, name)
                put(MediaStore.Audio.Media.MIME_TYPE, mime)
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/KingPlaySmoke")
            }
            val uri = ctx.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, v)!!
            ctx.contentResolver.openOutputStream(uri)!!.use { o -> inst.context.assets.open(name).use { it.copyTo(o) } }
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val db = Room.databaseBuilder(ctx, KingPlayDatabase::class.java, KingPlayDatabase.NAME).build()
        var id: Long? = null
        val deadline = System.currentTimeMillis() + 90_000
        while (id == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(1500)
            id = db.openHelper.readableDatabase.query("SELECT id FROM tracks WHERE fileName = 'smoke_mp3.mp3'").use { c ->
                if (c.moveToFirst()) c.getLong(0) else null
            }
        }
        db.close()
        note("track id=$id")
        if (id != null) step("play") {
            val token = SessionToken(ctx, ComponentName(ctx, "com.kingplay.core.player.service.PlaybackService"))
            val c = MediaController.Builder(ctx, token).buildAsync().get(30, TimeUnit.SECONDS)
            inst.runOnMainSync {
                c.setMediaItem(MediaItem.Builder().setMediaId("a:$id").build())
                c.prepare(); c.play()
            }
            Thread.sleep(3000)
        }

        for (dark in listOf(false, true)) {
            night(dark)
            val mode = if (dark) "dark" else "light"
            step("lib-$mode") { shot("1_library_king_$mode") }
            step("np-$mode") {
                compose.onAllNodes(hasContentDescription("Open now playing", substring = true, ignoreCase = true)).onFirst().performClick()
                shot("2_nowplaying_king_$mode")
                inst.uiAutomation.executeShellCommand("input keyevent KEYCODE_BACK").close()
                Thread.sleep(1000)
            }
            step("settings-$mode") {
                compose.onAllNodes(hasContentDescription("Settings", substring = true, ignoreCase = true)).onFirst().performClick()
                shot("3_settings_king_$mode")
            }
            if (!dark) {
                step("switch-kk") {
                    compose.onNodeWithText("kk-ui").performScrollTo().performClick()
                    shot("4_settings_kk_light")
                }
                step("kk-library") {
                    inst.uiAutomation.executeShellCommand("input keyevent KEYCODE_BACK").close()
                    shot("5_library_kk_light")
                }
                step("kk-np") {
                    compose.onAllNodes(hasContentDescription("Open now playing", substring = true, ignoreCase = true)).onFirst().performClick()
                    shot("6_nowplaying_kk_light")
                    inst.uiAutomation.executeShellCommand("input keyevent KEYCODE_BACK").close()
                }
                step("back-to-king") {
                    compose.onAllNodes(hasContentDescription("Settings", substring = true, ignoreCase = true)).onFirst().performClick()
                    compose.onNodeWithText("King Art").performScrollTo().performClick()
                    inst.uiAutomation.executeShellCommand("input keyevent KEYCODE_BACK").close()
                }
            } else {
                step("dark-kk") {
                    compose.onNodeWithText("kk-ui").performScrollTo().performClick()
                    inst.uiAutomation.executeShellCommand("input keyevent KEYCODE_BACK").close()
                    shot("7_library_kk_dark")
                }
            }
        }
        night(false)
        scenario.close()
    }
}
