package com.kingplay.desktop.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingplay.core.model.BuiltInEqPresets
import com.kingplay.core.model.EqPreset
import com.kingplay.desktop.settings.DesktopSettings
import com.kingplay.desktop.settings.asUserPreset
import com.kingplay.desktop.settings.flat
import com.kingplay.desktop.settings.matchingPreset
import com.kingplay.desktop.settings.matchingUserPreset
import com.kingplay.desktop.settings.withBand
import com.kingplay.desktop.settings.withPreamp
import com.kingplay.desktop.settings.withPreset
import com.kingplay.desktop.settings.withUserPreset
import com.kingplay.desktop.userdata.UserEqPreset

internal fun bandLabel(hz: Int): String = if (hz >= KILO) "${hz / KILO} kHz" else "$hz Hz"

internal fun gainLabel(db: Float): String = "%+.1f dB".format(db)

@Composable
fun EqualizerPage(
    settings: DesktopSettings,
    savedPresets: List<UserEqPreset>,
    onChange: ((DesktopSettings) -> DesktopSettings) -> Unit,
    onSavePreset: (UserEqPreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onDialog: (NameDialogState) -> Unit,
) {
    val current = settings.matchingPreset()
    val currentSaved = settings.matchingUserPreset(savedPresets)
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Equalizer", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(if (settings.eqEnabled) "On" else "Off", modifier = Modifier.padding(end = 8.dp))
            Switch(
                checked = settings.eqEnabled,
                onCheckedChange = { on -> onChange { it.copy(eqEnabled = on) } },
                modifier = Modifier.semantics { contentDescription = "Equalizer" },
            )
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BuiltInEqPresets.all.forEach { preset ->
                FilterChip(
                    selected = current?.id == preset.id,
                    onClick = { onChange { it.withPreset(preset).copy(eqEnabled = true) } },
                    label = { Text(preset.name) },
                )
            }
        }
        if (savedPresets.isNotEmpty()) {
            Text("My presets", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                savedPresets.forEach { preset ->
                    FilterChip(
                        selected = currentSaved?.name == preset.name,
                        onClick = { onChange { it.withUserPreset(preset).copy(eqEnabled = true) } },
                        label = { Text(preset.name) },
                    )
                }
            }
        }
        if (current == null && currentSaved == null) {
            Text("Custom", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        GainSlider("Preamp", settings.eqPreampDb, enabled = settings.eqEnabled) { db -> onChange { it.withPreamp(db) } }
        EqPreset.CENTER_FREQUENCIES_HZ.forEachIndexed { band, hz ->
            GainSlider(bandLabel(hz), settings.eqGainsDb[band], enabled = settings.eqEnabled) { db ->
                onChange { it.withBand(band, db) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onChange { it.flat() } }) { Text("Reset to flat") }
            OutlinedButton(onClick = {
                onDialog(NameDialogState("Save preset", currentSaved?.name.orEmpty(), "Save") { name ->
                    onSavePreset(settings.asUserPreset(name))
                })
            }) { Text("Save as preset") }
            if (currentSaved != null) {
                OutlinedButton(onClick = { onDeletePreset(currentSaved.name) }) {
                    Text("Delete \"${currentSaved.name}\"")
                }
            }
        }
        Text(
            "The sound engine's bands sit close to, not exactly on, these frequencies, so presets are approximate.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GainSlider(label: String, db: Float, enabled: Boolean, onValue: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(LABEL_WIDTH), style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = db,
            onValueChange = onValue,
            valueRange = EqPreset.MIN_GAIN_DB..EqPreset.MAX_GAIN_DB,
            enabled = enabled,
            modifier = Modifier.weight(1f).semantics { contentDescription = "$label ${gainLabel(db)}" },
        )
        Text(gainLabel(db), modifier = Modifier.width(VALUE_WIDTH), style = MaterialTheme.typography.labelMedium)
    }
}

private const val KILO = 1000
private val LABEL_WIDTH = 72.dp
private val VALUE_WIDTH = 64.dp
