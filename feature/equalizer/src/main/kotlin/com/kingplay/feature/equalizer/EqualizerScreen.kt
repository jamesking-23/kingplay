package com.kingplay.feature.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.component.KpIconButton
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.AudioSettings
import com.kingplay.core.model.EqPreset
import com.kingplay.core.model.ReplayGainMode
import com.kingplay.core.model.navigation.AppNavigator
import com.kingplay.core.ui.R as UiR
import com.kingplay.feature.equalizer.logic.bandLabel
import com.kingplay.feature.equalizer.logic.formatDb
import com.kingplay.feature.equalizer.logic.formatSemitones
import com.kingplay.feature.equalizer.logic.formatTempo
import com.kingplay.feature.equalizer.logic.isFlat

private const val DB_STEPS = 47 // -12..12 in 0.5 dB steps, minus the two ends
private const val PERCENT = 100
private const val MS_PER_SECOND = 1_000
private const val FADE_STEPS = 11
private const val BASS_STEPS = 9
private const val LOUDNESS_STEPS = 19
private const val TEMPO_STEPS = 29
private const val PITCH_STEPS = 23
private const val BAND_LABEL_WIDTH_DP = 40
private const val BAND_VALUE_WIDTH_DP = 64

@Composable
fun EqualizerScreen(navigator: AppNavigator, viewModel: EqualizerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    EqualizerContent(state = state, onEvent = viewModel::onEvent, onBack = navigator::back)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerContent(
    state: EqualizerState,
    onEvent: (EqualizerEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.eq_title), maxLines = 1) },
                navigationIcon = {
                    KpIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(UiR.string.ui_back),
                        onClick = onBack,
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = KpTheme.spacing.lg, vertical = KpTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm),
        ) {
            item { EnableRow(settings.eqEnabled) { onEvent(EqualizerEvent.SetEnabled(it)) } }
            item {
                ResponseCurve(
                    bandGainsDb = settings.bandGainsDb,
                    bassBoostStrength = settings.effects.bassBoostStrength,
                    enabled = settings.eqEnabled,
                )
            }
            item { SectionTitle(R.string.eq_presets) }
            item { PresetRow(state, onEvent) }
            item {
                TextButton(
                    onClick = { onEvent(EqualizerEvent.ResetCurve) },
                    enabled = !isFlat(settings.bandGainsDb, settings.preampDb),
                ) { Text(stringResource(R.string.eq_reset)) }
            }
            item {
                DbSlider(
                    label = stringResource(R.string.eq_preamp),
                    value = settings.preampDb,
                    onChange = { onEvent(EqualizerEvent.SetPreamp(it)) },
                    enabled = settings.eqEnabled,
                )
            }
            item { SectionTitle(R.string.eq_bands) }
            items(EqPreset.BAND_COUNT) { index ->
                val hz = EqPreset.CENTER_FREQUENCIES_HZ[index]
                BandSlider(
                    label = bandLabel(hz),
                    description = stringResource(R.string.eq_band_description, hz.toString()),
                    value = settings.bandGainsDb[index],
                    enabled = settings.eqEnabled,
                    onChange = { onEvent(EqualizerEvent.SetBand(index, it)) },
                )
            }
            item { SectionTitle(R.string.eq_effects) }
            item { EffectSliders(settings, onEvent) }
            item { SectionTitle(R.string.eq_replaygain) }
            item { ReplayGainSection(settings, onEvent) }
            item { SectionTitle(R.string.eq_level) }
            item { LevelSection(settings, onEvent) }
            item { DeviceSection(state, onEvent) }
        }
    }
}

@Composable
private fun SectionTitle(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = KpTheme.spacing.md).semantics { heading() },
    )
}

@Composable
private fun EnableRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val status = stringResource(if (enabled) R.string.eq_enable_on else R.string.eq_enable_off)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.eq_enable), style = MaterialTheme.typography.titleLarge)
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
            modifier = Modifier.semantics { stateDescription = status },
        )
    }
}

@Composable
private fun PresetRow(state: EqualizerState, onEvent: (EqualizerEvent) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
        if (state.settings.presetId == null) {
            item {
                FilterChip(selected = true, onClick = {}, label = { Text(stringResource(R.string.eq_preset_custom)) })
            }
        }
        items(state.presets, key = { it.id }) { preset ->
            FilterChip(
                selected = state.settings.presetId == preset.id,
                onClick = { onEvent(EqualizerEvent.ApplyPreset(preset)) },
                label = { Text(preset.name) },
            )
        }
    }
}

@Composable
private fun DbSlider(label: String, value: Float, onChange: (Float) -> Unit, enabled: Boolean) {
    LabeledSlider(
        label = label,
        valueText = formatDb(value),
        value = value,
        range = -AudioSettings.MAX_PREAMP_DB..AudioSettings.MAX_PREAMP_DB,
        steps = DB_STEPS,
        enabled = enabled,
        onChange = onChange,
    )
}

