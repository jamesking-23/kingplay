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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingplay.desktop.update.UpdateModel
import com.kingplay.desktop.update.UpdateUi
import com.kingplay.desktop.update.Updater
import com.kingplay.desktop.update.installedTag
import kotlin.system.exitProcess

/** "Check for updates": downloads the newest installer, runs it, and starts KingPlay again afterwards. */
@Composable
fun UpdateSection() {
    val ui by UpdateModel.state.collectAsState()
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
                    Text("Downloading… ${s.percent}%. You can keep using KingPlay.")
                    LinearProgressIndicator(progress = { s.percent / 100f }, modifier = Modifier.fillMaxWidth())
                }
                is UpdateUi.Ready -> Text("Version ${s.release.tag} is downloaded and ready to install.")
            }
        }
        when (val s = ui) {
            is UpdateUi.Available -> Button(onClick = { UpdateModel.download(s.release) }) { Text("Download update") }
            is UpdateUi.Ready -> Button(onClick = {
                Updater.installAndRestart(s.file)
                exitProcess(0)
            }) { Text("Install version ${s.release.tag} and restart") }
            UpdateUi.Checking, is UpdateUi.Downloading -> Unit
            else -> OutlinedButton(onClick = UpdateModel::check) { Text("Check for updates") }
        }
        Text(
            "You can also update by uninstalling KingPlay and installing the newest version.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
