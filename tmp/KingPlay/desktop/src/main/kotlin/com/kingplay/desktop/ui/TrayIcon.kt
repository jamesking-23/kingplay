package com.kingplay.desktop.ui

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage

/** A violet disc with a white play triangle, drawn in code so it shows on light and dark taskbars alike. */
fun trayIconPainter(): Painter {
    val size = ICON_SIZE
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = Color(0x6D, 0x4A, 0xFF)
        g.fillOval(2, 2, size - 4, size - 4)
        g.color = Color.WHITE
        val xs = intArrayOf(size * 2 / 5, size * 2 / 5, size * 7 / 10)
        val ys = intArrayOf(size / 4, size * 3 / 4, size / 2)
        g.fillPolygon(xs, ys, 3)
    } finally {
        g.dispose()
    }
    return BitmapPainter(image.toComposeImageBitmap())
}

private const val ICON_SIZE = 64
