package com.kingplay.app

import androidx.lifecycle.ViewModel
import com.kingplay.core.model.repository.LibraryScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** App-level actions that do not belong to any one screen. */
@HiltViewModel
class AppViewModel @Inject constructor(private val scanner: LibraryScanner) : ViewModel() {

    /** Media access was just granted: set up observers and scan now. Safe to call more than once. */
    fun onMediaAccessGranted() = scanner.start()
}
