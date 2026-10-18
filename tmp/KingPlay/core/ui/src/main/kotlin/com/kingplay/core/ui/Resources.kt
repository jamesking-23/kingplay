package com.kingplay.core.ui

import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

/** Plural string for [count]; the count is also the first format argument. */
@Composable
fun quantityString(@PluralsRes id: Int, count: Int, vararg extraArgs: Any): String {
    val resources = LocalContext.current.resources
    return resources.getQuantityString(id, count, count, *extraArgs)
}

/** "first · second", skipping parts that are blank. */
@Composable
fun joinWithDot(first: String, second: String?): String =
    if (second.isNullOrBlank()) first else stringResource(R.string.ui_dot_separator, first, second)
