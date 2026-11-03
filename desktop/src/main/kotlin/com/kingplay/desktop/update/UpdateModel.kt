package com.kingplay.desktop.update

import com.kingplay.core.model.update.ReleaseInfo
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data object UpToDate : UpdateUi
    data object Failed : UpdateUi
    data object DownloadFailed : UpdateUi
    data class Available(val release: ReleaseInfo) : UpdateUi
    data class Downloading(val percent: Int) : UpdateUi

    /** Downloaded and kept: installing is one click, with no second download. */
    data class Ready(val release: ReleaseInfo, val file: File) : UpdateUi
}

/**
 * Lives as long as the program, not the Settings page, so a download goes on while the person uses other pages
 * and a finished one is remembered (also after closing KingPlay) until it is installed.
 */
object UpdateModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<UpdateUi>(
        Updater.restoreDownloaded()?.let { (release, file) -> UpdateUi.Ready(release, file) } ?: UpdateUi.Idle,
    )
    val state: StateFlow<UpdateUi> = _state.asStateFlow()

    fun check() {
        val now = _state.value
        if (now is UpdateUi.Checking || now is UpdateUi.Downloading || now is UpdateUi.Ready) return
        _state.value = UpdateUi.Checking
        scope.launch {
            _state.value = when (val r = Updater.check()) {
                Updater.Result.UpToDate -> UpdateUi.UpToDate
                Updater.Result.Failed -> UpdateUi.Failed
                is Updater.Result.Available -> UpdateUi.Available(r.release)
            }
        }
    }

    fun download(release: ReleaseInfo) {
        if (_state.value is UpdateUi.Downloading) return
        _state.value = UpdateUi.Downloading(0)
        scope.launch {
            val file = Updater.download(release) { _state.value = UpdateUi.Downloading(it) }
            _state.value = if (file == null) UpdateUi.DownloadFailed else UpdateUi.Ready(release, file)
        }
    }
}
