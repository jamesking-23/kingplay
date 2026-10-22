package com.kingplay.core.model.backup

import com.kingplay.core.model.EqPreset
import com.kingplay.core.model.settings.AppAccent
import com.kingplay.core.model.settings.AppSettings
import com.kingplay.core.model.settings.AppThemeMode
import com.kingplay.core.model.settings.AppFont
import com.kingplay.core.model.settings.AppUiTheme
import com.kingplay.core.model.settings.HapticLevel
import com.kingplay.core.model.settings.HapticSettings

/** A song as the backup remembers it: where it was, and enough to double-check a match. */
data class BackupTrack(val path: String, val title: String, val artist: String)

data class BackupPlaylist(val name: String, val createdAtMs: Long, val tracks: List<BackupTrack>)

/** How often a song was played and when last; written by the PC app. The phone ignores it. */
data class BackupPlayStat(val track: BackupTrack, val count: Int, val lastPlayedMs: Long)

/** Where the PC app stopped a video, by full path. The phone ignores it. */
data class BackupVideoProgress(val path: String, val positionMs: Long, val durationMs: Long)

data class BackupPreset(val name: String, val preampDb: Float, val bandGainsDb: List<Float>)

data class BackupData(
    val createdAtMs: Long,
    val settings: AppSettings,
    val playlists: List<BackupPlaylist>,
    val favorites: List<BackupTrack>,
    val presets: List<BackupPreset>,
    val excludedFolders: List<String>,
    /** Imported lyrics by lyrics key. */
    val lyrics: Map<String, String>,
    /** Which app wrote the file. A desktop backup carries no phone settings, so restoring it leaves them alone. */
    val source: String? = null,
    val playStats: List<BackupPlayStat> = emptyList(),
    val videoProgress: List<BackupVideoProgress> = emptyList(),
)

class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class RestoreSummary(
    val playlistsAdded: Int,
    val playlistsSkipped: Int,
    val tracksMatched: Int,
    val tracksTotal: Int,
    val favoritesMatched: Int,
    val presetsAdded: Int,
    val lyricsAdded: Int,
)

sealed interface RestoreResult {
    data class Done(val summary: RestoreSummary) : RestoreResult

    /** The file is not a KingPlay backup, or is damaged or from a newer version. */
    data class Invalid(val reason: String) : RestoreResult
}

object BackupCodec {
    const val FORMAT = "kingplay-backup"
    const val VERSION = 1
    const val SOURCE_DESKTOP = "desktop"

    fun encode(data: BackupData): String = Json.write(
        linkedMapOf<String, Any?>(
            "format" to FORMAT,
            "version" to VERSION,
            "createdAtMs" to data.createdAtMs,
            "settings" to linkedMapOf(
                "themeMode" to data.settings.themeMode.name,
                "accent" to data.settings.accent.name,
                "dynamicColor" to data.settings.dynamicColor,
                "lowPowerVisuals" to data.settings.lowPowerVisuals,
                "uiTheme" to data.settings.uiTheme.name,
                "font" to data.settings.font.name,
                "textScale" to data.settings.textScale.toDouble(),
                "customAccent" to data.settings.customAccent,
                "haptics" to linkedMapOf(
                    "enabled" to data.settings.haptics.enabled,
                    "play" to data.settings.haptics.play.name,
                    "transport" to data.settings.haptics.transport.name,
                    "toggle" to data.settings.haptics.toggle.name,
                    "tap" to data.settings.haptics.tap.name,
                ),
                "nickname" to data.settings.nickname,
                "coverTheme" to data.settings.coverTheme,
            ),
            "playlists" to data.playlists.map {
                linkedMapOf("name" to it.name, "createdAtMs" to it.createdAtMs, "tracks" to it.tracks.map(::track))
            },
            "favorites" to data.favorites.map(::track),
            "presets" to data.presets.map {
                linkedMapOf("name" to it.name, "preampDb" to it.preampDb, "bandGainsDb" to it.bandGainsDb)
            },
            "excludedFolders" to data.excludedFolders,
            "lyrics" to data.lyrics,
        ).also {
            if (data.source != null) it["source"] = data.source
            if (data.playStats.isNotEmpty()) {
                it["playStats"] = data.playStats.map { stat ->
                    linkedMapOf(
                        "path" to stat.track.path,
                        "title" to stat.track.title,
                        "artist" to stat.track.artist,
                        "count" to stat.count,
                        "lastPlayedMs" to stat.lastPlayedMs,
                    )
                }
            }
            if (data.videoProgress.isNotEmpty()) {
                it["videoProgress"] = data.videoProgress.map { v ->
                    linkedMapOf("path" to v.path, "positionMs" to v.positionMs, "durationMs" to v.durationMs)
                }
            }
        },
    )

    private fun track(t: BackupTrack) = linkedMapOf("path" to t.path, "title" to t.title, "artist" to t.artist)

