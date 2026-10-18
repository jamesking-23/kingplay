package com.kingplay.core.player.di

import com.kingplay.core.model.repository.AudioSettingsRepository
import com.kingplay.core.model.repository.PlaybackController
import com.kingplay.core.player.controller.MediaControllerPlaybackController
import com.kingplay.core.player.engine.DefaultAudioSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerBindingsModule {
    @Binds
    abstract fun playbackController(impl: MediaControllerPlaybackController): PlaybackController

    @Binds
    abstract fun audioSettings(impl: DefaultAudioSettingsRepository): AudioSettingsRepository
}
