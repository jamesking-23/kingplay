package com.kingplay.core.ui

/** Load state every screen uses, so loading / empty / error are never forgotten. */
sealed interface Async<out T> {
    data object Loading : Async<Nothing>

    data object Empty : Async<Nothing>

    data class Error(val message: String? = null) : Async<Nothing>

    data class Content<T>(val data: T) : Async<T>
}
