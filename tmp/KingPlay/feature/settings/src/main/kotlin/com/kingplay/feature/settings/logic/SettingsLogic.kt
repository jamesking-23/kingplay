package com.kingplay.feature.settings.logic

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/** Suggested file name for a new backup, for example `kingplay-backup-2026-10-15.json`. */
fun backupFileName(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    "kingplay-backup-${DateFormat.format(Instant.ofEpochMilli(epochMs).atZone(zone))}.json"

/** Last part of a folder path, for showing in a list. */
fun folderLabel(path: String): String = path.trimEnd('/').substringAfterLast('/').ifEmpty { path }
