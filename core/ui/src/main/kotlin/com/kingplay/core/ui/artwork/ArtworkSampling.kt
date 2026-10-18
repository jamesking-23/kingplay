package com.kingplay.core.ui.artwork

/** Largest power of two that keeps both sides at least [target] pixels. 1 means decode at full size. */
internal fun sampleSize(width: Int, height: Int, target: Int): Int {
    if (width <= 0 || height <= 0 || target <= 0) return 1
    var sample = 1
    while (width / (sample * 2) >= target && height / (sample * 2) >= target) sample *= 2
    return sample
}
