package com.kingplay.feature.library.list

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.kingplay.core.designsystem.component.KpEmptyState
import com.kingplay.core.ui.Async
import com.kingplay.core.ui.AsyncContent

/** A tab that shows a plain list with the standard loading, empty and error states. */
@Composable
fun <T> ListTab(
    state: Async<List<T>>,
    onRetry: () -> Unit,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptyMessage: String,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    row: @Composable (T) -> Unit,
) {
    AsyncContent(
        state = state,
        onRetry = onRetry,
        modifier = modifier,
        empty = {
            KpEmptyState(icon = emptyIcon, title = emptyTitle, message = emptyMessage)
        },
    ) { items ->
        LazyColumn {
            if (header != null) item(key = "header") { header() }
            items(items, key = key) { item -> row(item) }
        }
    }
}
