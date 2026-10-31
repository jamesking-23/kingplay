package com.kingplay.feature.library.home

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingplay.core.ui.component.ProfileAvatar
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.component.KpIconButton
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.designsystem.theme.UiStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import com.kingplay.core.model.ScanFailure
import com.kingplay.core.model.ScanPhase
import com.kingplay.core.model.ScanState
import com.kingplay.core.model.navigation.AppNavigator
import com.kingplay.feature.library.R
import com.kingplay.feature.library.report.ReportBannerViewModel
import com.kingplay.feature.library.list.AlbumsTab
import com.kingplay.feature.library.list.ArtistsTab
import com.kingplay.feature.library.list.FoldersTab
import com.kingplay.feature.library.list.GenresTab
import com.kingplay.feature.library.list.PlaylistsTab
import com.kingplay.feature.library.list.SongsTab
import com.kingplay.feature.library.list.VideosTab
import kotlinx.coroutines.launch

private enum class LibraryTab(@StringRes val label: Int) {
    Songs(R.string.tab_songs),
    Playlists(R.string.tab_playlists),
    Albums(R.string.tab_albums),
    Artists(R.string.tab_artists),
    Genres(R.string.tab_genres),
    Folders(R.string.tab_folders),
    Videos(R.string.tab_videos),
}

/** The home screen: scan status, then one swipeable tab per way of browsing the library. */
@Composable
fun LibraryScreen(
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: LibraryHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tabs = LibraryTab.entries
    val pager = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var searching by rememberSaveable { mutableStateOf(false) }
    AskNotificationPermissionOnce()

    Scaffold(
        modifier = modifier,
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (searching) {
            SearchPanel(
                navigator = navigator,
                onClose = { searching = false },
                modifier = Modifier.padding(padding).statusBarsPadding(),
            )
            return@Scaffold
        }
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LibraryHeader(
                onSearch = { searching = true },
                onSettings = navigator::toSettings,
                onProfile = navigator::toProfile,
                onRescan = { full -> viewModel.onEvent(LibraryHomeEvent.Rescan(full)) },
            )
            ScanBanner(state.scan)
            val reportBanner: ReportBannerViewModel = hiltViewModel()
            val lastMonth by reportBanner.lastMonth.collectAsStateWithLifecycle()
            lastMonth?.let { month ->
                TextButton(
                    onClick = { navigator.toReport(month) },
                    modifier = Modifier.padding(horizontal = KpTheme.spacing.sm),
                ) { Text("Your listening report for last month is ready →") }
            }
            LibraryTabs(
                labels = tabs.map { stringResource(it.label) },
                selected = pager.currentPage,
                onSelect = { scope.launch { pager.animateScrollToPage(it) } },
            )
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                when (tabs[page]) {
                    LibraryTab.Songs -> SongsTab(snackbar = snackbar)
                    LibraryTab.Albums -> AlbumsTab(navigator)
                    LibraryTab.Artists -> ArtistsTab(navigator)
                    LibraryTab.Genres -> GenresTab(navigator)
                    LibraryTab.Folders -> FoldersTab(navigator)
                    LibraryTab.Playlists -> PlaylistsTab(navigator)
                    LibraryTab.Videos -> VideosTab(navigator)
                }
            }
        }
    }
}

/** Pills, an underline, bracketed text or plain tabs, depending on the design. */
@Composable
private fun LibraryTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val style = KpTheme.uiStyle
    val scheme = MaterialTheme.colorScheme
    if (style == UiStyle.KING_NEON || style == UiStyle.KING_GLASS || style == UiStyle.KK_UI) {
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = KpTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = KpTheme.spacing.xs),
        ) {
            items(labels.size) { i ->
                val on = i == selected
                val shape = if (style == UiStyle.KING_NEON) {
                    androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                } else {
                    androidx.compose.foundation.shape.RoundedCornerShape(50)
                }
                Text(
                    text = labels[i],
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) scheme.onPrimary else scheme.onSurface,
                    modifier = Modifier
                        .clip(shape)
                        .background(if (on) scheme.primary else scheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable(role = androidx.compose.ui.semantics.Role.Tab) { onSelect(i) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    } else {
        ScrollableTabRow(
            selectedTabIndex = selected,
            edgePadding = KpTheme.spacing.sm,
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
        ) {
            labels.forEachIndexed { index, label ->
                Tab(selected = selected == index, onClick = { onSelect(index) }, text = { Text(label) })
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onRescan: (full: Boolean) -> Unit,
    greeting: GreetingViewModel = hiltViewModel(),
) {
    var menuOpen by remember { mutableStateOf(false) }
    val who by greeting.state.collectAsStateWithLifecycle()
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(start = KpTheme.spacing.lg, end = KpTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (who.nickname.isNotBlank()) {
                Text(
                    stringResource(greetingRes(), who.nickname),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(stringResource(R.string.library_title), style = MaterialTheme.typography.headlineMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clickable(onClickLabel = stringResource(R.string.library_profile), onClick = onProfile)
                    .padding(KpTheme.spacing.sm),
            ) {
                ProfileAvatar(photoPath = who.photoPath, nickname = who.nickname, size = 32.dp)
            }
            KpIconButton(
                icon = Icons.Default.Search,
                contentDescription = stringResource(R.string.search_open),
                onClick = onSearch,
            )
            KpIconButton(
                icon = Icons.Default.Settings,
                contentDescription = stringResource(R.string.library_settings),
                onClick = onSettings,
            )
            Box {
                KpIconButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.library_more),
                    onClick = { menuOpen = true },
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_rescan)) },
                        onClick = {
                            menuOpen = false
                            onRescan(false)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_full_rescan)) },
                        onClick = {
                            menuOpen = false
                            onRescan(true)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanBanner(scan: ScanState) {
    when (scan) {
        ScanState.Idle -> Unit
        is ScanState.Failed -> Text(
            text = stringResource(
                if (scan.reason == ScanFailure.PERMISSION_DENIED) {
                    R.string.scan_permission_needed
                } else {
                    R.string.scan_failed
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = KpTheme.spacing.lg, vertical = KpTheme.spacing.sm),
        )
        is ScanState.Scanning -> Column(
            modifier = Modifier.padding(horizontal = KpTheme.spacing.lg, vertical = KpTheme.spacing.sm),
        ) {
            val label = when (scan.phase) {
                ScanPhase.INDEXING_AUDIO -> R.string.scan_audio
                ScanPhase.INDEXING_VIDEO -> R.string.scan_video
                ScanPhase.READING_DETAILS -> R.string.scan_details
            }
            Text(
                text = stringResource(label, scan.done, scan.total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(progress = { scan.fraction }, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Android 13+ needs a runtime grant before the "your report is ready" notification can appear. Asked once per launch. */
@Composable
private fun AskNotificationPermissionOnce() {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) runCatching { launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
    }
}

@androidx.annotation.StringRes
private fun greetingRes(): Int {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < NOON -> R.string.greeting_morning
        hour < EVENING -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
}

private const val NOON = 12
private const val EVENING = 17
