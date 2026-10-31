package com.kingplay.core.model

import com.kingplay.core.model.update.UpdateCheck
import com.kingplay.core.model.update.UpdatePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckTest {
    @Test
    fun newerPreviewWins() {
        assertTrue(UpdateCheck.isNewer("v1.0.0-preview9", "v1.0.0-preview8"))
        assertTrue(UpdateCheck.isNewer("v1.0.0-preview10", "v1.0.0-preview9"))
        assertFalse(UpdateCheck.isNewer("v1.0.0-preview8", "v1.0.0-preview8"))
        assertFalse(UpdateCheck.isNewer("v1.0.0-preview7", "v1.0.0-preview8"))
    }

    @Test
    fun finalReleaseBeatsPreviewsAndNewVersionBeatsFinal() {
        assertTrue(UpdateCheck.isNewer("v1.0.0", "v1.0.0-preview12"))
        assertTrue(UpdateCheck.isNewer("v1.1.0-preview1", "v1.0.0"))
    }

    @Test
    fun aNonReleaseBuildCanAlwaysUpdate() {
        assertTrue(UpdateCheck.isNewer("v1.0.0-preview1", "dev"))
    }

    @Test
    fun picksNewestReleaseWithAnApkAndIgnoresDrafts() {
        val json = """[
          {"tag_name":"v1.0.0-preview7","assets":[{"name":"KingPlay-debug.apk","size":10,"browser_download_url":"https://x/7.apk"}]},
          {"tag_name":"v1.0.0-preview9","draft":true,"assets":[{"name":"KingPlay-debug.apk","size":10,"browser_download_url":"https://x/9.apk"}]},
          {"tag_name":"v1.0.0-preview8","body":"notes","assets":[
            {"name":"KingPlay-1.0.0.exe","size":5,"browser_download_url":"https://x/8.exe"},
            {"name":"KingPlay-debug.apk","size":20,"browser_download_url":"https://x/8.apk"}]}
        ]"""
        val android = UpdateCheck.newest(json, UpdatePlatform.ANDROID)
        assertNotNull(android)
        assertEquals("v1.0.0-preview8", android!!.tag)
        assertEquals("https://x/8.apk", android.downloadUrl)
        assertEquals(20L, android.sizeBytes)
        assertEquals("https://x/8.exe", UpdateCheck.newest(json, UpdatePlatform.WINDOWS)?.downloadUrl)
    }

    @Test
    fun rejectsPlainHttpAndBadJson() {
        val http = """[{"tag_name":"v1.0.0-preview8","assets":[{"name":"a.apk","browser_download_url":"http://x/a.apk"}]}]"""
        assertNull(UpdateCheck.newest(http, UpdatePlatform.ANDROID))
        assertNull(UpdateCheck.newest("not json", UpdatePlatform.ANDROID))
    }

    @Test
    fun displayedNotesNeverContainLinks() {
        val raw = "## What's Changed\n* Faster scan by @x in https://github.com/a/b/pull/3\nBetter search\n\n\n**Full Changelog**: https://github.com/a/b/compare/v1...v2"
        val shown = UpdateCheck.plainNotes(raw)
        assertEquals("Better search", shown)
    }

    @Test
    fun whatsNewShowsOnlyAfterAnUpdate() {
        val w = com.kingplay.core.model.update.WhatsNew
        assertFalse(w.shouldShow("", "v1.0.0-preview16"))
        assertFalse(w.shouldShow("v1.0.0-preview16", "v1.0.0-preview16"))
        assertFalse(w.shouldShow("v1.0.0-preview15", "dev"))
        assertTrue(w.shouldShow("v1.0.0-preview15", "v1.0.0-preview16"))
    }
}
