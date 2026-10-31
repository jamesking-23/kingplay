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

    private fun root() = inst.uiAutomation.rootInActiveWindow

    private fun find(n: AccessibilityNodeInfo?, needle: String): AccessibilityNodeInfo? {
        if (n == null) return null
        val label = "${n.text ?: ""} ${n.contentDescription ?: ""}"
        if (label.contains(needle, ignoreCase = true)) return n
        for (i in 0 until n.childCount) find(n.getChild(i), needle)?.let { return it }
        return null
    }

    /** The tallest scrollable node: the main vertical list, not a horizontal chip row. */
    private fun findScrollable(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var best: AccessibilityNodeInfo? = null
        var bestHeight = -1
        fun walk(x: AccessibilityNodeInfo?) {
            if (x == null) return
            if (x.isScrollable) {
                val r = android.graphics.Rect()
                x.getBoundsInScreen(r)
                if (r.height() > bestHeight) { best = x; bestHeight = r.height() }
            }
            for (i in 0 until x.childCount) walk(x.getChild(i))
        }
        walk(n)
        return best
    }

    /** Clicks the node matching [needle], scrolling the screen down to find it when it is off-screen. */
    private fun click(needle: String) {
        var node = find(root(), needle)
        if (node == null) {
            // Back to the top, then down in steps, so a card between two steps is never skipped.
            repeat(8) {
                findScrollable(root())?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                Thread.sleep(250)
            }
            node = find(root(), needle)
        }
        var tries = 0
        while (node == null && tries++ < 30) {
            findScrollable(root())?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            Thread.sleep(500)
            node = find(root(), needle)
        }
        val found = node ?: error("nothing matches '$needle'")
        var n: AccessibilityNodeInfo? = found
        while (n != null && !n.isClickable) n = n.parent
        if (n != null) {
            n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            // Not reported as clickable: tap the middle of where it is drawn.
            val r = android.graphics.Rect()
            found.getBoundsInScreen(r)
            inst.uiAutomation.executeShellCommand("input tap ${r.centerX()} ${r.centerY()}").close()
        }
        Thread.sleep(900)
    }

    private fun typeInto(text: String) {
        fun edit(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (n == null) return null
            if (n.className?.toString()?.contains("EditText") == true) return n
            for (i in 0 until n.childCount) edit(n.getChild(i))?.let { return it }
            return null
        }
        val field = edit(root()) ?: error("no text field")
        val args = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
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
        // First run: the welcome asks for a nickname before anything else.
        step("welcome") {
            Thread.sleep(2500)
            shot("00_welcome")
            typeInto("James")
            shot("01_welcome_filled")
            click("Let's go")
            Thread.sleep(1500)
        }
        val db = Room.databaseBuilder(ctx, KingPlayDatabase::class.java, KingPlayDatabase.NAME).build()
        var id: Long? = null
        val deadline = System.currentTimeMillis() + 90_000
        while (id == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(1500)
            id = db.openHelper.readableDatabase.query("SELECT id FROM tracks WHERE fileName = 'smoke_mp3.mp3'").use { c ->
                if (c.moveToFirst()) c.getLong(0) else null
            }
        }
        note("track id=$id")
        if (id != null) {
            // Seed listening history so Most played and the monthly report have something to show.
            step("seed") {
                val w = db.openHelper.writableDatabase
                w.execSQL("INSERT OR IGNORE INTO track_user_data(trackId, playCount, lastPlayedMs, isFavorite) VALUES($id, 0, NULL, 0)")
                w.execSQL("UPDATE track_user_data SET playCount = 7, lastPlayedMs = ${System.currentTimeMillis()} WHERE trackId = $id")
                val now = System.currentTimeMillis()
                val lastMonth = now - 33L * 24 * 3600 * 1000
                repeat(7) { i -> w.execSQL("INSERT INTO play_events(trackId, playedAtMs) VALUES($id, ${now - i * 3_600_000L})") }
                repeat(5) { i -> w.execSQL("INSERT INTO play_events(trackId, playedAtMs) VALUES($id, ${lastMonth - i * 3_600_000L})") }
            }
        }
        db.close()
        if (id != null) step("play") {
            val token = SessionToken(ctx, ComponentName(ctx, "com.kingplay.core.player.service.PlaybackService"))
            val c = MediaController.Builder(ctx, token).buildAsync().get(30, TimeUnit.SECONDS)
            inst.runOnMainSync {
                c.setMediaItem(MediaItem.Builder().setMediaId("a:$id").build())
                c.prepare(); c.play()
            }
            Thread.sleep(3000)
        }
        night(false)

        step("features") {
            shot("10_library_greeting")
            fun open(route: String) {
                scenario.close()
                val intent = android.content.Intent(ctx, MainActivity::class.java)
                    .putExtra("kp_route", route).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                scenario = ActivityScenario.launch(intent)
                Thread.sleep(3000)
            }
            step("profile") {
                open("profile")
                shot("11_profile")
            }
            step("settings") {
                open("settings")
                shot("12_settings_0")
                for (page in 1..6) {
                    findScrollable(root())?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    Thread.sleep(700)
                    shot("12_settings_$page")
                }
            }
            step("lyrics-browser") {
                open("lyrics_browser")
                Thread.sleep(6000)
                shot("17_lyrics_browser")
                click("Add copied lyrics")
                shot("18_lyrics_nothing_copied")
            }
        }
        step("designs") {
            fun openUi(ui: String, route: String? = null) {
                scenario.close()
                val intent = android.content.Intent(ctx, MainActivity::class.java)
                    .putExtra("kp_ui", ui).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                route?.let { intent.putExtra("kp_route", it) }
                scenario = ActivityScenario.launch(intent)
                Thread.sleep(3500)
            }
            for (ui in listOf("KING_ART", "KK_UI", "KING_NEON", "KING_GLASS", "KING_CLASSIC")) {
                step("design $ui") {
                    openUi(ui)
                    shot("20_${ui}_library")
                }
            }
            for (ui in listOf("KING_ART", "KK_UI", "KING_NEON", "KING_GLASS", "KING_CLASSIC")) {
                step("np $ui") {
                    openUi(ui, "nowplaying")
                    shot("40_${ui}_nowplaying")
                }
            }
            for (ui in listOf("KING_GLASS", "KING_CLASSIC")) {
                step("settings $ui") {
                    openUi(ui, "settings")
                    shot("41_${ui}_settings")
                }
            }
            step("long press") {
                openUi("KING_NEON")
                val r = android.graphics.Rect()
                (find(root(), "Smoke FLAC") ?: error("no song row")).getBoundsInScreen(r)
                inst.uiAutomation.executeShellCommand("input swipe ${r.centerX()} ${r.centerY()} ${r.centerX()} ${r.centerY()} 1000").close()
                Thread.sleep(800)
                shot("21_long_press_menu")
                click("Add to playlist")
                shot("22_add_to_playlist")
            }
            // Large text: the playlist tab and a detail screen must still lay out.
            inst.uiAutomation.executeShellCommand("settings put system font_scale 1.6").close()
            Thread.sleep(1500)
            step("large font") {
                openUi("KING_NEON")
                shot("30_large_library")
                repeat(1) {
                    inst.uiAutomation.executeShellCommand("input swipe 900 1300 120 1300 150").close()
                    Thread.sleep(500)
                }
                shot("31_large_playlists")
                click("Most played")
                shot("32_large_most_played")
            }
            inst.uiAutomation.executeShellCommand("settings put system font_scale 1.0").close()
        }
        scenario.close()
    }
}
