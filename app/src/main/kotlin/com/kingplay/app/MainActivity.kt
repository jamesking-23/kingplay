package com.kingplay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.theme.KingPlayTheme
import com.kingplay.feature.settings.profile.WelcomeScreen
import com.kingplay.feature.settings.toThemeConfig
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()
    private val coverViewModel: CoverColorViewModel by viewModels()

    private fun debugStartRoute(): String? = if (BuildConfig.DEBUG) intent?.getStringExtra("kp_route") else null

    private fun debugStyle(): com.kingplay.core.designsystem.theme.UiStyle? = if (BuildConfig.DEBUG) {
        intent?.getStringExtra("kp_ui")?.let { runCatching { com.kingplay.core.designsystem.theme.UiStyle.valueOf(it) }.getOrNull() }
    } else {
        null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by appViewModel.settings.collectAsStateWithLifecycle()
            val cover by coverViewModel.color.collectAsStateWithLifecycle()
            settings?.let { saved ->
                KingPlayTheme(
                    saved.toThemeConfig()
                        .let { if (saved.coverTheme) it.copy(coverColor = cover) else it }
                        .let { c -> debugStyle()?.let { c.copy(uiStyle = it) } ?: c },
                ) {
                    if (saved.nickname.isBlank()) {
                        WelcomeScreen()
                    } else {
                        PermissionGate { KingPlayRoot(startRoute = debugStartRoute()) }
                    }
                }
            }
        }
    }
}
