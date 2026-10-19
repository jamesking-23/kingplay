package com.kingplay.app

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingplay.core.designsystem.component.KpButton
import com.kingplay.core.designsystem.component.KpButtonStyle
import com.kingplay.core.designsystem.theme.KingPlayTheme
import com.kingplay.core.designsystem.theme.ThemeConfig
import com.kingplay.core.designsystem.theme.ThemeMode
import com.kingplay.core.designsystem.theme.UiStyle
import com.kingplay.core.ui.component.MediaRow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Renders a sample screen in every design, light and dark, and sends the pictures out through logcat. */
@RunWith(AndroidJUnit4::class)
class DesignGalleryTest {
    @get:Rule
    val rule = createComposeRule()

    private fun send(name: String) {
        val full = rule.onRoot().captureToImage().asAndroidBitmap()
        val bmp = Bitmap.createScaledBitmap(full, full.width / 2, full.height / 2, true)
        val bytes = java.io.ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 72, it) }.toByteArray()
        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        val chunks = b64.chunked(3500)
        chunks.forEachIndexed { i, c -> android.util.Log.i("KPSHOT", "$name ${i + 1} ${chunks.size} $c") }
    }

    @Test
    fun gallery() {
        val styles = listOf(
            UiStyle.KING_ART to "art", UiStyle.KING_NEON to "neon", UiStyle.KING_GLASS to "glass",
            UiStyle.KING_CLASSIC to "classic", UiStyle.KK_UI to "kk",
        )
        var current by androidx.compose.runtime.mutableStateOf(styles[0].first to false)
        rule.setContent {
            val (style, dark) = current
            KingPlayTheme(ThemeConfig(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, uiStyle = style, useDynamicColor = false)) {
                Sample(style)
            }
        }
        for ((style, key) in styles) for (dark in listOf(false, true)) {
            current = style to dark
            rule.waitForIdle()
            send("g_${key}_${if (dark) "dark" else "light"}")
        }
    }
}

@Composable
private fun Sample(style: UiStyle) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Library", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 16.dp))
            TabRow(selectedTabIndex = 0) {
                listOf("Songs", "Albums", "Artists").forEachIndexed { i, t ->
                    Tab(selected = i == 0, onClick = {}, text = { Text(t) })
                }
            }
            MediaRow(title = "Midnight City", subtitle = "M83 · Hurry Up, We're Dreaming", trailingText = "4:03", onClick = {}, highlighted = true)
            MediaRow(title = "Digital Love", subtitle = "Daft Punk · Discovery", trailingText = "4:58", onClick = {})
            MediaRow(title = "Nightcall", subtitle = "Kavinsky · OutRun", trailingText = "4:18", onClick = {})
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpButton(text = "Play", onClick = {})
                KpButton(text = "Shuffle all", onClick = {}, style = KpButtonStyle.Tonal)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                val shape = MaterialTheme.shapes.extraLarge
                val cover = Modifier.size(150.dp)
                when (style) {
                    UiStyle.KING_NEON -> Box(
                        cover.shadow(28.dp, shape, ambientColor = colors.primary, spotColor = colors.secondary)
                            .border(3.dp, Brush.linearGradient(listOf(colors.primary, colors.secondary)), shape).padding(3.dp)
                            .background(Color(0xFF8A2BE2), shape),
                    )
                    UiStyle.KING_GLASS -> Box(
                        cover.padding(10.dp).shadow(24.dp, shape, ambientColor = colors.primary, spotColor = colors.primary)
                            .background(Color(0xFF5B8DEF), shape),
                    )
                    UiStyle.KING_CLASSIC -> Box(
                        cover.shadow(10.dp, shape).background(colors.secondary, shape).padding(8.dp)
                            .background(colors.surface, shape).padding(2.dp).background(Color(0xFFB5651D), MaterialTheme.shapes.extraSmall),
                    )
                    else -> Box(cover.background(Color(0xFFD94F4F), shape))
                }
                Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Now playing", style = MaterialTheme.typography.titleMedium)
                    Text("Midnight City", style = MaterialTheme.typography.bodyLarge)
                    Slider(value = 0.4f, onValueChange = {})
                    FilledIconButton(onClick = {}, modifier = Modifier.size(56.dp)) { Icon(Icons.Default.PlayArrow, null) }
                }
            }
        }
    }
}
