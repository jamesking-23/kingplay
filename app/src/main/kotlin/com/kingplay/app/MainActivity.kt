package com.kingplay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingplay.core.designsystem.component.KpButton
import com.kingplay.core.designsystem.component.KpButtonStyle
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
            var showLibrary by rememberSaveable { mutableStateOf(true) }
            val config = ThemeConfig(
                mode = mode,
                useDynamicColor = dynamic,
                accent = accent,
                lowPowerVisuals = lowPower,
            )

            KingPlayTheme(config) {
                // Temporary dev scaffold: switch between the step 2b library status and the design gallery.
                Box {
                    if (showLibrary) {
                        LibraryStatusScreen()
                    } else {
                        DesignSystemGalleryScreen(
                            config = config,
                            layout = calculateWindowSizeClass(this@MainActivity).toContentLayout(),
                            onModeChange = { mode = it },
                            onAccentChange = { accent = it },
                            onDynamicChange = { dynamic = it },
                            onLowPowerChange = { lowPower = it },
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(8.dp),
                    ) {
                        KpButton(
                            text = stringResource(R.string.tab_library),
                            onClick = { showLibrary = true },
                            style = if (showLibrary) KpButtonStyle.Filled else KpButtonStyle.Outlined,
                        )
                        KpButton(
                            text = stringResource(R.string.tab_design),
                            onClick = { showLibrary = false },
                            style = if (showLibrary) KpButtonStyle.Outlined else KpButtonStyle.Filled,
                        )
                    }
                }
            }
        }
    }
}
