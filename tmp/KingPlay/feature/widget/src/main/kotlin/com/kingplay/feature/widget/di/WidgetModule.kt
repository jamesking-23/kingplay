package com.kingplay.feature.widget.di

import com.kingplay.core.model.repository.NowPlayingPublisher
import com.kingplay.feature.widget.WidgetPublisher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class WidgetModule {
    @Binds
    abstract fun publisher(impl: WidgetPublisher): NowPlayingPublisher
}
