package com.kingplay.core.player.di

import com.kingplay.core.model.repository.AudioSettingsRepository
import com.kingplay.core.model.repository.LyricsRepository
import com.kingplay.core.model.repository.PlaybackController
import com.kingplay.core.model.repository.SleepTimer
import com.kingplay.core.model.repository.SpectrumSource
import com.kingplay.core.player.controller.MediaControllerPlaybackController
import com.kingplay.core.player.engine.DefaultAudioSettingsRepository
import com.kingplay.core.player.engine.DefaultSleepTimer
import com.kingplay.core.player.engine.DefaultSpectrumSource
import com.kingplay.core.player.engine.PlayingLyrics
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

    @Binds
    abstract fun lyrics(impl: PlayingLyrics): LyricsRepository

    @Binds
    abstract fun sleepTimer(impl: DefaultSleepTimer): SleepTimer

    @Binds
    abstract fun spectrum(impl: DefaultSpectrumSource): SpectrumSource
}
