package com.kingplay.feature.settings

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.border
import androidx.compose.material3.Slider
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import kotlin.math.roundToInt
import com.kingplay.core.designsystem.component.Haptics
import com.kingplay.core.designsystem.theme.AccentChoices
import com.kingplay.core.designsystem.theme.family
import com.kingplay.feature.settings.update.UpdateSection
import com.kingplay.core.model.settings.AppFont
import com.kingplay.core.model.settings.HapticKind
import com.kingplay.core.model.settings.HapticLevel
import com.kingplay.core.model.settings.HapticSettings
import com.kingplay.core.model.settings.AppUiTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.component.KpIconButton
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.ScanState
import com.kingplay.core.model.navigation.AppNavigator
import com.kingplay.core.model.settings.AppAccent
import com.kingplay.core.model.settings.AppSettings
import com.kingplay.core.model.settings.AppThemeMode
import com.kingplay.core.ui.R as UiR
import com.kingplay.feature.settings.logic.backupFileName
import com.kingplay.feature.settings.logic.folderLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(navigator: AppNavigator, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
        if (uri != null) {
            viewModel.onEvent(
                SettingsEvent.Export { text -> withContext(Dispatchers.IO) { writeText(context, uri, text) } },
            )
        }
    }
    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.onEvent(SettingsEvent.Import { withContext(Dispatchers.IO) { readText(context, uri) } })
        }
    }
    val versionName = remember(context) { versionName(context) }

    SettingsContent(
        state = state,
        versionName = versionName,
        onEvent = viewModel::onEvent,
        onBack = navigator::back,
        onEqualizer = navigator::toEqualizer,
        onCreateBackup = { createBackup.launch(backupFileName(System.currentTimeMillis())) },
        onRestoreBackup = { pickBackup.launch(arrayOf("*/*")) },
    )
}

private const val BACKUP_MIME = "application/json"
private const val MAX_BACKUP_BYTES = 20_000_000

private fun writeText(context: Context, uri: Uri, text: String): Boolean =
    runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
    }.getOrDefault(false)

private fun readText(context: Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytes()
            if (bytes.size > MAX_BACKUP_BYTES) null else bytes.toString(Charsets.UTF_8)
        }
    }.getOrNull()

