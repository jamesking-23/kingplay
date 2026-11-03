package com.kingplay.feature.video.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.ui.format.formatDuration
import com.kingplay.feature.video.R
import com.kingplay.feature.video.logic.AbLoop
import com.kingplay.feature.video.logic.RepeatChoice
import com.kingplay.feature.video.logic.SleepTimerMinutes
import com.kingplay.feature.video.logic.SpeedSteps
import com.kingplay.feature.video.logic.VideoResize
import com.kingplay.feature.video.logic.formatTimerMinutes
import com.kingplay.feature.video.logic.formatSpeed
import kotlin.math.abs

private val OptionHeight = 48.dp

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = KpTheme.spacing.lg).semantics { heading() },
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = OptionHeight)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = KpTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = KpTheme.spacing.md),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSheet(current: Float, onPick: (Float) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = KpTheme.spacing.lg)) {
            SheetTitle(stringResource(R.string.video_speed, formatSpeed(current)))
            SpeedSteps.forEach { speed ->
                RadioRow(
                    label = formatSpeed(speed),
                    selected = abs(speed - current) < SPEED_TOLERANCE,
                    onClick = {
                        onPick(speed)
                        onDismiss()
                    },
                )
            }
        }
    }
}

private const val SPEED_TOLERANCE = 0.01f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSheet(options: List<TrackOption>, onPick: (TrackOption) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = KpTheme.spacing.lg),
        ) {
            SheetTitle(stringResource(R.string.video_audio_tracks))
            if (options.size < 2) {
                Text(
                    text = stringResource(R.string.video_no_audio_tracks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(KpTheme.spacing.lg),
                )
            }
            options.forEach { option ->
                RadioRow(
                    label = option.label,
                    selected = option.selected,
                    onClick = {
                        onPick(option)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSheet(
    options: List<TrackOption>,
    subtitlesOn: Boolean,
    sizeIndex: Int,
    onPick: (TrackOption) -> Unit,
    onOff: () -> Unit,
    onLoadFile: () -> Unit,
    onSize: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sizeLabels = listOf(
        R.string.video_size_small,
        R.string.video_size_normal,
        R.string.video_size_large,
        R.string.video_size_huge,
    )
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = KpTheme.spacing.lg),
        ) {
            SheetTitle(stringResource(R.string.video_subtitles))
            RadioRow(
                label = stringResource(R.string.video_subtitles_off),
                selected = !subtitlesOn || options.none { it.selected },
                onClick = {
                    onOff()
                    onDismiss()
                },
            )
            options.forEach { option ->
                RadioRow(
                    label = option.label,
                    selected = subtitlesOn && option.selected,
                    onClick = {
                        onPick(option)
                        onDismiss()
                    },
                )
            }
            Text(
                text = stringResource(R.string.video_subtitles_load),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = OptionHeight)
                    .clickable(role = Role.Button) {
                        onLoadFile()
                        onDismiss()
                    }
                    .padding(horizontal = KpTheme.spacing.lg, vertical = KpTheme.spacing.md),
            )
            Text(
                text = stringResource(R.string.video_subtitle_size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = KpTheme.spacing.lg).semantics { heading() },
            )
            Row(
                modifier = Modifier.padding(horizontal = KpTheme.spacing.lg, vertical = KpTheme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm),
            ) {
                sizeLabels.forEachIndexed { index, label ->
                    FilterChip(
                        selected = index == sizeIndex,
                        onClick = { onSize(index) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
        }
    }
}

/** Picture shape, repeat, A–B repeat, sleep timer and mute in one place, like VLC's playback menus. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoOptionsSheet(
    resize: VideoResize,
    repeat: RepeatChoice,
    abLoop: AbLoop,
    sleepMinutes: Int,
    muted: Boolean,
    onResize: (VideoResize) -> Unit,
    onRepeat: (RepeatChoice) -> Unit,
    onMarkAb: () -> Unit,
    onClearAb: () -> Unit,
    onSleep: (Int) -> Unit,
    onMute: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = KpTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm),
        ) {
            SheetTitle(stringResource(R.string.video_options))
            SectionLabel(stringResource(R.string.video_options_picture))
            FlowRow(
                modifier = Modifier.padding(horizontal = KpTheme.spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm),
            ) {
                VideoResize.entries.forEach { choice ->
                    FilterChip(
                        selected = choice == resize,
                        onClick = { onResize(choice) },
                        label = { Text(stringResource(resizeLabel(choice))) },
                    )
                }
            }
            SectionLabel(stringResource(R.string.video_options_repeat))
            Row(
                modifier = Modifier.padding(horizontal = KpTheme.spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm),
            ) {
                listOf(
                    RepeatChoice.OFF to R.string.video_repeat_off,
                    RepeatChoice.ONE to R.string.video_repeat_one,
                    RepeatChoice.ALL to R.string.video_repeat_all,
                ).forEach { (choice, label) ->
                    FilterChip(
                        selected = choice == repeat,
                        onClick = { onRepeat(choice) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
            SectionLabel(stringResource(R.string.video_options_ab))
            Text(
                text = when {
                    abLoop.active -> stringResource(
                        R.string.video_ab_active,
                        formatDuration(abLoop.start ?: 0L),
                        formatDuration(abLoop.end ?: 0L),
                    )
                    abLoop.start != null -> stringResource(R.string.video_ab_start, formatDuration(abLoop.start))
                    else -> stringResource(R.string.video_ab_none)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = KpTheme.spacing.lg),
            )
            Row(modifier = Modifier.padding(horizontal = KpTheme.spacing.md)) {
                TextButton(onClick = onMarkAb) { Text(stringResource(R.string.video_ab_set)) }
                TextButton(onClick = onClearAb, enabled = abLoop.start != null) {
                    Text(stringResource(R.string.video_ab_clear))
                }
            }
            SectionLabel(stringResource(R.string.video_options_sleep))
            FlowRow(
                modifier = Modifier.padding(horizontal = KpTheme.spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm),
            ) {
                SleepTimerMinutes.forEach { minutes ->
                    FilterChip(
                        selected = minutes == sleepMinutes,
                        onClick = { onSleep(minutes) },
                        label = { Text(formatTimerMinutes(minutes)) },
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = OptionHeight)
                    .padding(horizontal = KpTheme.spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.video_mute),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = muted, onCheckedChange = onMute)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = KpTheme.spacing.lg).semantics { heading() },
    )
}
