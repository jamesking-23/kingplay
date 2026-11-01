package com.kingplay.desktop.ui

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage

/** The KingPlay logo (black on white), used for the tray, every window and the taskbar. */
fun trayIconPainter(): Painter = logoPainter

private val logoPainter: Painter by lazy {
    val loaded = runCatching {
        Thread.currentThread().contextClassLoader.getResourceAsStream("logo.png")?.use { javax.imageio.ImageIO.read(it) }
    }.getOrNull()
    val size = ICON_SIZE
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        if (loaded != null) {
            g.drawImage(loaded, 0, 0, size, size, null)
        } else {
            g.color = Color.WHITE
            g.fillRect(0, 0, size, size)
            g.color = Color.BLACK
            g.fillOval(size / 4, size / 4, size / 2, size / 2)
        }
    } finally {
        g.dispose()
    }
    BitmapPainter(image.toComposeImageBitmap())
}

private const val ICON_SIZE = 128
