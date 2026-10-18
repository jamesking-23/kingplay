package com.kingplay.feature.library.home

import androidx.annotation.StringRes
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.component.KpIconButton
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.ScanFailure
import com.kingplay.core.model.ScanPhase
import com.kingplay.core.model.ScanState
import com.kingplay.core.model.navigation.AppNavigator
import com.kingplay.feature.library.R
import com.kingplay.feature.library.list.AlbumsTab
import com.kingplay.feature.library.list.ArtistsTab
import com.kingplay.feature.library.list.FoldersTab
import com.kingplay.feature.library.list.GenresTab
import com.kingplay.feature.library.list.PlaylistsTab
import com.kingplay.feature.library.list.SongsTab
import kotlinx.coroutines.launch

private enum class LibraryTab(@StringRes val label: Int) {
    Songs(R.string.tab_songs),
    Albums(R.string.tab_albums),
    Artists(R.string.tab_artists),
    Genres(R.string.tab_genres),
    Folders(R.string.tab_folders),
    Playlists(R.string.tab_playlists),
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

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LibraryHeader(
                onSettings = navigator::toSettings,
                onRescan = { full -> viewModel.onEvent(LibraryHomeEvent.Rescan(full)) },
            )
            ScanBanner(state.scan)
            ScrollableTabRow(selectedTabIndex = pager.currentPage, edgePadding = KpTheme.spacing.sm) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pager.currentPage == index,
                        onClick = { scope.launch { pager.animateScrollToPage(index) } },
                        text = { Text(stringResource(tab.label)) },
                    )
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                when (tabs[page]) {
                    LibraryTab.Songs -> SongsTab(snackbar = snackbar)
                    LibraryTab.Albums -> AlbumsTab(navigator)
                    LibraryTab.Artists -> ArtistsTab(navigator)
                    LibraryTab.Genres -> GenresTab(navigator)
                    LibraryTab.Folders -> FoldersTab(navigator)
                    LibraryTab.Playlists -> PlaylistsTab(navigator)
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(onSettings: () -> Unit, onRescan: (full: Boolean) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(start = KpTheme.spacing.lg, end = KpTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.library_title), style = MaterialTheme.typography.headlineMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
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
