package com.kingplay.feature.widget

import android.content.Context
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.kingplay.core.model.repository.NowPlayingSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Home-screen controller: cover, title, artist and previous / play-pause / next. */
class PlaybackWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = SnapshotStore(context).load()
        val cover = withContext(Dispatchers.IO) { CoverLoader.load(context, snapshot.artworkSource) }
        provideContent {
            GlanceTheme {
                WidgetContent(context, snapshot, cover)
            }
        }
    }
}

@Composable
private fun WidgetContent(context: Context, snapshot: NowPlayingSnapshot, cover: android.graphics.Bitmap?) {
    val lines = widgetLines(
        snapshot,
        idleTitle = context.getString(R.string.widget_idle_title),
        idleHint = context.getString(R.string.widget_idle_hint),
    )
    val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val openModifier = if (openApp != null) GlanceModifier.clickable(actionStartActivity(openApp)) else GlanceModifier

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(12.dp)
            .then(openModifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(context, cover)
        Spacer(GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = lines.title,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = lines.subtitle,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            )
            Spacer(GlanceModifier.size(8.dp))
            Controls(context, snapshot.isPlaying, enabled = !lines.idle)
        }
    }
}

@Composable
private fun Cover(context: Context, cover: android.graphics.Bitmap?) {
    val modifier = GlanceModifier.size(72.dp).cornerRadius(16.dp)
    if (cover != null) {
        Image(
            provider = ImageProvider(cover),
            contentDescription = context.getString(R.string.widget_cover),
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(GlanceTheme.colors.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_music),
                contentDescription = null,
                modifier = GlanceModifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun Controls(context: Context, playing: Boolean, enabled: Boolean) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(
            context, R.drawable.ic_widget_previous, R.string.widget_previous,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS, enabled,
        )
        Spacer(GlanceModifier.width(8.dp))
        if (playing) {
            ControlButton(
                context, R.drawable.ic_widget_pause, R.string.widget_pause,
                KeyEvent.KEYCODE_MEDIA_PAUSE, enabled,
            )
        } else {
            ControlButton(
                context, R.drawable.ic_widget_play, R.string.widget_play,
                KeyEvent.KEYCODE_MEDIA_PLAY, true,
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        ControlButton(context, R.drawable.ic_widget_next, R.string.widget_next, KeyEvent.KEYCODE_MEDIA_NEXT, enabled)
    }
}

@Composable
private fun ControlButton(context: Context, icon: Int, label: Int, keyCode: Int, enabled: Boolean) {
    val description = context.getString(label)
    val base = GlanceModifier
        .size(44.dp) // 44dp keeps the touch target above the 48dp guideline once the padding is added
        .padding(4.dp)
        .cornerRadius(22.dp)
        .background(GlanceTheme.colors.primaryContainer)
        .semantics { contentDescription = description }
    Box(
        modifier = if (enabled) base.clickable(mediaKeyAction(context, keyCode)) else base,
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = description,
            modifier = GlanceModifier.size(24.dp),
        )
    }
}