private fun versionName(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsState,
    versionName: String,
    onEvent: (SettingsEvent) -> Unit,
    onBack: () -> Unit,
    onEqualizer: () -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), maxLines = 1) },
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
            item { SectionTitle(R.string.settings_appearance) }
            item { AppearanceSection(state.settings, onEvent) }
            item { SectionTitle(R.string.settings_personalise) }
            item { PersonaliseSection(state.settings, onEvent) }
            item { SectionTitle(R.string.settings_haptics) }
            item { HapticsSection(state.settings.haptics, onEvent) }
            item { SectionTitle(R.string.settings_audio) }
            item { OutlinedButton(onClick = onEqualizer) { Text(stringResource(R.string.settings_equalizer)) } }
            item { SectionTitle(R.string.settings_library) }
            item { LibrarySection(state.scan, onEvent) }
            if (state.excludedFolders.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.settings_excluded),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = KpTheme.spacing.sm),
                    )
                }
                items(state.excludedFolders, key = { it }) { path ->
                    ExcludedRow(path) { onEvent(SettingsEvent.IncludeFolder(path)) }
                }
            }
            item { SectionTitle(R.string.settings_privacy) }
            item {
                SwitchRow(
                    title = stringResource(R.string.settings_analytics),
                    hint = stringResource(R.string.settings_analytics_hint),
                    checked = state.analyticsConsent,
                    onChange = { onEvent(SettingsEvent.SetAnalytics(it)) },
                )
            }
            item { SectionTitle(R.string.settings_backup) }
            item {
                BackupSection(
                    status = state.backup,
                    onCreate = onCreateBackup,
                    onRestore = onRestoreBackup,
                    onDismiss = { onEvent(SettingsEvent.DismissBackupStatus) },
                )
            }
            item { SectionTitle(R.string.settings_updates) }
            item { UpdateSection() }
            item { SectionTitle(R.string.settings_about) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
                    AboutSection(versionName)
                    Text(
                        text = stringResource(R.string.settings_about_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
private fun SwitchRow(
    title: String,
    hint: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val on = stringResource(R.string.settings_on)
    val off = stringResource(R.string.settings_off)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            modifier = Modifier.semantics { stateDescription = if (checked) on else off },
        )
    }
}

@Composable
private fun AppearanceSection(settings: AppSettings, onEvent: (SettingsEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
        Text(stringResource(R.string.settings_ui_theme), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.settings_ui_theme_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val designs = listOf(
            Triple(AppUiTheme.KING_ART, R.string.settings_ui_king_art, R.string.settings_ui_king_art_desc),
            Triple(AppUiTheme.KING_NEON, R.string.settings_ui_king_neon, R.string.settings_ui_king_neon_desc),
            Triple(AppUiTheme.KING_GLASS, R.string.settings_ui_king_glass, R.string.settings_ui_king_glass_desc),
            Triple(AppUiTheme.KING_CLASSIC, R.string.settings_ui_king_classic, R.string.settings_ui_king_classic_desc),
            Triple(AppUiTheme.KK_UI, R.string.settings_ui_kk, R.string.settings_ui_kk_desc),
        )
        designs.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
                pair.forEach { (theme, title, desc) ->
                    UiThemeCard(
                        title = stringResource(title),
                        description = stringResource(desc),
                        selected = settings.uiTheme == theme,
                        onClick = { onEvent(SettingsEvent.SetUiTheme(theme)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
            items(AppThemeMode.entries) { mode ->
                FilterChip(
                    selected = settings.themeMode == mode,
                    onClick = { onEvent(SettingsEvent.SetThemeMode(mode)) },
                    label = { Text(stringResource(modeLabel(mode))) },
                )
            }
        }
        if (settings.uiTheme != AppUiTheme.KK_UI) {
            Text(
                stringResource(R.string.settings_ui_accent_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (settings.uiTheme == AppUiTheme.KK_UI) {
            Text(stringResource(R.string.settings_accent), style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
                items(AppAccent.entries) { accent ->
                    val theme = accent.toTheme()
                    FilterChip(
                        selected = settings.accent == accent,
                        onClick = { onEvent(SettingsEvent.SetAccent(accent)) },
                        label = { Text(stringResource(theme.labelRes)) },
                        leadingIcon = {
                            Box(
                                Modifier.size(SWATCH_SIZE).clip(CircleShape).background(theme.swatch),
                            )
                        },
                    )
                }
            }
            val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            SwitchRow(
                title = stringResource(R.string.settings_dynamic),
                hint = if (dynamicSupported) null else stringResource(R.string.settings_dynamic_unavailable),
                checked = settings.dynamicColor && dynamicSupported,
                onChange = { onEvent(SettingsEvent.SetDynamicColor(it)) },
                enabled = dynamicSupported,
            )
        }
        SwitchRow(
            title = stringResource(R.string.settings_low_power),
            hint = stringResource(R.string.settings_low_power_hint),
            checked = settings.lowPowerVisuals,
            onChange = { onEvent(SettingsEvent.SetLowPower(it)) },
        )
    }
}

private val SWATCH_SIZE = 18.dp

/** One selectable design. Selection is announced by TalkBack and drawn as a thicker outline, not by colour alone. */
@Composable
private fun UiThemeCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.primaryContainer else colors.surfaceContainer,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.onSurface else colors.outlineVariant),
    ) {
        Column(Modifier.padding(KpTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.xs)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun modeLabel(mode: AppThemeMode): Int = when (mode) {
    AppThemeMode.SYSTEM -> R.string.settings_theme_system
    AppThemeMode.LIGHT -> R.string.settings_theme_light
    AppThemeMode.DARK -> R.string.settings_theme_dark
    AppThemeMode.AMOLED -> R.string.settings_theme_amoled
}

@Composable
private fun LibrarySection(scan: ScanState, onEvent: (SettingsEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
        val status = when (scan) {
            ScanState.Idle -> stringResource(R.string.settings_scan_idle)
            is ScanState.Scanning -> stringResource(R.string.settings_scan_running, scan.done, scan.total)
            is ScanState.Failed -> stringResource(R.string.settings_scan_failed)
        }
        Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val idle = scan !is ScanState.Scanning
        Row(horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
            Button(onClick = { onEvent(SettingsEvent.Rescan(full = false)) }, enabled = idle) {
                Text(stringResource(R.string.settings_scan_now))
            }
            OutlinedButton(onClick = { onEvent(SettingsEvent.Rescan(full = true)) }, enabled = idle) {
                Text(stringResource(R.string.settings_scan_full))
            }
        }
    }
}

@Composable
private fun ExcludedRow(path: String, onInclude: () -> Unit) {
    val label = folderLabel(path)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(
                text = path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        TextButton(onClick = onInclude) { Text(stringResource(R.string.settings_include_again, label)) }
    }
}

@Composable
private fun BackupSection(
    status: BackupStatus,
    onCreate: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
        Text(
            text = stringResource(R.string.settings_backup_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val busy = status == BackupStatus.Working
        Row(horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
            Button(onClick = onCreate, enabled = !busy) { Text(stringResource(R.string.settings_backup_create)) }
            OutlinedButton(onClick = onRestore, enabled = !busy) {
                Text(stringResource(R.string.settings_backup_restore))
            }
        }
        val message = statusMessage(status)
        if (message != null) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (!busy) TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_backup_dismiss)) }
        }
    }
}

@Composable
private fun statusMessage(status: BackupStatus): String? = when (status) {
    BackupStatus.Idle -> null
    BackupStatus.Working -> stringResource(R.string.settings_backup_working)
    BackupStatus.Exported -> stringResource(R.string.settings_backup_exported)
    BackupStatus.ExportFailed -> stringResource(R.string.settings_backup_export_failed)
    BackupStatus.RestoreFailed -> stringResource(R.string.settings_restore_failed)
    is BackupStatus.Invalid -> stringResource(R.string.settings_restore_invalid, status.reason)
    is BackupStatus.Restored -> {
        val s = status.summary
        val done = stringResource(
            R.string.settings_restore_done,
            s.playlistsAdded,
            s.favoritesMatched,
            s.presetsAdded,
            s.lyricsAdded,
            s.tracksMatched,
            s.tracksTotal,
        )
        if (s.playlistsSkipped > 0) {
            done + " " + stringResource(R.string.settings_restore_skipped, s.playlistsSkipped)
        } else {
            done
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonaliseSection(settings: AppSettings, onEvent: (SettingsEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
        Text(stringResource(R.string.settings_font), style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.xs),
        ) {
            AppFont.entries.forEach { font ->
                FilterChip(
                    selected = settings.font == font,
                    onClick = { onEvent(SettingsEvent.SetFont(font)) },
                    label = { Text(stringResource(fontLabel(font)), fontFamily = font.family()) },
                )
            }
        }
        Text(
            stringResource(R.string.settings_font_sample),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.settings_text_size, (settings.textScale * PERCENT).toInt()),
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = settings.textScale,
            onValueChange = { onEvent(SettingsEvent.SetTextScale((it * SCALE_STEPS).roundToInt() / SCALE_STEPS)) },
            valueRange = AppSettings.MIN_TEXT_SCALE..AppSettings.MAX_TEXT_SCALE,
        )
        Text(stringResource(R.string.settings_colour), style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm),
        ) {
            FilterChip(
                selected = settings.customAccent == null,
                onClick = { onEvent(SettingsEvent.SetCustomAccent(null)) },
                label = { Text(stringResource(R.string.settings_colour_default)) },
            )
            AccentChoices.forEach { argb ->
                val picked = settings.customAccent == argb
                Box(
                    Modifier
                        .size(SWATCH_BUTTON)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color(argb.toInt()))
                        .border(
                            if (picked) 3.dp else 1.dp,
                            if (picked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        )
                        .selectable(selected = picked, role = Role.RadioButton) {
                            onEvent(SettingsEvent.SetCustomAccent(argb))
                        }
                        .semantics { contentDescription = "Colour #%06X".format(argb and 0xFFFFFF) },
                )
            }
        }
    }
}

private const val PERCENT = 100
private const val SCALE_STEPS = 20f
private val SWATCH_BUTTON = 40.dp

private fun fontLabel(font: AppFont): Int = when (font) {
    AppFont.DEFAULT -> R.string.font_default
    AppFont.POPPINS -> R.string.font_poppins
    AppFont.HANKEN -> R.string.font_hanken
    AppFont.SOURCE_SANS -> R.string.font_source_sans
    AppFont.SOURCE_SERIF -> R.string.font_source_serif
    AppFont.LORA -> R.string.font_lora
    AppFont.FREDOKA -> R.string.font_fredoka
    AppFont.HANDWRITTEN -> R.string.font_handwritten
    AppFont.SKETCH -> R.string.font_sketch
    AppFont.MONO -> R.string.font_mono
    AppFont.READABLE -> R.string.font_readable
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun HapticsSection(haptics: HapticSettings, onEvent: (SettingsEvent) -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
        SwitchRow(
            title = stringResource(R.string.settings_haptics_on),
            hint = stringResource(R.string.settings_haptics_hint),
            checked = haptics.enabled,
            onChange = { on -> onEvent(SettingsEvent.SetHaptics { it.copy(enabled = on) }) },
        )
        if (haptics.enabled) {
            HapticRow(R.string.haptic_play, haptics.play, HapticKind.PLAY, context) { level ->
                onEvent(SettingsEvent.SetHaptics { it.copy(play = level) })
            }
            HapticRow(R.string.haptic_transport, haptics.transport, HapticKind.TRANSPORT, context) { level ->
                onEvent(SettingsEvent.SetHaptics { it.copy(transport = level) })
            }
            HapticRow(R.string.haptic_toggle, haptics.toggle, HapticKind.TOGGLE, context) { level ->
                onEvent(SettingsEvent.SetHaptics { it.copy(toggle = level) })
            }
            HapticRow(R.string.haptic_tap, haptics.tap, HapticKind.TAP, context) { level ->
                onEvent(SettingsEvent.SetHaptics { it.copy(tap = level) })
            }
            Text(
                stringResource(R.string.settings_haptics_try),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun HapticRow(
    label: Int,
    level: HapticLevel,
    kind: HapticKind,
    context: Context,
    onLevel: (HapticLevel) -> Unit,
) {
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
            HapticLevel.entries.forEach { option ->
                FilterChip(
                    selected = level == option,
                    onClick = {
                        onLevel(option)
                        // Picking a level plays it right away, so the choice can be felt.
                        Haptics.play(context, option, kind)
                    },
                    label = { Text(stringResource(levelLabel(option))) },
                )
            }
        }
    }
}

private fun levelLabel(level: HapticLevel): Int = when (level) {
    HapticLevel.OFF -> R.string.haptic_off
    HapticLevel.LIGHT -> R.string.haptic_light
    HapticLevel.MEDIUM -> R.string.haptic_medium
    HapticLevel.STRONG -> R.string.haptic_strong
}
