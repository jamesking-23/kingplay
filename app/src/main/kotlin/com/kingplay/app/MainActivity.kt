package com.kingplay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.theme.KingPlayTheme
import com.kingplay.feature.settings.toThemeConfig
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by appViewModel.settings.collectAsStateWithLifecycle()
            settings?.let { saved ->
                KingPlayTheme(saved.toThemeConfig()) {
                    PermissionGate { KingPlayRoot() }
                }
            }
        }
    }
}
