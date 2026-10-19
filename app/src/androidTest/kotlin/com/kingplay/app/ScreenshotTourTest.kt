package com.kingplay.app

import android.Manifest
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import android.view.accessibility.AccessibilityNodeInfo
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
@RunWith(AndroidJUnit4::class)
class ScreenshotTourTest {
    @get:Rule
    val perms: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_VIDEO,
    )

    private val inst = InstrumentationRegistry.getInstrumentation()
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val out = File(ctx.getExternalFilesDir(null), "shots").apply { mkdirs() }
    private val log = StringBuilder()

    private fun note(s: String) { log.append(s).append('\n') }

    private fun shot(name: String) {
        Thread.sleep(1200)
        val full = inst.uiAutomation.takeScreenshot() ?: return note("no screenshot $name")
        val bmp = Bitmap.createScaledBitmap(full, full.width * 3 / 5, full.height * 3 / 5, true)
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

    /** Finds a node whose text or description contains [needle] and clicks it (or its clickable parent). */
    private fun click(needle: String) {
        fun find(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (n == null) return null
            val label = "${n.text ?: ""} ${n.contentDescription ?: ""}"
            if (label.contains(needle, ignoreCase = true)) return n
            for (i in 0 until n.childCount) find(n.getChild(i))?.let { return it }
            return null
        }
        var node = find(inst.uiAutomation.rootInActiveWindow) ?: error("nothing matches '$needle'")
        while (!node.isClickable) node = node.parent ?: error("'$needle' is not clickable")
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Thread.sleep(900)
    }

    private fun back() {
        inst.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        Thread.sleep(900)
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
        var scenario = ActivityScenario.launch(MainActivity::class.java)
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

        fun tour(prefix: String, mode: String) {
            step("$prefix-lib") { shot("1_${prefix}_library_$mode") }
            step("$prefix-np") {
                click("Open now playing")
                shot("2_${prefix}_nowplaying_$mode")
                back()
            }
            step("$prefix-settings") {
                click("Settings")
                shot("3_${prefix}_settings_$mode")
            }
        }

        night(false)
        tour("king", "light")
        step("kk") { click("kk-ui"); back(); }
        step("kk-light") { tour("kk", "light") }
        night(true)
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(2500)
        step("kk-dark") { tour("kk", "dark") }
        step("king") { click("King Art"); back() }
        step("king-dark") { tour("king", "dark") }
        night(false)
        scenario.close()
    }
}
