package com.kingplay.feature.settings.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.component.KpButton
import com.kingplay.core.designsystem.component.KpButtonStyle
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.feature.settings.R

/** "Check for updates": finds a newer build, downloads it and hands it to Android's installer. */
@Composable
fun UpdateSection(modifier: Modifier = Modifier, viewModel: UpdateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
        Text(
            stringResource(R.string.update_installed, viewModel.installed.tag),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm),
        ) {
            when (val s = state) {
                UpdateState.Idle -> Unit
                UpdateState.Checking -> Message(R.string.update_checking)
                UpdateState.UpToDate -> Message(R.string.update_up_to_date)
                is UpdateState.Available -> Message(R.string.update_available, s.release.tag)
                is UpdateState.Downloading -> {
                    Message(R.string.update_downloading, s.percent)
                    LinearProgressIndicator(progress = { s.percent / PERCENT }, modifier = Modifier.fillMaxWidth())
                }
                is UpdateState.Installing -> Message(R.string.update_installing)
                is UpdateState.NeedsPermission -> Message(R.string.update_needs_permission)
                is UpdateState.Failed -> Message(
                    when (s.reason) {
                        UpdateState.Reason.OFFLINE -> R.string.update_failed_offline
                        UpdateState.Reason.NO_RELEASE -> R.string.update_failed_none
                        UpdateState.Reason.DOWNLOAD -> R.string.update_failed_download
                        UpdateState.Reason.WRONG_FILE -> R.string.update_failed_file
                    },
                    isError = true,
                )
            }
        }
        when (val s = state) {
            is UpdateState.Available -> {
                if (s.release.notes.isNotBlank()) {
                    Text(
                        s.release.notes.take(NOTES_MAX),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                KpButton(stringResource(R.string.update_install), onClick = { viewModel.install(s.release) })
            }
            is UpdateState.NeedsPermission -> {
                KpButton(stringResource(R.string.update_open_permission), onClick = viewModel::openPermissionSettings)
                KpButton(
                    stringResource(R.string.update_try_again),
                    onClick = { viewModel.install(s.release) },
                    style = KpButtonStyle.Tonal,
                )
            }
            is UpdateState.Downloading, UpdateState.Checking -> Unit
            else -> KpButton(
                stringResource(R.string.update_check),
                onClick = viewModel::check,
                style = KpButtonStyle.Tonal,
            )
        }
        Text(
            stringResource(R.string.update_or_manual),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = KpTheme.spacing.xs),
        )
    }
}

@Composable
private fun Message(res: Int, vararg args: Any, isError: Boolean = false) {
    Text(
        stringResource(res, *args),
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
}

private const val PERCENT = 100f
private const val NOTES_MAX = 600
