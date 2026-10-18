package com.kingplay.desktop.cover

import java.io.File

private val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp", "bmp")
private val COVER_NAMES = listOf("cover", "folder", "front", "album", "albumart", "albumartsmall")

/** Largest image file read as a cover; anything bigger is almost certainly not album art. */
const val MAX_COVER_BYTES = 8L * 1024 * 1024

/**
 * The picture that sits next to the music (cover.jpg, folder.png …), by the usual names in order of preference.
 * Returns null when there is none.
 */
fun findFolderCover(directory: File): File? {
    val files = directory.listFiles()?.filter { it.isFile && it.length() in 1..MAX_COVER_BYTES } ?: return null
    for (name in COVER_NAMES) {
        for (ext in IMAGE_EXTENSIONS) {
            files.firstOrNull { it.name.equals("$name.$ext", ignoreCase = true) }?.let { return it }
        }
    }
    return null
}

/** A small "least recently used" map: the oldest entries go first once [maxSize] is reached. */
class LruCache<K : Any, V : Any>(private val maxSize: Int) {
    private val map = object : LinkedHashMap<K, V>(INITIAL_CAPACITY, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean = size > maxSize
    }

    init {
        require(maxSize > 0) { "maxSize must be positive" }
    }

    @Synchronized
    operator fun get(key: K): V? = map[key]

    @Synchronized
    operator fun set(key: K, value: V) {
        map[key] = value
    }

    @Synchronized
    fun contains(key: K): Boolean = map.containsKey(key)

    val size: Int @Synchronized get() = map.size

    private companion object {
        const val INITIAL_CAPACITY = 16
        const val LOAD_FACTOR = 0.75f
    }
}

/** Size to draw a cover at so its longer side is [target] pixels, never enlarging. */
fun scaledSize(width: Int, height: Int, target: Int): Pair<Int, Int> {
    if (width <= 0 || height <= 0 || target <= 0) return 1 to 1
    val longest = maxOf(width, height)
    if (longest <= target) return width to height
    val factor = target.toDouble() / longest
    return maxOf(1, (width * factor).toInt()) to maxOf(1, (height * factor).toInt())
}
