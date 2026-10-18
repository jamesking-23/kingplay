package com.kingplay.data.settings.di

import com.kingplay.core.common.ConsentGate
import com.kingplay.core.model.repository.SettingsRepository
import com.kingplay.data.settings.DataStoreConsentGate
import com.kingplay.data.settings.DataStoreSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    @Binds abstract fun settings(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds abstract fun consent(impl: DataStoreConsentGate): ConsentGate
}
