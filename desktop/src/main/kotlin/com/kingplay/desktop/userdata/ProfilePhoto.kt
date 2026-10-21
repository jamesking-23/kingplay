package com.kingplay.desktop.userdata

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** Keeps the profile picture as a small private copy inside the KingPlay data folder. */
object ProfilePhoto {
    const val MAX_SIDE = 512

    /** The size a picture is shrunk to so its longest side is at most [MAX_SIDE]; smaller pictures stay as they are. */
    fun fitted(width: Int, height: Int): Pair<Int, Int> {
        val longest = maxOf(width, height)
        if (longest <= MAX_SIDE || width <= 0 || height <= 0) return width to height
        val scale = MAX_SIDE.toDouble() / longest
        return maxOf(1, (width * scale).toInt()) to maxOf(1, (height * scale).toInt())
    }

    /** Reads [source], shrinks it, saves it as a new JPEG in [dataDir] and removes older copies. Null if unreadable. */
    fun save(source: File, dataDir: File, now: Long = System.currentTimeMillis()): File? = runCatching {
        val original = ImageIO.read(source) ?: return null
        val (w, h) = fitted(original.width, original.height)
        val flat = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = flat.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(original, 0, 0, w, h, java.awt.Color.WHITE, null)
        g.dispose()
        dataDir.mkdirs()
        val target = File(dataDir, "profile_$now.jpg")
        if (!ImageIO.write(flat, "jpg", target)) return null
        removeOld(dataDir, keep = target)
        target
    }.getOrNull()

    fun remove(dataDir: File) = removeOld(dataDir, keep = null)

    private fun removeOld(dataDir: File, keep: File?) {
        dataDir.listFiles { f -> f.name.startsWith("profile_") && f != keep }?.forEach { it.delete() }
    }
}
