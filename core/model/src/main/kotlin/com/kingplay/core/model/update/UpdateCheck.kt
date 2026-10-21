package com.kingplay.core.model.update

import com.kingplay.core.model.backup.Json

/** A published KingPlay release that has an installer for this platform. */
data class ReleaseInfo(
    val tag: String,
    val notes: String,
    val downloadUrl: String,
    val fileName: String,
    val sizeBytes: Long,
)

/** Which installer file to look for in a release. */
enum class UpdatePlatform(val extension: String) { ANDROID(".apk"), WINDOWS(".exe") }

/** The tag of the build that is running, such as "v1.0.0-preview8", or "dev" for a build that is not a release. */
data class InstalledVersion(val tag: String)

object UpdateCheck {
    private const val FINAL_RELEASE_RANK = 1_000_000L
    private val NUMBER = Regex("\\d+")

    /** Release tags as comparable numbers: v1.0.0-preview8 is [1, 0, 0, 8]; a final release ranks above every preview. */
    fun versionOf(tag: String): List<Long>? {
        val numbers = NUMBER.findAll(tag).map { it.value.toLongOrNull() ?: return null }.toList()
        if (numbers.isEmpty()) return null
        val preview = listOf("preview", "beta", "alpha", "rc").any { tag.contains(it, ignoreCase = true) }
        return if (preview) numbers else numbers + FINAL_RELEASE_RANK
    }

    /** True when [latest] is a newer release than [installed]. A build that is not a release can always be updated. */
    fun isNewer(latest: String, installed: String): Boolean {
        val new = versionOf(latest) ?: return false
        val old = versionOf(installed) ?: return true
        for (i in 0 until maxOf(new.size, old.size)) {
            val a = new.getOrElse(i) { 0L }
            val b = old.getOrElse(i) { 0L }
            if (a != b) return a > b
        }
        return false
    }

    /** The newest published release that carries an installer for [platform], or null. */
    fun newest(releasesJson: String, platform: UpdatePlatform): ReleaseInfo? {
        val list = runCatching { Json.parse(releasesJson) as? List<*> }.getOrNull() ?: return null
        var best: ReleaseInfo? = null
        for (entry in list) {
            val release = entry as? Map<*, *> ?: continue
            if (release["draft"] == true) continue
            val tag = release["tag_name"] as? String ?: continue
            val asset = (release["assets"] as? List<*>).orEmpty()
                .mapNotNull { it as? Map<*, *> }
                .firstOrNull { (it["name"] as? String)?.endsWith(platform.extension, ignoreCase = true) == true }
                ?: continue
            val url = asset["browser_download_url"] as? String ?: continue
            if (!url.startsWith("https://")) continue
            val info = ReleaseInfo(
                tag = tag,
                notes = (release["body"] as? String).orEmpty().trim(),
                downloadUrl = url,
                fileName = asset["name"] as? String ?: "update${platform.extension}",
                sizeBytes = (asset["size"] as? Number)?.toLong() ?: 0L,
            )
            if (best == null || isNewer(tag, best.tag)) best = info
        }
        return best
    }
}
