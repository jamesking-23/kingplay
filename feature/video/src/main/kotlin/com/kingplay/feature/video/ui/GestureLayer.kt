package com.kingplay.feature.video.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.kingplay.feature.video.logic.DragAxis
import com.kingplay.feature.video.logic.TapZone
import com.kingplay.feature.video.logic.dragAxis
import com.kingplay.feature.video.logic.tapZone
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HOLD_MS = 600L

/** What the touch layer reports. Positions are in pixels; the screen turns them into seeks and level changes. */
class GestureHandlers(
    val onTap: () -> Unit,
    val onDoubleTap: (TapZone) -> Unit,
    val onHold: (Boolean) -> Unit,
    val onSeekStart: () -> Unit,
    val onSeekDrag: (dx: Float, width: Float) -> Unit,
    val onSeekEnd: () -> Unit,
    val onLevelStart: (rightSide: Boolean) -> Unit,
    val onLevelDrag: (dy: Float, height: Float, rightSide: Boolean) -> Unit,
    val onLevelEnd: () -> Unit,
)

/**
 * Full-screen touch area over the picture: tap shows or hides the controls, double tap skips 10 seconds (or
 * pauses in the middle), holding plays at double speed, a sideways drag scrubs, and a vertical drag changes
 * brightness on the left or volume on the right. Every one of these also has a button on the controls.
 */
@Composable
fun GestureLayer(handlers: GestureHandlers, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    val current by rememberUpdatedState(handlers)
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                var held = false
                detectTapGestures(
                    onTap = { current.onTap() },
                    onDoubleTap = { offset -> current.onDoubleTap(tapZone(offset.x, size.width.toFloat())) },
                    onPress = {
                        coroutineScope {
                            val hold = launch {
                                delay(HOLD_MS)
                                held = true
                                current.onHold(true)
                            }
                            tryAwaitRelease()
                            hold.cancel()
                            if (held) {
                                held = false
                                current.onHold(false)
                            }
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                var total = Offset.Zero
                var axis: DragAxis? = null
                var rightSide = false
                detectDragGestures(
                    onDragStart = { offset ->
                        total = Offset.Zero
                        axis = null
                        rightSide = offset.x > size.width / 2f
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        total += delta
                        if (axis == null) {
                            axis = dragAxis(total.x, total.y, viewConfiguration.touchSlop)
                            when (axis) {
                                DragAxis.HORIZONTAL -> current.onSeekStart()
                                DragAxis.VERTICAL -> current.onLevelStart(rightSide)
                                null -> Unit
                            }
                        }
                        when (axis) {
                            DragAxis.HORIZONTAL -> current.onSeekDrag(total.x, size.width.toFloat())
                            DragAxis.VERTICAL -> current.onLevelDrag(total.y, size.height.toFloat(), rightSide)
                            null -> Unit
                        }
                    },
                    onDragEnd = {
                        when (axis) {
                            DragAxis.HORIZONTAL -> current.onSeekEnd()
                            DragAxis.VERTICAL -> current.onLevelEnd()
                            null -> Unit
                        }
                    },
                    onDragCancel = {
                        when (axis) {
                            DragAxis.HORIZONTAL -> current.onSeekEnd()
                            DragAxis.VERTICAL -> current.onLevelEnd()
                            null -> Unit
                        }
                    },
                )
            },
    ) { content() }
}
