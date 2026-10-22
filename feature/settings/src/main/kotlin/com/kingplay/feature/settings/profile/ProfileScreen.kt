package com.kingplay.feature.settings.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.component.KpButton
import com.kingplay.core.designsystem.component.KpIconButton
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.navigation.AppNavigator
import com.kingplay.core.model.settings.AppSettings
import com.kingplay.core.ui.component.ProfileAvatar
import com.kingplay.core.ui.R as UiR
import com.kingplay.feature.settings.R
import java.text.DateFormat
import java.util.Date

/** Who you are in KingPlay, plus the lifetime numbers: songs played, videos watched and time spent listening. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navigator: AppNavigator, modifier: Modifier = Modifier, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    LaunchedEffect(state.nickname) { if (name.isEmpty()) name = state.nickname }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setPhoto(uri)
    }
    Scaffold(
        modifier = modifier,
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    KpIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(UiR.string.ui_back),
                        onClick = navigator::back,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(KpTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.md),
        ) {
            ProfileAvatar(photoPath = state.photoPath, nickname = state.nickname, size = 112.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
                TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text(stringResource(if (state.photoPath == null) R.string.profile_add_photo else R.string.profile_change_photo))
                }
                if (state.photoPath != null) {
                    TextButton(onClick = viewModel::removePhoto) { Text(stringResource(R.string.profile_remove_photo)) }
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(AppSettings.MAX_NICKNAME) },
                label = { Text(stringResource(R.string.profile_nickname)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            KpButton(
                text = stringResource(R.string.profile_save),
                onClick = { viewModel.saveNickname(name) },
                enabled = name.isNotBlank() && name.trim() != state.nickname,
            )
            Text(
                stringResource(R.string.profile_records),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(top = KpTheme.spacing.md).semantics { heading() },
            )
            val r = state.records
            Stat(stringResource(R.string.profile_songs_played), r.songPlays.toString())
            Stat(stringResource(R.string.profile_videos_played), r.videoPlays.toString())
            Stat(stringResource(R.string.profile_listening_time), formatListening(r.totalListenMs))
            if (r.audioListenMs > 0 || r.videoListenMs > 0) {
                Text(
                    stringResource(R.string.profile_listening_split, formatListening(r.audioListenMs), formatListening(r.videoListenMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (r.installedAtMs > 0) {
                Text(
                    stringResource(R.string.profile_since, DateFormat.getDateInstance(DateFormat.LONG).format(Date(r.installedAtMs))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(R.string.profile_records_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(KpTheme.spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/** "3 h 20 min", "45 min", or "under a minute". */
internal fun formatListening(ms: Long): String {
    val minutes = ms / MS_PER_MINUTE
    val hours = minutes / MINUTES_PER_HOUR
    return when {
        minutes < 1 -> "under a minute"
        hours < 1 -> "$minutes min"
        hours < HOURS_PER_DAY -> "$hours h ${minutes % MINUTES_PER_HOUR} min"
        else -> "${hours / HOURS_PER_DAY} d ${hours % HOURS_PER_DAY} h"
    }
}

private const val MS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60L
private const val HOURS_PER_DAY = 24L
