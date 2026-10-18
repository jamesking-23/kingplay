package com.kingplay.desktop.ui

import com.kingplay.core.model.repository.PlaybackState

/** Windows cuts tray tooltips at 127 characters; stay under it. */
private const val MAX_TOOLTIP = 120

/** "Song — Artist" while something is loaded, with a paused marker; just the app name when idle. */
fun trayTooltip(state: PlaybackState): String {
    val title = state.title?.takeIf { it.isNotBlank() } ?: return "KingPlay"
    val text = listOfNotNull(title, state.artist?.takeIf { it.isNotBlank() }).joinToString(" — ")
    val shown = if (state.isPlaying) text else "$text (paused)"
    return if (shown.length <= MAX_TOOLTIP) shown else shown.take(MAX_TOOLTIP - 1).trimEnd() + "…"
}

enum class CloseAction { HideToTray, Quit }

/** Closing the window hides it only when the person asked for that and a tray exists to bring it back. */
fun closeAction(closeToTray: Boolean, trayAvailable: Boolean): CloseAction =
    if (closeToTray && trayAvailable) CloseAction.HideToTray else CloseAction.Quit
