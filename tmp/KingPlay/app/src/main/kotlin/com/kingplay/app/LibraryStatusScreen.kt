package com.kingplay.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.component.KpButton
import com.kingplay.core.designsystem.component.KpButtonStyle
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.ScanFailure
import com.kingplay.core.model.ScanPhase
import com.kingplay.core.model.ScanState
import com.kingplay.data.library.platform.MediaPermissions

/** Temporary home for step 2b: permission, scan progress, counts and the newest tracks. Replaced in step 2d. */
@Composable
fun LibraryStatusScreen(viewModel: LibraryStatusViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(MediaPermissions.hasAny(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasPermission = MediaPermissions.hasAny(context)
        viewModel.onEvent(LibraryStatusEvent.PermissionResult)
    }
    val spacing = KpTheme.spacing

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(stringResource(R.string.library_title), style = MaterialTheme.typography.headlineMedium)
            Text(scanText(state.scan), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(
                    R.string.library_counts,
                    state.songs,
                    state.albums,
                    state.artists,
                    state.videos,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                if (!hasPermission) {
                    KpButton(
                        text = stringResource(R.string.library_grant),
                        onClick = { launcher.launch(MediaPermissions.required()) },
                    )
                } else {
                    KpButton(
                        text = stringResource(R.string.library_rescan),
                        onClick = { viewModel.onEvent(LibraryStatusEvent.Rescan) },
                        style = KpButtonStyle.Tonal,
                    )
                    KpButton(
                        text = stringResource(R.string.library_full_rescan),
                        onClick = { viewModel.onEvent(LibraryStatusEvent.FullRescan) },
                        style = KpButtonStyle.Outlined,
                    )
                }
            }
            Text(stringResource(R.string.library_recent), style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                items(state.recent, key = { it.id }) { track ->
                    Column {
                        Text(track.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        Text(
                            "${track.artistName} · ${track.format}" +
                                if (track.bitrateKbps > 0) " · ${track.bitrateKbps} kbps" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun scanText(scan: ScanState): String = when (scan) {
    ScanState.Idle -> stringResource(R.string.library_idle)
    is ScanState.Failed -> when (scan.reason) {
        ScanFailure.PERMISSION_DENIED -> stringResource(R.string.library_permission_needed)
        ScanFailure.UNKNOWN -> stringResource(R.string.library_failed)
    }
    is ScanState.Scanning -> when (scan.phase) {
        ScanPhase.INDEXING_AUDIO -> stringResource(R.string.library_scanning_audio, scan.done, scan.total)
        ScanPhase.INDEXING_VIDEO -> stringResource(R.string.library_scanning_video, scan.done, scan.total)
        ScanPhase.READING_DETAILS -> stringResource(R.string.library_reading_details, scan.done, scan.total)
    }
}
