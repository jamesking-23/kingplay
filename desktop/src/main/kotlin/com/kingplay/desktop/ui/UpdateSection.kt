package com.kingplay.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingplay.core.model.update.ReleaseInfo
import com.kingplay.desktop.update.Updater
import com.kingplay.desktop.update.installedTag
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data object UpToDate : UpdateUi
    data object Failed : UpdateUi
    data object DownloadFailed : UpdateUi
    data class Available(val release: ReleaseInfo) : UpdateUi
    data class Downloading(val percent: Int) : UpdateUi
    data object Installing : UpdateUi
}

/** "Check for updates": downloads the newest installer, runs it, and starts KingPlay again afterwards. */
@Composable
fun UpdateSection() {
    var ui by remember { mutableStateOf<UpdateUi>(UpdateUi.Idle) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Installed version: $installedTag", style = MaterialTheme.typography.bodyMedium)
        Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            when (val s = ui) {
                UpdateUi.Idle -> Unit
                UpdateUi.Checking -> Text("Checking for updates…")
                UpdateUi.UpToDate -> Text("You have the latest version.")
                UpdateUi.Failed -> Text("Could not check for updates. Check your internet connection.", color = MaterialTheme.colorScheme.error)
                UpdateUi.DownloadFailed -> Text("The download failed. Try again.", color = MaterialTheme.colorScheme.error)
                is UpdateUi.Available -> Text("A new version is available: ${s.release.tag}")
                is UpdateUi.Downloading -> {
                    Text("Downloading… ${s.percent}%")
                    LinearProgressIndicator(progress = { s.percent / 100f }, modifier = Modifier.fillMaxWidth())
                }
                UpdateUi.Installing -> Text("Starting the installer. KingPlay will close and open again when it is done.")
            }
        }
        when (val s = ui) {
            is UpdateUi.Available -> Button(onClick = {
                ui = UpdateUi.Downloading(0)
                scope.launch {
                    val file = withContext(Dispatchers.IO) { Updater.download(s.release) { ui = UpdateUi.Downloading(it) } }
                    if (file == null) {
                        ui = UpdateUi.DownloadFailed
                    } else {
                        ui = UpdateUi.Installing
                        withContext(Dispatchers.IO) { Updater.installAndRestart(file) }
                        exitProcess(0)
                    }
                }
            }) { Text("Download and install") }
            UpdateUi.Checking, is UpdateUi.Downloading, UpdateUi.Installing -> Unit
            else -> OutlinedButton(onClick = {
                ui = UpdateUi.Checking
                scope.launch {
                    ui = when (val r = withContext(Dispatchers.IO) { Updater.check() }) {
                        Updater.Result.UpToDate -> UpdateUi.UpToDate
                        Updater.Result.Failed -> UpdateUi.Failed
                        is Updater.Result.Available -> UpdateUi.Available(r.release)
                    }
                }
            }) { Text("Check for updates") }
        }
        Text(
            "You can also update by uninstalling KingPlay and installing the newest version from the project page.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
