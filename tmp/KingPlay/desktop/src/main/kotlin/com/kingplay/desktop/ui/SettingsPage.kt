package com.kingplay.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingplay.core.model.settings.AppAccent
import com.kingplay.core.model.settings.AppThemeMode
import com.kingplay.desktop.DesktopAppState
import com.kingplay.desktop.settings.DesktopSettings
import com.kingplay.desktop.userdata.ImportOutcome

internal fun themeLabel(mode: AppThemeMode): String = when (mode) {
    AppThemeMode.SYSTEM -> "Match Windows"
    AppThemeMode.LIGHT -> "Light"
    AppThemeMode.DARK -> "Dark"
    AppThemeMode.AMOLED -> "Black"
}

internal fun accentLabel(accent: AppAccent): String = when (accent) {
    AppAccent.VIOLET -> "Violet"
    AppAccent.OCEAN -> "Ocean"
    AppAccent.EMBER -> "Ember"
    AppAccent.EMERALD -> "Emerald"
}

@Composable
fun SettingsPage(
    settings: DesktopSettings,
    onChange: ((DesktopSettings) -> DesktopSettings) -> Unit,
    onImportBackup: () -> Unit,
    onExportBackup: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.themeMode == mode,
                    onClick = { onChange { it.copy(themeMode = mode) } },
                    label = { Text(themeLabel(mode)) },
                )
            }
        }

        Text("Accent colour", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppAccent.entries.forEach { accent ->
                FilterChip(
                    selected = settings.accent == accent,
                    onClick = { onChange { it.copy(accent = accent) } },
                    label = { Text(accentLabel(accent)) },
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Your data", style = MaterialTheme.typography.titleMedium)
        Text(
            "Playlists and favorites can move between KingPlay on your phone and on this PC. " +
                "On the phone, use Settings → Back up; here, choose that file. Songs are matched by file name, " +
                "so copy your music across first. Importing only adds; nothing is overwritten.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onImportBackup) { Text("Import backup") }
            OutlinedButton(onClick = onExportBackup) { Text("Export backup") }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            "KingPlay for Windows $APP_VERSION. Sound by VLC. Your music files are only read, never changed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun importBackup(app: DesktopAppState, onNotice: (String) -> Unit) {
    val (_, text) = chooseTextFile("Choose a KingPlay backup", "KingPlay backup", "json", "kingplay", "txt") ?: return
    when (val outcome = app.importBackup(text)) {
        is ImportOutcome.Invalid -> onNotice("That file is not a KingPlay backup: ${outcome.reason}")
        is ImportOutcome.Done -> {
            val s = outcome.summary
            onNotice(
                "Added ${s.playlistsAdded} playlists (${s.playlistsSkipped} already existed). " +
                    "Found ${s.tracksMatched} of ${s.tracksTotal} playlist songs and " +
                    "${s.favoritesMatched} of ${s.favoritesTotal} favorites.",
            )
        }
    }
}

internal fun exportBackup(app: DesktopAppState, onNotice: (String) -> Unit) {
    val saved = saveTextFile("Save backup", "kingplay-pc-backup.json", app.exportBackup())
    onNotice(if (saved != null) "Saved $saved" else "The backup was not saved.")
}

private const val APP_VERSION = "0.1.0"
