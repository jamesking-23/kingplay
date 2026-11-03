package com.kingplay.desktop.update

import com.kingplay.core.model.update.ReleaseInfo
import com.kingplay.core.model.update.UpdateCheck
import com.kingplay.core.model.update.UpdatePlatform
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** The release this program was built from ("v1.0.0-preview9"), or "dev" when run from source. */
val installedTag: String = System.getProperty("kingplay.tag")?.takeIf { it.isNotBlank() } ?: "dev"

/** Looks for a newer Windows installer among the project's GitHub releases, downloads it and runs it. */
object Updater {
    private const val RELEASES_URL = "https://api.github.com/repos/jamesking-23/kingplay/releases?per_page=15"
    private const val TIMEOUT_MS = 20_000
    private const val BUFFER = 64 * 1024

    sealed interface Result {
        data object UpToDate : Result
        data class Available(val release: ReleaseInfo) : Result
        data object Failed : Result
    }

    fun check(): Result {
        val body = runCatching { get(RELEASES_URL) }.getOrNull() ?: return Result.Failed
        val newest = UpdateCheck.newest(body, UpdatePlatform.WINDOWS) ?: return Result.Failed
        return if (UpdateCheck.isNewer(newest.tag, installedTag)) Result.Available(newest) else Result.UpToDate
    }

    /** Downloads the installer to a temporary folder, reporting percent done. Returns null if it failed. */
    fun download(release: ReleaseInfo, onProgress: (Int) -> Unit): File? = runCatching {
        val dir = updateDir()
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, release.fileName)
        val conn = open(release.downloadUrl)
        try {
            check(conn.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${conn.responseCode}" }
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.sizeBytes
            var done = 0L
            conn.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) onProgress((done * 100 / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            if (release.sizeBytes > 0) check(target.length() == release.sizeBytes) { "Incomplete download" }
            writeMeta(dir, release)
        } catch (t: Throwable) {
            target.delete()
            throw t
        } finally {
            conn.disconnect()
        }
        target
    }.getOrNull()

    private fun updateDir() = File(System.getProperty("java.io.tmpdir"), "kingplay-update").apply { mkdirs() }

    private const val SEP = "\u0001"

    private fun writeMeta(dir: File, r: ReleaseInfo) {
        runCatching {
            File(dir, "update.meta").writeText(
                listOf(r.tag, r.sizeBytes, r.downloadUrl, r.fileName, r.notes.replace("\n", SEP)).joinToString("\n"),
            )
        }
    }

    /** A finished download from an earlier visit that has not been installed: the release and its installer file. */
    fun restoreDownloaded(): Pair<ReleaseInfo, File>? = runCatching {
        val dir = updateDir()
        val lines = File(dir, "update.meta").readText().split("\n")
        val release = ReleaseInfo(
            tag = lines[0],
            sizeBytes = lines[1].toLong(),
            downloadUrl = lines[2],
            fileName = lines[3],
            notes = lines.getOrElse(4) { "" }.replace(SEP, "\n"),
        )
        val file = File(dir, release.fileName)
        val complete = file.exists() && (release.sizeBytes <= 0 || file.length() == release.sizeBytes)
        if (complete && UpdateCheck.isNewer(release.tag, installedTag)) {
            release to file
        } else {
            dir.listFiles()?.forEach { it.delete() }
            null
        }
    }.getOrNull()

    /**
     * Runs the installer and, when it finishes, starts KingPlay again. The caller must close the program straight
     * afterwards so Windows can replace its files.
     */
    fun installAndRestart(installer: File) {
        val app = ProcessHandle.current().info().command().orElse(null)
        val relaunch = if (app != null && app.endsWith("KingPlay.exe", ignoreCase = true)) " && start \"\" \"$app\"" else ""
        ProcessBuilder("cmd", "/c", "start \"\" /wait \"${installer.absolutePath}\"$relaunch").start()
    }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "KingPlay-updater")
    }

    private fun get(url: String): String {
        val conn = open(url)
        return try {
            check(conn.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${conn.responseCode}" }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