    /** Throws [BackupFormatException] for anything that is not a version this app can read. */
    fun decode(text: String): BackupData {
        val root = try {
            Json.parse(text)
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException("The file is not readable: ${e.message}", e)
        } as? Map<*, *> ?: throw BackupFormatException("The file is not a KingPlay backup")
        if (root["format"] != FORMAT) throw BackupFormatException("The file is not a KingPlay backup")
        val version = (root["version"] as? Long)?.toInt() ?: throw BackupFormatException("The backup has no version")
        if (version > VERSION) throw BackupFormatException("The backup was made by a newer version of KingPlay")
        return try {
            BackupData(
                createdAtMs = root.long("createdAtMs"),
                settings = settings(root["settings"] as? Map<*, *> ?: emptyMap<Any, Any>()),
                playlists = root.list("playlists").map {
                    val m = it.asMap()
                    BackupPlaylist(m.string("name"), m.long("createdAtMs"), m.list("tracks").map(::trackOf))
                },
                favorites = root.list("favorites").map(::trackOf),
                presets = root.list("presets").mapNotNull(::presetOf),
                excludedFolders = root.list("excludedFolders").map { it as? String ?: throw bad() },
                lyrics = (root["lyrics"] as? Map<*, *> ?: emptyMap<Any, Any>())
                    .entries.associate { (k, v) -> (k as String) to (v as? String ?: throw bad()) },
                source = root["source"] as? String,
                playStats = root.list("playStats").map {
                    val m = it.asMap()
                    BackupPlayStat(
                        trackOf(m),
                        ((m["count"] as? Number)?.toInt() ?: 0).coerceAtLeast(0),
                        (m["lastPlayedMs"] as? Number)?.toLong() ?: 0,
                    )
                },
                videoProgress = root.list("videoProgress").mapNotNull {
                    val m = it.asMap()
                    val path = m["path"] as? String ?: return@mapNotNull null
                    BackupVideoProgress(
                        path,
                        ((m["positionMs"] as? Number)?.toLong() ?: 0).coerceAtLeast(0),
                        ((m["durationMs"] as? Number)?.toLong() ?: 0).coerceAtLeast(0),
                    )
                },
            )
        } catch (e: ClassCastException) {
            throw BackupFormatException("The backup is damaged", e)
        }
    }

    private fun bad() = BackupFormatException("The backup is damaged")

    private fun Any?.asMap(): Map<*, *> = this as? Map<*, *> ?: throw bad()

    private fun Map<*, *>.string(key: String): String = this[key] as? String ?: throw bad()

    private fun Map<*, *>.long(key: String): Long = (this[key] as? Number)?.toLong() ?: throw bad()

    private fun Map<*, *>.list(key: String): List<*> = when (val v = this[key]) {
        null -> emptyList<Any>()
        is List<*> -> v
        else -> throw bad()
    }

    private fun trackOf(value: Any?): BackupTrack {
        val m = value.asMap()
        return BackupTrack(m.string("path"), m["title"] as? String ?: "", m["artist"] as? String ?: "")
    }

    private fun presetOf(value: Any?): BackupPreset? {
        val m = value.asMap()
        val gains = m.list("bandGainsDb").map { (it as? Number)?.toFloat() ?: throw bad() }
        if (gains.size != EqPreset.BAND_COUNT) return null
        return BackupPreset(m.string("name"), (m["preampDb"] as? Number)?.toFloat() ?: 0f, gains)
    }

    /** Unknown or missing values keep their defaults, so a backup from an older version still restores. */
    private fun settings(m: Map<*, *>): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = enumOrNull<AppThemeMode>(m["themeMode"]) ?: defaults.themeMode,
            accent = enumOrNull<AppAccent>(m["accent"]) ?: defaults.accent,
            dynamicColor = m["dynamicColor"] as? Boolean ?: defaults.dynamicColor,
            lowPowerVisuals = m["lowPowerVisuals"] as? Boolean ?: defaults.lowPowerVisuals,
            uiTheme = enumOrNull<AppUiTheme>(m["uiTheme"]) ?: defaults.uiTheme,
            font = enumOrNull<AppFont>(m["font"]) ?: defaults.font,
            textScale = ((m["textScale"] as? Number)?.toFloat() ?: defaults.textScale)
                .coerceIn(AppSettings.MIN_TEXT_SCALE, AppSettings.MAX_TEXT_SCALE),
            customAccent = (m["customAccent"] as? Number)?.toLong(),
            haptics = (m["haptics"] as? Map<*, *>)?.let { h ->
                val d = defaults.haptics
                HapticSettings(
                    enabled = h["enabled"] as? Boolean ?: d.enabled,
                    play = enumOrNull<HapticLevel>(h["play"]) ?: d.play,
                    transport = enumOrNull<HapticLevel>(h["transport"]) ?: d.transport,
                    toggle = enumOrNull<HapticLevel>(h["toggle"]) ?: d.toggle,
                    tap = enumOrNull<HapticLevel>(h["tap"]) ?: d.tap,
                )
            } ?: defaults.haptics,
            nickname = (m["nickname"] as? String).orEmpty().trim().take(AppSettings.MAX_NICKNAME),
            coverTheme = m["coverTheme"] as? Boolean ?: defaults.coverTheme,
        )
    }

    private inline fun <reified E : Enum<E>> enumOrNull(value: Any?): E? =
        (value as? String)?.let { name -> enumValues<E>().firstOrNull { it.name == name } }
}
