package com.kingplay.feature.widget

import com.kingplay.core.model.repository.NowPlayingSnapshot

/** Longest text the widget shows; a very long tag must not blow up the remote views. */
internal const val WIDGET_TEXT_LIMIT = 80

/** Largest cover edge, in pixels, that goes into the widget (remote views have a hard memory cap). */
internal const val WIDGET_COVER_PX = 256

internal fun clampText(text: String, limit: Int = WIDGET_TEXT_LIMIT): String {
    val clean = text.replace('\n', ' ').replace('\r', ' ').trim()
    return if (clean.length <= limit) clean else clean.take(limit - 1).trimEnd() + "…"
}

/** What the widget draws, decided in one place so it can be tested without Android. */
internal data class WidgetLines(val title: String, val subtitle: String, val idle: Boolean)

internal fun widgetLines(snapshot: NowPlayingSnapshot, idleTitle: String, idleHint: String): WidgetLines =
    if (snapshot.isIdle) {
        WidgetLines(idleTitle, idleHint, idle = true)
    } else {
        WidgetLines(clampText(snapshot.title), clampText(snapshot.artist), idle = false)
    }

/** Picks a sample size so the decoded cover is at most [target] px on its longer side. */
internal fun sampleSizeFor(width: Int, height: Int, target: Int = WIDGET_COVER_PX): Int {
    if (width <= 0 || height <= 0 || target <= 0) return 1
    var sample = 1
    var longest = maxOf(width, height)
    while (longest / 2 >= target) {
        longest /= 2
        sample *= 2
    }
    return sample
}
