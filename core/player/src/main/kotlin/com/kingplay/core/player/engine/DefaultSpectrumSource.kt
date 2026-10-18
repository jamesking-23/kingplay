package com.kingplay.core.player.engine

import com.kingplay.core.model.repository.SpectrumSource
import com.kingplay.core.player.dsp.SPECTRUM_BANDS
import javax.inject.Inject
import javax.inject.Singleton

/** Handoff between the audio thread (writes) and the screen (reads once per frame). */
@Singleton
class DefaultSpectrumSource @Inject constructor() : SpectrumSource {
    @Volatile private var bands = FloatArray(SPECTRUM_BANDS)

    @Volatile var active = false
        private set

    override val bandCount: Int = SPECTRUM_BANDS

    override fun latest(): FloatArray = bands

    override fun setActive(active: Boolean) {
        this.active = active
        if (!active) bands = FloatArray(SPECTRUM_BANDS)
    }

    /** Called from the audio thread. Each call hands over a new array, so readers never see a half-written one. */
    fun publish(newBands: FloatArray) {
        bands = newBands
    }
}
