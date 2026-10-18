package com.kingplay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kingplay.core.designsystem.theme.AccentTheme
import com.kingplay.core.designsystem.theme.KingPlayTheme
import com.kingplay.core.designsystem.theme.ThemeConfig
import com.kingplay.core.designsystem.theme.ThemeMode
import com.kingplay.core.ui.toContentLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // Until the settings step lands, theme choices live here so the design system can be exercised.
            var mode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
            var accent by rememberSaveable { mutableStateOf(AccentTheme.VIOLET) }
            var dynamic by rememberSaveable { mutableStateOf(true) }
            var lowPower by rememberSaveable { mutableStateOf(false) }
            val config = ThemeConfig(
                mode = mode,
                useDynamicColor = dynamic,
                accent = accent,
                lowPowerVisuals = lowPower,
            )

            KingPlayTheme(config) {
                DesignSystemGalleryScreen(
                    config = config,
                    layout = calculateWindowSizeClass(this).toContentLayout(),
                    onModeChange = { mode = it },
                    onAccentChange = { accent = it },
                    onDynamicChange = { dynamic = it },
                    onLowPowerChange = { lowPower = it },
                )
            }
        }
    }
}
