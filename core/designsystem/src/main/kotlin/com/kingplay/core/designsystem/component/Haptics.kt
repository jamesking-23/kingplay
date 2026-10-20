package com.kingplay.core.designsystem.component

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.kingplay.core.model.settings.HapticKind
import com.kingplay.core.model.settings.HapticLevel
import com.kingplay.core.model.settings.HapticSettings

val LocalHaptics = staticCompositionLocalOf { HapticSettings() }

/** Wraps [onClick] so the tap vibrates the way the person set for ordinary buttons. */
@Composable
fun rememberHapticClick(onClick: () -> Unit): () -> Unit = rememberHapticClick(HapticKind.TAP, onClick)

/** Wraps [onClick] with the vibration the person chose for this [kind] of button (maybe none). */
@Composable
fun rememberHapticClick(kind: HapticKind, onClick: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val level = LocalHaptics.current.levelFor(kind)
    return remember(onClick, context, level, kind) {
        {
            Haptics.play(context, level, kind)
            onClick()
        }
    }
}

/** The vibration itself, shared by every button and by the settings screen's "try it" buttons. */
object Haptics {
    /** Plays [level]; the play button gets a double pulse. Returns false when nothing could be played. */
    fun play(context: Context, level: HapticLevel, kind: HapticKind = HapticKind.TAP): Boolean {
        if (level == HapticLevel.OFF) return false
        return runCatching {
            val vibrator = vibratorOf(context)
            if (!vibrator.hasVibrator()) return@runCatching false
            vibrator.vibrate(effect(vibrator, level, double = kind == HapticKind.PLAY && level != HapticLevel.LIGHT))
            true
        }.getOrDefault(false)
    }

    private fun effect(vibrator: Vibrator, level: HapticLevel, double: Boolean): VibrationEffect {
        val ms = when (level) {
            HapticLevel.LIGHT -> LIGHT_MS
            HapticLevel.MEDIUM -> MEDIUM_MS
            else -> STRONG_MS
        }
        val amp = when (level) {
            HapticLevel.LIGHT -> LIGHT_AMP
            HapticLevel.MEDIUM -> MEDIUM_AMP
            else -> STRONG_AMP
        }
        if (double) {
            val timings = longArrayOf(0, ms, GAP_MS, ms)
            return if (vibrator.hasAmplitudeControl()) {
                VibrationEffect.createWaveform(timings, intArrayOf(0, amp, 0, amp), -1)
            } else {
                VibrationEffect.createWaveform(timings, -1)
            }
        }
        if (vibrator.hasAmplitudeControl()) return VibrationEffect.createOneShot(ms, amp)
        // No strength control: the phone's own predefined effects are the best way to tell the levels apart.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return VibrationEffect.createPredefined(
                when (level) {
                    HapticLevel.LIGHT -> VibrationEffect.EFFECT_TICK
                    HapticLevel.MEDIUM -> VibrationEffect.EFFECT_CLICK
                    else -> VibrationEffect.EFFECT_HEAVY_CLICK
                },
            )
        }
        return VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
    }

    @Suppress("DEPRECATION")
    private fun vibratorOf(context: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    private const val LIGHT_MS = 10L
    private const val MEDIUM_MS = 20L
    private const val STRONG_MS = 36L
    private const val GAP_MS = 45L
    private const val LIGHT_AMP = 70
    private const val MEDIUM_AMP = 150
    private const val STRONG_AMP = 255
}

/** A quick left-right shake whenever [trigger] changes, so the button visibly "vibrates" with the buzz. */
fun Modifier.shakeOn(trigger: Any?): Modifier = composed {
    val angle = remember { Animatable(0f) }
    val first = remember { mutableStateOf(true) }
    LaunchedEffect(trigger) {
        if (first.value) {
            first.value = false
            return@LaunchedEffect
        }
        angle.snapTo(0f)
        for (target in SHAKE_STEPS) angle.animateTo(target, tween(SHAKE_STEP_MS))
        angle.animateTo(0f, tween(SHAKE_STEP_MS))
    }
    this.graphicsLayer { rotationZ = angle.value }
}

private val SHAKE_STEPS = floatArrayOf(10f, -10f, 7f, -7f, 3f)
private const val SHAKE_STEP_MS = 40
