package com.kingplay.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingplay.core.designsystem.component.KpButton
import com.kingplay.core.designsystem.component.KpButtonStyle
import com.kingplay.core.designsystem.component.KpEmptyState
import com.kingplay.core.designsystem.component.KpErrorState
import com.kingplay.core.designsystem.component.KpIconButton
import com.kingplay.core.designsystem.component.KpSkeletonList
import com.kingplay.core.designsystem.theme.AccentTheme
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.designsystem.theme.ThemeConfig
import com.kingplay.core.designsystem.theme.ThemeMode
import com.kingplay.core.ui.ContentLayout

/** Temporary home screen: a live gallery of the design system, replaced by :feature:home in step 2d. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DesignSystemGalleryScreen(
    config: ThemeConfig,
    layout: ContentLayout,
    onModeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentTheme) -> Unit,
    onDynamicChange: (Boolean) -> Unit,
    onLowPowerChange: (Boolean) -> Unit,
) {
    val spacing = KpTheme.spacing
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .then(if (layout == ContentLayout.COMPACT) Modifier.fillMaxWidth() else Modifier.widthIn(max = 640.dp))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                Text(stringResource(R.string.gallery_title), style = MaterialTheme.typography.headlineMedium)

                Text(stringResource(R.string.gallery_theme_mode), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = config.mode == mode,
                            onClick = { onModeChange(mode) },
                            label = { Text(stringResource(modeLabel(mode))) },
                        )
                    }
                }

                Text(stringResource(R.string.gallery_accent), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    AccentTheme.entries.forEach { accent ->
                        FilterChip(
                            selected = config.accent == accent,
                            onClick = { onAccentChange(accent) },
                            enabled = !config.useDynamicColor,
                            label = { Text(stringResource(accent.labelRes)) },
                        )
                    }
                }

                SwitchRow(stringResource(R.string.gallery_dynamic), config.useDynamicColor, onDynamicChange)
                SwitchRow(stringResource(R.string.gallery_low_power), config.lowPowerVisuals, onLowPowerChange)

                Text(stringResource(R.string.gallery_buttons), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    KpButton(stringResource(R.string.gallery_filled), onClick = {}, leadingIcon = Icons.Default.PlayArrow)
                    KpButton(stringResource(R.string.gallery_tonal), onClick = {}, style = KpButtonStyle.Tonal)
                    KpButton(stringResource(R.string.gallery_outlined), onClick = {}, style = KpButtonStyle.Outlined)
                    KpButton(stringResource(R.string.gallery_text), onClick = {}, style = KpButtonStyle.Text)
                    KpIconButton(Icons.Default.Search, stringResource(R.string.gallery_search), onClick = {})
                }

                Text(stringResource(R.string.gallery_states), style = MaterialTheme.typography.titleMedium)
                KpSkeletonList(rows = 3)
                Box(Modifier.height(220.dp)) {
                    KpEmptyState(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.gallery_empty_title),
                        message = stringResource(R.string.gallery_empty_message),
                    )
                }
                Box(Modifier.height(220.dp)) {
                    KpErrorState(onRetry = {}, message = stringResource(R.string.gallery_error_message))
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun modeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.mode_system
    ThemeMode.LIGHT -> R.string.mode_light
    ThemeMode.DARK -> R.string.mode_dark
    ThemeMode.AMOLED -> R.string.mode_amoled
}
