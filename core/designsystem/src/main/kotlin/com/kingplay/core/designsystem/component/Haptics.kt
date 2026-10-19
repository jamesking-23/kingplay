package com.kingplay.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Wraps [onClick] so the tap gives light haptic feedback. */
@Composable
fun rememberHapticClick(onClick: () -> Unit): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return remember(onClick, haptics) {
        {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    }
}

/**
 * Play/pause feedback that is actually felt: a short double pulse from the device vibrator (system "touch feedback"
 * settings do not silence it), falling back to the standard haptic when no vibrator exists. [onTap] runs after.
 */
@Composable
fun rememberVibratingClick(onClick: () -> Unit): () -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptics = LocalHapticFeedback.current
    return remember(onClick, context, haptics) {
        {
            if (!vibratePulse(context)) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    }
}

@Suppress("DEPRECATION")
private fun vibratePulse(context: android.content.Context): Boolean = runCatching {
    val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager)
            .defaultVibrator
    } else {
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
    }
    if (!vibrator.hasVibrator()) return@runCatching false
    val timings = longArrayOf(0, PULSE_MS, PULSE_GAP_MS, PULSE_MS)
    vibrator.vibrate(android.os.VibrationEffect.createWaveform(timings, -1))
    true
}.getOrDefault(false)

private const val PULSE_MS = 28L
private const val PULSE_GAP_MS = 45L

/** A quick left-right shake of [trigger]'s change, so the button visibly "vibrates" along with the buzz. */
fun androidx.compose.ui.Modifier.shakeOn(trigger: Any?): androidx.compose.ui.Modifier = androidx.compose.ui.composed {
    val angle = remember { androidx.compose.animation.core.Animatable(0f) }
    val first = remember { androidx.compose.runtime.mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(trigger) {
        if (first.value) {
            first.value = false
            return@LaunchedEffect
        }
        angle.snapTo(0f)
        for (target in SHAKE_STEPS) angle.animateTo(target, androidx.compose.animation.core.tween(SHAKE_STEP_MS))
        angle.animateTo(0f, androidx.compose.animation.core.tween(SHAKE_STEP_MS))
    }
    this.graphicsLayer { rotationZ = angle.value }
}

private val SHAKE_STEPS = floatArrayOf(10f, -10f, 7f, -7f, 3f)
private const val SHAKE_STEP_MS = 40
