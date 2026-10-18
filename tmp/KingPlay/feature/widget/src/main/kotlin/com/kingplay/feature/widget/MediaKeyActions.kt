package com.kingplay.feature.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartService
import com.kingplay.core.player.service.PlaybackService

/**
 * The widget buttons press media keys on the playback service, exactly as a headset button would.
 * Media3 turns the key into the matching player command, so the widget needs no player code of its own.
 */
internal fun mediaKeyAction(context: Context, keyCode: Int): Action {
    val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
        .setComponent(ComponentName(context, PlaybackService::class.java))
        .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    // The service may not be running yet (e.g. after a reboot), so it has to be allowed to start in the foreground.
    return actionStartService(intent, isForegroundService = true)
}
