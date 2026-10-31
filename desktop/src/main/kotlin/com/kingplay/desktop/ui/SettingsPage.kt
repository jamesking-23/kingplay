package com.kingplay.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import com.kingplay.core.model.settings.AppSettings
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingplay.core.model.settings.AppAccent
import com.kingplay.core.model.settings.AppThemeMode
import com.kingplay.core.model.settings.AppUiTheme
import com.kingplay.desktop.DesktopAppState
import com.kingplay.desktop.settings.DesktopSettings
import com.kingplay.desktop.userdata.ImportOutcome
import com.kingplay.desktop.userdata.importNotice

internal fun uiThemeLabel(ui: AppUiTheme): String = when (ui) {
    AppUiTheme.KING_ART -> "King Art (hand-drawn)"
    AppUiTheme.KK_UI -> "kk-ui (classic)"
    AppUiTheme.KING_NEON -> "KingNeon (night-club)"
    AppUiTheme.KING_GLASS -> "KingGlass (airy)"
    AppUiTheme.KING_CLASSIC -> "KingClassic (vintage hi-fi)"
}

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
    onOpenMini: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        Text("UI theme", style = MaterialTheme.typography.titleMedium)
        Text(
            "Switch designs like switching apps; your music and settings stay the same.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppUiTheme.entries.forEach { ui ->
                FilterChip(
                    selected = settings.uiTheme == ui,
                    onClick = { onChange { it.copy(uiTheme = ui) } },
                    label = { Text(uiThemeLabel(ui)) },
                )
            }
        }

        Text("Light or dark", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.themeMode == mode,
                    onClick = { onChange { it.copy(themeMode = mode) } },
                    label = { Text(themeLabel(mode)) },
                )
            }
        }

        Text("Font", style = MaterialTheme.typography.titleMedium)
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            com.kingplay.core.model.settings.AppFont.entries.forEach { font ->
                FilterChip(
                    selected = settings.font == font,
                    onClick = { onChange { it.copy(font = font) } },
                    label = { Text(fontLabel(font)) },
                )
            }
        }
        Text(
            "The quick brown fox jumps over the lazy dog. 0123456789",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text("Text size ${(settings.textScale * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
        androidx.compose.material3.Slider(
            value = settings.textScale,
            onValueChange = { v -> onChange { it.copy(textScale = (Math.round(v * 20) / 20f)) } },
            valueRange = AppSettings.MIN_TEXT_SCALE..AppSettings.MAX_TEXT_SCALE,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        Text("Your own colour", style = MaterialTheme.typography.titleMedium)
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilterChip(
                selected = settings.customAccent == null,
                onClick = { onChange { it.copy(customAccent = null) } },
                label = { Text("Design colour") },
            )
            CUSTOM_COLOURS.forEach { (name, argb) ->
                FilterChip(
                    selected = settings.customAccent == argb,
                    onClick = { onChange { it.copy(customAccent = argb) } },
                    label = { Text(name) },
                    leadingIcon = {
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .size(14.dp)
                                .background(androidx.compose.ui.graphics.Color(argb.toInt()), androidx.compose.foundation.shape.CircleShape),
                        )
                    },
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Switch(
                checked = settings.coverTheme,
                onCheckedChange = { on -> onChange { it.copy(coverTheme = on) } },
                modifier = Modifier.semantics { contentDescription = "Colour from cover art" },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Colour from cover art")
                Text(
                    "Blend the app's colours with the cover of the song that is playing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (settings.uiTheme == AppUiTheme.KK_UI) {
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
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Window", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Switch(
                checked = settings.closeToTray,
                onCheckedChange = { on -> onChange { it.copy(closeToTray = on) } },
                modifier = Modifier.semantics { contentDescription = TRAY_LABEL },
            )
            Text(TRAY_LABEL, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Switch(
                checked = settings.globalMediaKeys,
                onCheckedChange = { on -> onChange { it.copy(globalMediaKeys = on) } },
                modifier = Modifier.semantics { contentDescription = KEYS_LABEL },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(KEYS_LABEL)
                Text(
                    "Takes effect the next time KingPlay starts. A key another program already uses is left to it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(onClick = onOpenMini) { Text("Open mini player") }

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
        Text("Updates", style = MaterialTheme.typography.titleMedium)
        UpdateSection()

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("About", style = MaterialTheme.typography.titleMedium)
        AboutSection()
        Text(
            "Sound by VLC. Your music files are only read, never changed.",
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
            onNotice(importNotice(outcome.summary))
        }
    }
}

internal fun exportBackup(app: DesktopAppState, onNotice: (String) -> Unit) {
    val saved = saveTextFile("Save backup", "kingplay-pc-backup.json", app.exportBackup())
    onNotice(if (saved != null) "Saved $saved" else "The backup was not saved.")
}


private const val TRAY_LABEL = "Keep playing in the tray when the window is closed"

private const val KEYS_LABEL = "Use the keyboard's media keys even when KingPlay is in the background"

private val CUSTOM_COLOURS = listOf(
    "Crimson" to 0xFFD32F2F,
    "Orange" to 0xFFF57C00,
    "Gold" to 0xFFFBC02D,
    "Green" to 0xFF2E7D32,
    "Teal" to 0xFF00897B,
    "Sky" to 0xFF0288D1,
    "Indigo" to 0xFF3949AB,
    "Pink" to 0xFFD81B60,
)
