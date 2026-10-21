package com.kingplay.feature.settings.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.update.InstalledVersion
import com.kingplay.core.model.update.ReleaseInfo
import com.kingplay.core.model.update.UpdateCheck
import com.kingplay.core.model.update.UpdatePlatform
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class Downloading(val release: ReleaseInfo, val percent: Int) : UpdateState

    /** The file is downloaded and the system installer has been opened. */
    data class Installing(val release: ReleaseInfo) : UpdateState

    /** Android needs the person to allow KingPlay to install apps first. */
    data class NeedsPermission(val release: ReleaseInfo) : UpdateState
    data class Failed(val reason: Reason) : UpdateState

    enum class Reason { OFFLINE, NO_RELEASE, DOWNLOAD, WRONG_FILE }
}

/** Checks the project's GitHub releases for a newer build and installs it through Android's own installer. */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val installed: InstalledVersion,
) : ViewModel() {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()
    private var job: Job? = null

    fun check() {
        if (_state.value is UpdateState.Checking || _state.value is UpdateState.Downloading) return
        _state.value = UpdateState.Checking
        job = viewModelScope.launch {
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

    fun install(release: ReleaseInfo) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            _state.value = UpdateState.NeedsPermission(release)
            return
        }
        _state.value = UpdateState.Downloading(release, 0)
        job = viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching { download(release) { p -> _state.value = UpdateState.Downloading(release, p) } }.getOrNull()
            }
            if (file == null) {
                _state.value = UpdateState.Failed(UpdateState.Reason.DOWNLOAD)
                return@launch
            }
            if (!isOurPackage(file)) {
                file.delete()
                _state.value = UpdateState.Failed(UpdateState.Reason.WRONG_FILE)
                return@launch
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_TYPE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            _state.value = UpdateState.Installing(release)
        }
    }

    /** Opens Android's "install unknown apps" page for KingPlay. */
    fun openPermissionSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun isOurPackage(file: File): Boolean =
        context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)?.packageName == context.packageName

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

    private fun download(release: ReleaseInfo, onProgress: (Int) -> Unit): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "KingPlay-update.apk")
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

    private companion object {
        const val RELEASES_URL = "https://api.github.com/repos/jamesking-23/kingplay/releases?per_page=15"
        const val APK_TYPE = "application/vnd.android.package-archive"
        const val TIMEOUT_MS = 20_000
        const val BUFFER = 64 * 1024
        const val PERCENT = 100
    }
}
