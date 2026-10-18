package com.kingplay.core.player.engine

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.kingplay.core.player.dsp.DspConfig
import com.kingplay.core.player.dsp.EqChain
import dagger.hilt.android.scopes.ServiceScoped
import java.nio.ByteBuffer
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Puts the equalizer, bass boost, loudness and ReplayGain in the player's audio path.
 *
 * It only touches 16-bit and float PCM. For any other format it stays inactive instead of throwing, because a
 * throw here would stop playback; the music then plays unprocessed.
 */
@OptIn(UnstableApi::class)
@ServiceScoped
class KingPlayAudioProcessor @Inject constructor() : BaseAudioProcessor() {

    @Volatile private var config: DspConfig = DspConfig.Bypass
    private var chain: EqChain? = null
    private var encoding = C.ENCODING_INVALID
    private var channels = 0
    private var wasBypass = true
    private var scratch = FloatArray(0)

    /** Safe to call from any thread. The audio thread picks the new config up on its next buffer. */
    fun setConfig(newConfig: DspConfig) {
        config = newConfig
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val supported = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        if (!supported || inputAudioFormat.channelCount !in 1..MAX_CHANNELS || inputAudioFormat.sampleRate <= 0) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        encoding = inputAudioFormat.encoding
        channels = inputAudioFormat.channelCount
        chain = EqChain(inputAudioFormat.sampleRate, channels)
        wasBypass = true
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val output = replaceOutputBuffer(size)
        val current = config
        val eq = chain
        if (eq == null || current.bypass) {
            wasBypass = true
            output.put(inputBuffer)
            output.flip()
            return
        }
        if (wasBypass) {
            // Filter memory is stale after a stretch of pass-through. Start clean to avoid a click.
            eq.reset()
            wasBypass = false
        }
        eq.configure(current)
        val bytesPerSample = if (encoding == C.ENCODING_PCM_FLOAT) BYTES_FLOAT else BYTES_16BIT
        val frames = size / bytesPerSample / channels
        val samples = frames * channels
        if (scratch.size < samples) scratch = FloatArray(samples)
        if (encoding == C.ENCODING_PCM_FLOAT) {
            for (i in 0 until samples) scratch[i] = inputBuffer.getFloat()
        } else {
            for (i in 0 until samples) scratch[i] = inputBuffer.getShort() / PCM16_SCALE
        }
        eq.process(scratch, frames)
        if (encoding == C.ENCODING_PCM_FLOAT) {
            for (i in 0 until samples) output.putFloat(scratch[i])
        } else {
            for (i in 0 until samples) {
                val value = (scratch[i] * PCM16_SCALE).roundToInt()
                output.putShort(value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
            }
        }
        output.put(inputBuffer) // a partial trailing frame, if any, goes through untouched
        output.flip()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onFlush() {
        chain?.reset()
        wasBypass = true
    }

    override fun onReset() {
        chain = null
        scratch = FloatArray(0)
        encoding = C.ENCODING_INVALID
        channels = 0
    }

    private companion object {
        const val MAX_CHANNELS = 8
        const val BYTES_16BIT = 2
        const val BYTES_FLOAT = 4
        const val PCM16_SCALE = 32_768f
    }
}
