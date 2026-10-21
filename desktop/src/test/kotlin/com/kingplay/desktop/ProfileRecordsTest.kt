package com.kingplay.desktop

import com.kingplay.core.model.settings.AppFont
import com.kingplay.desktop.settings.DesktopSettings
import com.kingplay.desktop.settings.SettingsStore
import com.kingplay.desktop.userdata.PlayStat
import com.kingplay.desktop.userdata.ProfilePhoto
import com.kingplay.desktop.userdata.UserData
import com.kingplay.desktop.userdata.UserDataStore
import com.kingplay.desktop.userdata.formatListening
import com.kingplay.desktop.userdata.greeting
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileRecordsTest {
    private fun temp(): File = File.createTempFile("kp", "").also { it.delete(); it.mkdirs(); it.deleteOnExit() }

    @Test
    fun listeningTimeReadsNaturally() {
        assertEquals("under a minute", formatListening(20_000))
        assertEquals("45 min", formatListening(45 * 60_000L))
        assertEquals("3 h 20 min", formatListening(200 * 60_000L))
        assertEquals("2 d 5 h", formatListening((53 * 60) * 60_000L))
    }

    @Test
    fun greetingFollowsTheHour() {
        assertEquals("Good morning, James", greeting(8, "James"))
        assertEquals("Good afternoon, James", greeting(13, "James"))
        assertEquals("Good evening, James", greeting(21, "James"))
    }

    @Test
    fun recordsAddUpAndSurviveSaving() {
        val data = UserData(stats = mapOf(1L to PlayStat(3, 0), 2L to PlayStat(4, 0)))
            .withListening(audioMs = 60_000, videoMs = 30_000)
            .withListening(audioMs = 1_000, videoMs = -5)
            .withVideoPlay().withVideoPlay()
            .withInstalledAt(1_000).withInstalledAt(9_999)
        assertEquals(7L, data.songPlays)
        assertEquals(61_000L, data.audioListenMs)
        assertEquals(30_000L, data.videoListenMs)
        assertEquals(2L, data.videoPlays)
        assertEquals(1_000L, data.installedAtMs)
        val back = UserDataStore.decode(UserDataStore.encode(data))
        assertEquals(data.videoPlays, back.videoPlays)
        assertEquals(data.audioListenMs, back.audioListenMs)
        assertEquals(data.installedAtMs, back.installedAtMs)
    }

    @Test
    fun profileAndLookSettingsSurviveSaving() {
        val file = File(temp(), "settings.json")
        val store = SettingsStore(file)
        store.save(DesktopSettings(nickname = "James", font = AppFont.LORA, textScale = 1.2f, customAccent = 0xFFD32F2F))
        val back = store.load()
        assertEquals("James", back.nickname)
        assertEquals(AppFont.LORA, back.font)
        assertEquals(1.2f, back.textScale, 0.001f)
        assertEquals(0xFFD32F2F, back.customAccent)
        assertNull(back.profilePhoto)
    }

    @Test
    fun largePicturesAreShrunkAndSmallOnesKept() {
        assertEquals(512 to 256, ProfilePhoto.fitted(2048, 1024))
        assertEquals(300 to 200, ProfilePhoto.fitted(300, 200))
    }

    @Test
    fun aPictureIsCopiedAndTheOldCopyRemoved() {
        val dir = temp()
        val source = File(dir, "me.png")
        ImageIO.write(BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB), "png", source)
        val first = ProfilePhoto.save(source, dir, now = 1)
        assertNotNull(first)
        val second = ProfilePhoto.save(source, dir, now = 2)
        assertNotNull(second)
        assertFalse(first!!.exists())
        assertTrue(second!!.exists())
        val saved = ImageIO.read(second)
        assertEquals(512, saved.width)
        assertNull(ProfilePhoto.save(File(dir, "missing.png"), dir))
        ProfilePhoto.remove(dir)
        assertFalse(second.exists())
    }
}
