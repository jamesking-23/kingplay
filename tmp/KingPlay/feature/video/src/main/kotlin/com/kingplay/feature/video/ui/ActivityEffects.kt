package com.kingplay.feature.video.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.roundToInt

private const val MAX_PIP_RATIO = 2.39f
private const val MIN_PIP_RATIO = 1f / 2.39f
private const val PIP_RATIO_SCALE = 1000
private const val DEFAULT_BRIGHTNESS = 0.5f

fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/** Hides the status and navigation bars while the video screen is open, and brings them back afterwards. */
@Composable
fun ImmersiveMode(activity: Activity?) {
    DisposableEffect(activity) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

/** Puts the screen in [orientation] while this is shown and returns it to automatic when it leaves. */
@Composable
fun ScreenOrientation(activity: Activity?, orientation: Int) {
    DisposableEffect(activity, orientation) {
        activity?.requestedOrientation = orientation
        onDispose { }
    }
    DisposableEffect(activity) {
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
}

/** Window brightness (0..1). Gives the system its control back when the screen closes. */
class BrightnessControl(private val activity: Activity?) {
    fun current(): Float {
        val set = activity?.window?.attributes?.screenBrightness ?: DEFAULT_BRIGHTNESS
        return if (set >= 0f) set else DEFAULT_BRIGHTNESS
    }

    fun set(level: Float) {
        val window = activity?.window ?: return
        window.attributes = window.attributes.apply { screenBrightness = level.coerceIn(0.01f, 1f) }
    }

    fun release() {
        val window = activity?.window ?: return
        window.attributes = window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }
}

@Composable
fun rememberBrightnessControl(activity: Activity?): BrightnessControl {
    val control = remember(activity) { BrightnessControl(activity) }
    DisposableEffect(control) { onDispose { control.release() } }
    return control
}

/** Music-stream volume (0..1). */
class VolumeControl(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

    fun current(): Float = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max

    fun set(level: Float) {
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (level.coerceIn(0f, 1f) * max).roundToInt(), 0)
    }
}

/** True while the activity is shown in a small picture-in-picture window. */
@Composable
fun rememberInPictureInPicture(activity: Activity?): Boolean {
    var inPip by remember { mutableStateOf(activity?.isInPictureInPictureMode == true) }
    DisposableEffect(activity) {
        val host = activity as? ComponentActivity
        val listener = Consumer<PictureInPictureModeChangedInfo> { inPip = it.isInPictureInPictureMode }
        host?.addOnPictureInPictureModeChangedListener(listener)
        onDispose { host?.removeOnPictureInPictureModeChangedListener(listener) }
    }
    return inPip
}

private fun pipParams(width: Int, height: Int, autoEnter: Boolean): PictureInPictureParams {
    val builder = PictureInPictureParams.Builder()
    if (width > 0 && height > 0) {
        val ratio = (width.toFloat() / height).coerceIn(MIN_PIP_RATIO, MAX_PIP_RATIO)
        builder.setAspectRatio(Rational((ratio * PIP_RATIO_SCALE).roundToInt(), PIP_RATIO_SCALE))
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setAutoEnterEnabled(autoEnter)
    return builder.build()
}

fun enterPictureInPicture(activity: Activity?, width: Int, height: Int) {
    activity?.enterPictureInPictureMode(pipParams(width, height, autoEnter = false))
}

/** On Android 12+ the phone can enter picture-in-picture by itself when the user swipes home during playback. */
@Composable
fun AutoEnterPictureInPicture(activity: Activity?, enabled: Boolean, width: Int, height: Int) {
    DisposableEffect(activity, enabled, width, height) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity?.setPictureInPictureParams(pipParams(width, height, autoEnter = enabled))
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity?.setPictureInPictureParams(pipParams(width, height, autoEnter = false))
            }
        }
    }
}
