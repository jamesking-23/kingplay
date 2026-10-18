package com.kingplay.feature.nowplaying.stage

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.sleep.SleepTimerSpec
import com.kingplay.core.model.sleep.SleepTimerState
import com.kingplay.core.model.sleep.remainingMs
import com.kingplay.feature.nowplaying.R
import com.kingplay.feature.nowplaying.logic.SleepExtendMinutes
import com.kingplay.feature.nowplaying.logic.SleepPresetMinutes
import com.kingplay.feature.nowplaying.logic.formatRemaining
import kotlinx.coroutines.delay

private const val TICK_MS = 1_000L
private const val SHORT_TRACK_COUNT = 3

/** Sleep timer choices. While a timer runs, shows what is left and offers +10 minutes or cancel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(onDismiss: () -> Unit, viewModel: SleepTimerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val timer = state.timer
    fun choose(spec: SleepTimerSpec) {
        viewModel.onEvent(SleepEvent.Start(spec))
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KpTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.np_sleep_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            if (timer !is SleepTimerState.Off) {
                ActiveTimer(timer, onExtend = { viewModel.onEvent(SleepEvent.Extend(SleepExtendMinutes)) }) {
                    viewModel.onEvent(SleepEvent.Cancel)
                    onDismiss()
                }
            }
            SleepPresetMinutes.forEach { minutes ->
                SleepOption(
                    text = pluralStringResource(R.plurals.np_sleep_minutes, minutes, minutes),
                    onClick = { choose(SleepTimerSpec.Minutes(minutes)) },
                )
            }
            SleepOption(stringResource(R.string.np_sleep_end_of_song)) { choose(SleepTimerSpec.Tracks(1)) }
            SleepOption(
                text = pluralStringResource(R.plurals.np_sleep_songs, SHORT_TRACK_COUNT, SHORT_TRACK_COUNT),
                onClick = { choose(SleepTimerSpec.Tracks(SHORT_TRACK_COUNT)) },
            )
            Text(
                text = stringResource(R.string.np_sleep_fade_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = KpTheme.spacing.md),
            )
        }
    }
}

@Composable
private fun ActiveTimer(timer: SleepTimerState, onExtend: () -> Unit, onCancel: () -> Unit) {
    val summary = when (timer) {
        is SleepTimerState.Timed -> {
            val left by produceState(initialValue = remainingMs(timer, SystemClock.elapsedRealtime()) ?: 0L, timer) {
                while (true) {
                    value = remainingMs(timer, SystemClock.elapsedRealtime()) ?: 0L
                    delay(TICK_MS)
                }
            }
            stringResource(R.string.np_sleep_remaining, formatRemaining(left))
        }
        is SleepTimerState.AfterTracks ->
            pluralStringResource(R.plurals.np_sleep_songs_left, timer.remaining, timer.remaining)
        SleepTimerState.Off -> ""
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(summary, style = MaterialTheme.typography.titleMedium)
        Row {
            if (timer is SleepTimerState.Timed) {
                TextButton(onClick = onExtend) {
                    Text(stringResource(R.string.np_sleep_extend, SleepExtendMinutes))
                }
            }
            TextButton(onClick = onCancel) { Text(stringResource(R.string.np_sleep_cancel)) }
        }
    }
}

@Composable
private fun SleepOption(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
    }
}
