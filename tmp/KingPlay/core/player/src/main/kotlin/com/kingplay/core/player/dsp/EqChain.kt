package com.kingplay.core.player.dsp

/**
 * Runs a [DspConfig] over interleaved float audio. Filter memory is kept per channel and survives configuration
 * changes, so moving a slider while music plays does not click. Not thread-safe: use from the audio thread only.
 */
class EqChain(private val sampleRate: Int, private val channels: Int) {
    private var filters: List<BiquadCoefficients> = emptyList()
    private var states: Array<Array<BiquadState>> = emptyArray()
    private var gain = 1f
    private var applied: DspConfig? = null

    /** Cheap when [config] is the one already applied (same object). */
    fun configure(config: DspConfig) {
        if (config === applied) return
        applied = config
        val designed = designFilters(config.bandGainsDb, config.bassShelfDb, sampleRate.toDouble())
        if (designed.size != states.size) {
            // The number of active filters changed. Existing memory would belong to different filters.
            states = Array(designed.size) { Array(channels) { BiquadState() } }
        }
        filters = designed
        gain = config.outputGain
    }

    /** Processes [frames] frames in place. [samples] holds `frames * channels` interleaved values in -1..1. */
    fun process(samples: FloatArray, frames: Int) {
        val filterCount = filters.size
        var index = 0
        for (frame in 0 until frames) {
            for (channel in 0 until channels) {
                var x = samples[index].toDouble()
                for (f in 0 until filterCount) x = states[f][channel].process(x, filters[f])
                samples[index] = softLimit((x * gain).toFloat())
                index++
            }
        }
    }

    fun reset() {
        for (perFilter in states) for (state in perFilter) state.reset()
    }
}
