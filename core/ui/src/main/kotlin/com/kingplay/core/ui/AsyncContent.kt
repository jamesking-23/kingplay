package com.kingplay.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kingplay.core.designsystem.component.KpErrorState
import com.kingplay.core.designsystem.component.KpSkeletonList
import com.kingplay.core.designsystem.theme.Motion

/** Load state every screen uses, so loading / empty / error are never forgotten. */
sealed interface Async<out T> {
    data object Loading : Async<Nothing>

    data object Empty : Async<Nothing>

    data class Error(val message: String? = null) : Async<Nothing>

    data class Content<T>(val data: T) : Async<T>
}

@Composable
fun <T> AsyncContent(
    state: Async<T>,
    onRetry: () -> Unit,
    empty: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    loading: @Composable () -> Unit = { KpSkeletonList() },
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = state,
        modifier = modifier,
        contentKey = { it::class },
        transitionSpec = {
            fadeIn(Motion.effects(Motion.DurationMedium)) togetherWith fadeOut(Motion.effects(Motion.DurationShort))
        },
        label = "asyncContent",
    ) { current ->
        when (current) {
            Async.Loading -> loading()
            Async.Empty -> empty()
            is Async.Error -> KpErrorState(onRetry = onRetry, message = current.message)
            is Async.Content -> content(current.data)
        }
    }
}
