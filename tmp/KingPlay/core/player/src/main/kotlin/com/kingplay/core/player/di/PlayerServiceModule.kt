package com.kingplay.core.player.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.kingplay.core.player.engine.KingPlayAudioProcessor
import com.kingplay.core.player.engine.KingPlayRenderersFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

/**
 * Service-scoped on purpose: the service releases the player when it is destroyed, so the next service instance
 * must get a fresh one. A singleton would hand out a released player.
 */
@Module
@InstallIn(ServiceComponent::class)
object PlayerServiceModule {
    @Provides
    @ServiceScoped
    fun player(@ApplicationContext context: Context, processor: KingPlayAudioProcessor): Player {
        val music = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        return ExoPlayer.Builder(context, KingPlayRenderersFactory(context, processor))
            .setAudioAttributes(music, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
    }
}
