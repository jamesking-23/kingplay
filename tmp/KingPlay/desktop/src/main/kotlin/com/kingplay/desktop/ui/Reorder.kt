package com.kingplay.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/** Rows that can be dragged are all this tall, so the drop position is plain arithmetic. */
val REORDER_ROW_HEIGHT = 56.dp

/** What a row needs to be reorderable: where it is, how many rows there are, and what to do on a drop. */
class ReorderSpec(val position: Int, val count: Int, val onMove: (from: Int, to: Int) -> Unit)

class DragState(private val latest: State<ReorderSpec>, private val rowPx: Float) {
    private val spec get() = latest.value

    var dragPx by mutableFloatStateOf(0f)
    var dragging by mutableStateOf(false)

    val rowModifier: Modifier
        get() = Modifier
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer { translationY = dragPx }
            .background(if (dragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)

    fun finish() {
        val to = dragTarget(spec.position, dragPx, rowPx, spec.count)
        dragging = false
        dragPx = 0f
        if (to != spec.position) spec.onMove(spec.position, to)
    }

    fun cancel() {
        dragging = false
        dragPx = 0f
    }
}

@Composable
fun rememberDragState(spec: ReorderSpec): DragState {
    val rowPx = with(LocalDensity.current) { REORDER_ROW_HEIGHT.toPx() }
    // The row always reads the newest spec, so a callback captured before an earlier move is never used.
    val latest = rememberUpdatedState(spec)
    return remember(rowPx) { DragState(latest, rowPx) }
}

/** The grip at the end of a row. Dragging moves the row; "Move up/down" in the row menu does it by keyboard. */
@Composable
fun ReorderHandle(state: DragState, label: String) {
    Icon(
        imageVector = Icons.Filled.DragHandle,
        contentDescription = label,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .size(40.dp)
            .padding(8.dp)
            .alpha(if (state.dragging) 1f else HANDLE_ALPHA)
            .pointerInput(state) {
                detectDragGestures(
                    onDragStart = { state.dragging = true },
                    onDragEnd = state::finish,
                    onDragCancel = state::cancel,
                    onDrag = { change, amount ->
                        change.consume()
                        state.dragPx += amount.y
                    },
                )
            },
    )
}

private const val HANDLE_ALPHA = 0.7f
