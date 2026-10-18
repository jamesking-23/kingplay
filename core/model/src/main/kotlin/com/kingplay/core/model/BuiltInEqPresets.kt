package com.kingplay.core.model

/**
 * Presets shipped with the app. They live in code, not in the database, so they can be improved
 * between releases. Ids are zero or negative; user presets in the database always have positive ids.
 */
object BuiltInEqPresets {
    // Bands: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k Hz
    val all: List<EqPreset> = listOf(
        EqPreset.Flat,
        preset(-1, "Bass boost", 6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
        preset(-2, "Treble boost", 0f, 0f, 0f, 0f, 0f, 1f, 2f, 4f, 5f, 6f),
        preset(-3, "Vocal", -2f, -2f, -1f, 1f, 3f, 4f, 3f, 1f, 0f, -1f),
        preset(-4, "Rock", 4f, 3f, 2f, -1f, -2f, -1f, 2f, 3f, 4f, 4f),
        preset(-5, "Pop", -1f, 1f, 3f, 4f, 3f, 0f, -1f, -1f, 1f, 2f),
        preset(-6, "Jazz", 3f, 2f, 1f, 2f, -2f, -2f, 0f, 1f, 2f, 3f),
        preset(-7, "Classical", 4f, 3f, 2f, 1f, -1f, -1f, 0f, 2f, 3f, 4f),
        preset(-8, "Electronic", 5f, 4f, 1f, 0f, -2f, 2f, 1f, 1f, 4f, 5f),
        preset(-9, "Hip-hop", 5f, 4f, 3f, 1f, -1f, -1f, 1f, 0f, 2f, 3f),
        preset(-10, "Acoustic", 4f, 3f, 2f, 1f, 2f, 2f, 3f, 3f, 3f, 2f),
        preset(-11, "Late night", 3f, 2f, 0f, -1f, -1f, 0f, 1f, 1f, 0f, -2f),
    )

    fun byId(id: Long): EqPreset? = all.firstOrNull { it.id == id }

    private fun preset(id: Long, name: String, vararg gains: Float) = EqPreset(
        id = id,
        name = name,
        preampDb = 0f,
        bandGainsDb = gains.toList(),
        isBuiltIn = true,
    )
}
