package com.kingplay.core.player.engine

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * The standard renderers with one change: our processor sits in the audio sink. Float output is on so that
 * 24-bit and 32-bit files reach the processor as float instead of being cut to 16 bits first.
 */
@OptIn(UnstableApi::class)
class KingPlayRenderersFactory(
    context: Context,
    private val processor: KingPlayAudioProcessor,
) : DefaultRenderersFactory(context) {

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink = DefaultAudioSink.Builder(context)
        .setEnableFloatOutput(true)
        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
        .setAudioProcessors(arrayOf<AudioProcessor>(processor))
        .build()
}
