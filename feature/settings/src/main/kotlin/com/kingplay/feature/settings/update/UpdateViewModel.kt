package com.kingplay.feature.settings.update

import androidx.lifecycle.ViewModel
import com.kingplay.core.model.update.InstalledVersion
import com.kingplay.core.model.update.ReleaseInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class Downloading(val release: ReleaseInfo, val percent: Int) : UpdateState

    /** The file is downloaded and checked; nothing is installed until the person taps Install. */
    data class Ready(val release: ReleaseInfo) : UpdateState

    /** The system installer has been opened. */
    data class Installing(val release: ReleaseInfo) : UpdateState

    /** Android needs the person to allow KingPlay to install apps first. */
    data class NeedsPermission(val release: ReleaseInfo) : UpdateState
    data class Failed(val reason: Reason) : UpdateState

    enum class Reason { OFFLINE, NO_RELEASE, DOWNLOAD, WRONG_FILE }
}

/** A thin window onto [UpdateManager], which keeps working when this screen is closed. */
@HiltViewModel
class UpdateViewModel @Inject constructor(private val manager: UpdateManager) : ViewModel() {
    val installed: InstalledVersion = manager.installed
    val state: StateFlow<UpdateState> = manager.state

    fun check() = manager.check()

    fun install(release: ReleaseInfo) = manager.download(release)

    fun installReady() = manager.installReady()

    fun openPermissionSettings() = manager.openPermissionSettings()

    fun permissionGranted() = manager.permissionGranted()
}
