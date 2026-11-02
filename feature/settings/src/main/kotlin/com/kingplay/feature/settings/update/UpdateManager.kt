package com.kingplay.feature.settings.update

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.kingplay.core.model.update.InstalledVersion
import com.kingplay.core.model.update.ReleaseInfo
import com.kingplay.core.model.update.UpdateCheck
import com.kingplay.core.model.update.UpdatePlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns checking for, downloading and installing updates. It lives as long as the app process, not as long as the
 * Settings screen, so a download carries on while the person looks at other screens or other apps, and a finished
 * download is remembered on disk: coming back later shows "Install" instead of downloading again.
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    val installed: InstalledVersion,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()
    private var job: Job? = null
    private var startedActivities = 0
    private val dir get() = File(context.cacheDir, DIR).apply { mkdirs() }
    private val apk get() = File(dir, APK_NAME)
    private val meta get() = File(dir, META_NAME)

    init {
        (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityStarted(activity: Activity) {
                    startedActivities++
                    // Back from the installer without installing: the download is still there to install.
                    val current = _state.value
                    if (current is UpdateState.Installing) _state.value = UpdateState.Ready(current.release)
                }
                override fun onActivityStopped(activity: Activity) {
                    startedActivities = (startedActivities - 1).coerceAtLeast(0)
                }
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
        restoreDownloaded()
    }

    private val inForeground get() = startedActivities > 0

    /** A finished download from an earlier visit (or earlier run) that has not been installed yet. */
    private fun restoreDownloaded() {
        val release = readMeta()
        if (release == null || !apk.exists() || !isOurPackage(apk) || !UpdateCheck.isNewer(release.tag, installed.tag)) {
            clearDownloaded()
            return
        }
        _state.value = UpdateState.Ready(release)
    }

    fun check() {
        val now = _state.value
        if (now is UpdateState.Checking || now is UpdateState.Downloading || now is UpdateState.Ready) return
        _state.value = UpdateState.Checking
        job = scope.launch {
            val body = withContext(Dispatchers.IO) { runCatching { fetch(RELEASES_URL) }.getOrNull() }
            _state.value = when {
                body == null -> UpdateState.Failed(UpdateState.Reason.OFFLINE)
                else -> {
                    val newest = UpdateCheck.newest(body, UpdatePlatform.ANDROID)
                    when {
                        newest == null -> UpdateState.Failed(UpdateState.Reason.NO_RELEASE)
                        UpdateCheck.isNewer(newest.tag, installed.tag) -> UpdateState.Available(newest)
                        else -> UpdateState.UpToDate
                    }
                }
            }
        }
    }

    /** Downloads [release] in the background, then opens the installer (or notifies, if the app is not on screen). */
    fun download(release: ReleaseInfo) {
        if (_state.value is UpdateState.Downloading) return
        if (!context.packageManager.canRequestPackageInstalls()) {
            _state.value = UpdateState.NeedsPermission(release)
            return
        }
        _state.value = UpdateState.Downloading(release, 0)
        job = scope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching { downloadFile(release) { p -> _state.value = UpdateState.Downloading(release, p) } }.getOrNull()
            }
            if (file == null) {
                _state.value = UpdateState.Failed(UpdateState.Reason.DOWNLOAD)
                return@launch
            }
            if (!isOurPackage(file)) {
                clearDownloaded()
                _state.value = UpdateState.Failed(UpdateState.Reason.WRONG_FILE)
                return@launch
            }
            writeMeta(release)
            _state.value = UpdateState.Ready(release)
            if (inForeground) installReady() else notifyReady(release)
        }
    }

    /** Opens Android's installer for the downloaded update. The file stays until the new version replaces the app. */
    fun installReady() {
        val ready = _state.value as? UpdateState.Ready ?: return
        if (!context.packageManager.canRequestPackageInstalls()) {
            _state.value = UpdateState.NeedsPermission(ready.release)
            return
        }
        if (!apk.exists()) {
            clearDownloaded()
            _state.value = UpdateState.Available(ready.release)
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_TYPE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        _state.value = UpdateState.Installing(ready.release)
    }

    /** Opens Android's "install unknown apps" page for KingPlay. */
    fun openPermissionSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** After the person allowed installs: carry on with the download or the install that was waiting. */
    fun permissionGranted() {
        val waiting = (_state.value as? UpdateState.NeedsPermission)?.release ?: return
        if (apk.exists() && readMeta()?.tag == waiting.tag) {
            _state.value = UpdateState.Ready(waiting)
            installReady()
        } else {
            download(waiting)
        }
    }

    private fun notifyReady(release: ReleaseInfo) {
        val allowed = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33
        if (!allowed) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "App updates", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?.putExtra(EXTRA_INSTALL_UPDATE, true)
        val tap = PendingIntent.getActivity(
            context,
            0,
            open ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("KingPlay ${release.tag} is ready")
                .setContentText("Tap to install the update.")
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun isOurPackage(file: File): Boolean =
        context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)?.packageName == context.packageName

    private fun writeMeta(release: ReleaseInfo) {
        runCatching {
            meta.writeText(
                listOf(release.tag, release.sizeBytes, release.downloadUrl, release.fileName, release.notes.replace("\n", SEP))
                    .joinToString("\n"),
            )
        }
    }

    private fun readMeta(): ReleaseInfo? = runCatching {
        val lines = meta.readText().split("\n")
        ReleaseInfo(
            tag = lines[0],
            sizeBytes = lines[1].toLong(),
            downloadUrl = lines[2],
            fileName = lines[3],
            notes = lines.getOrElse(4) { "" }.replace(SEP, "\n"),
        )
    }.getOrNull()

    private fun clearDownloaded() {
        runCatching { dir.listFiles()?.forEach { it.delete() } }
    }

    private fun fetch(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "KingPlay-updater")
        return try {
            check(conn.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${conn.responseCode}" }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadFile(release: ReleaseInfo, onProgress: (Int) -> Unit): File {
        clearDownloaded()
        val target = apk
        val conn = URL(release.downloadUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.setRequestProperty("User-Agent", "KingPlay-updater")
        try {
            check(conn.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${conn.responseCode}" }
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.sizeBytes
            var done = 0L
            var lastPercent = -1
            conn.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        val percent = if (total > 0) (done * PERCENT / total).toInt().coerceIn(0, PERCENT) else 0
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(percent)
                        }
                    }
                }
            }
            if (release.sizeBytes > 0) check(target.length() == release.sizeBytes) { "Incomplete download" }
        } catch (t: Throwable) {
            target.delete()
            throw t
        } finally {
            conn.disconnect()
        }
        return target
    }

    companion object {
        /** Intent extra MainActivity checks, set by the "update is ready" notification. */
        const val EXTRA_INSTALL_UPDATE = "kp_install_update"
        private const val SEP = "\u0001"
        private const val DIR = "updates"
        private const val APK_NAME = "KingPlay-update.apk"
        private const val META_NAME = "update.meta"
        private const val CHANNEL = "updates_ready"
        private const val NOTIFICATION_ID = 4107
        private const val RELEASES_URL = "https://api.github.com/repos/jamesking-23/kingplay/releases?per_page=15"
        private const val APK_TYPE = "application/vnd.android.package-archive"
        private const val TIMEOUT_MS = 20_000
        private const val BUFFER = 64 * 1024
        private const val PERCENT = 100
    }
}
