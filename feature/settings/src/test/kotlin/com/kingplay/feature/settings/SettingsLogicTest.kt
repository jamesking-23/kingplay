package com.kingplay.feature.settings

import com.kingplay.feature.settings.logic.backupFileName
import com.kingplay.feature.settings.logic.folderLabel
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsLogicTest {
    @Test
    fun backupNameUsesTheLocalDate() {
        // 2026-10-15 23:30 in Kampala (UTC+3) is already 20:30 UTC the same day; 22:00 UTC is the 16th in Kampala.
        val kampala = ZoneId.of("Africa/Kampala")
        val utcLate = java.time.Instant.parse("2026-10-15T22:00:00Z").toEpochMilli()
        assertEquals("kingplay-backup-2026-10-16.json", backupFileName(utcLate, kampala))
        assertEquals("kingplay-backup-2026-10-15.json", backupFileName(utcLate, ZoneId.of("UTC")))
    }

    @Test
    fun folderLabelsShowTheLastSegment() {
        assertEquals("Recordings", folderLabel("/storage/emulated/0/Recordings/"))
        assertEquals("Music", folderLabel("/Music"))
        assertEquals("/", folderLabel("/"))
    }
}