/** One EQ band: the frequency on the left, the slider in the middle, the gain on the right. */
@Composable
private fun BandSlider(label: String, description: String, value: Float, enabled: Boolean, onChange: (Float) -> Unit) {
    val valueText = formatDb(value)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(BAND_LABEL_WIDTH_DP.dp),
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = EqPreset.MIN_GAIN_DB..EqPreset.MAX_GAIN_DB,
            steps = DB_STEPS,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .semantics { stateDescription = "$description, $valueText" },
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(BAND_VALUE_WIDTH_DP.dp),
        )
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(valueText, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            enabled = enabled,
            modifier = Modifier.semantics { stateDescription = "$label, $valueText" },
        )
    }
}

@Composable
private fun EffectSliders(settings: AudioSettings, onEvent: (EqualizerEvent) -> Unit) {
    val effects = settings.effects
    Column {
        LabeledSlider(
            label = stringResource(R.string.eq_bass),
            valueText = stringResource(
                R.string.eq_percent,
                effects.bassBoostStrength * PERCENT / AudioSettings.MAX_BASS_BOOST,
            ),
            value = effects.bassBoostStrength.toFloat(),
            range = 0f..AudioSettings.MAX_BASS_BOOST.toFloat(),
            steps = BASS_STEPS,
            onChange = { onEvent(EqualizerEvent.SetBass(it.toInt())) },
        )
        LabeledSlider(
            label = stringResource(R.string.eq_loudness),
            valueText = stringResource(
                R.string.eq_percent,
                effects.loudnessGainMb * PERCENT / AudioSettings.MAX_LOUDNESS_MB,
            ),
            value = effects.loudnessGainMb.toFloat(),
            range = 0f..AudioSettings.MAX_LOUDNESS_MB.toFloat(),
            steps = LOUDNESS_STEPS,
            onChange = { onEvent(EqualizerEvent.SetLoudness(it.toInt())) },
        )
        LabeledSlider(
            label = stringResource(R.string.eq_tempo),
            valueText = formatTempo(effects.tempo),
            value = effects.tempo,
            range = AudioSettings.MIN_TEMPO..AudioSettings.MAX_TEMPO,
            steps = TEMPO_STEPS,
            onChange = { onEvent(EqualizerEvent.SetTempo(it)) },
        )
        LabeledSlider(
            label = stringResource(R.string.eq_pitch),
            valueText = formatSemitones(effects.pitchSemitones),
            value = effects.pitchSemitones,
            range = -AudioSettings.MAX_PITCH_SEMITONES..AudioSettings.MAX_PITCH_SEMITONES,
            steps = PITCH_STEPS,
            onChange = { onEvent(EqualizerEvent.SetPitch(it)) },
        )
    }
}

@Composable
private fun ReplayGainSection(settings: AudioSettings, onEvent: (EqualizerEvent) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
            ReplayGainMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.replayGainMode == mode,
                    onClick = { onEvent(EqualizerEvent.SetReplayGain(mode)) },
                    label = {
                        Text(
                            stringResource(
                                when (mode) {
                                    ReplayGainMode.OFF -> R.string.eq_rg_off
                                    ReplayGainMode.TRACK -> R.string.eq_rg_track
                                    ReplayGainMode.ALBUM -> R.string.eq_rg_album
                                },
                            ),
                        )
                    },
                )
            }
        }
        DbSlider(
            label = stringResource(R.string.eq_rg_preamp),
            value = settings.replayGainPreampDb,
            onChange = { onEvent(EqualizerEvent.SetReplayGainPreamp(it)) },
            enabled = settings.replayGainMode != ReplayGainMode.OFF,
        )
    }
}

@Composable
private fun LevelSection(settings: AudioSettings, onEvent: (EqualizerEvent) -> Unit) {
    val fadeSeconds = settings.fadeMs / MS_PER_SECOND
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.eq_headroom), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.eq_headroom_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = settings.autoHeadroom, onCheckedChange = { onEvent(EqualizerEvent.SetHeadroom(it)) })
        }
        LabeledSlider(
            label = stringResource(R.string.eq_fade),
            valueText = if (fadeSeconds == 0) {
                stringResource(R.string.eq_fade_off)
            } else {
                stringResource(R.string.eq_seconds, fadeSeconds)
            },
            value = fadeSeconds.toFloat(),
            range = 0f..(AudioSettings.MAX_FADE_MS / MS_PER_SECOND).toFloat(),
            steps = FADE_STEPS,
            onChange = { onEvent(EqualizerEvent.SetFade(it.toInt() * MS_PER_SECOND)) },
        )
    }
}

@Composable
private fun DeviceSection(state: EqualizerState, onEvent: (EqualizerEvent) -> Unit) {
    val output = state.output ?: return
    Column(modifier = Modifier.padding(top = KpTheme.spacing.md)) {
        SectionTitle(R.string.eq_device)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.eq_device_remember, output.displayName),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(R.string.eq_device_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.hasProfile,
                onCheckedChange = { onEvent(EqualizerEvent.SetRememberForDevice(it)) },
            )
        }
    }
}
