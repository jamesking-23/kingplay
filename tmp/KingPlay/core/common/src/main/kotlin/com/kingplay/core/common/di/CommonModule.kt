package com.kingplay.core.common.di

import com.kingplay.core.common.AnalyticsTracker
import com.kingplay.core.common.ApplicationScope
import com.kingplay.core.common.ConsentGate
import com.kingplay.core.common.DefaultFeatureFlags
import com.kingplay.core.common.Dispatcher
import com.kingplay.core.common.Entitlements
import com.kingplay.core.common.FeatureFlagSource
import com.kingplay.core.common.FeatureFlags
import com.kingplay.core.common.InMemoryConsentGate
import com.kingplay.core.common.InMemoryFeatureFlagSource
import com.kingplay.core.common.KpDispatchers
import com.kingplay.core.common.NoOpAnalyticsTracker
import com.kingplay.core.common.ProUnlockedEntitlements
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
abstract class CommonBindingsModule {
    @Binds
    abstract fun featureFlags(impl: DefaultFeatureFlags): FeatureFlags

    @Binds
    @IntoSet
    abstract fun localFlagSource(impl: InMemoryFeatureFlagSource): FeatureFlagSource

    @Binds
    abstract fun entitlements(impl: ProUnlockedEntitlements): Entitlements

    @Binds
    abstract fun consentGate(impl: InMemoryConsentGate): ConsentGate

    @Binds
    abstract fun analyticsTracker(impl: NoOpAnalyticsTracker): AnalyticsTracker
}

@Module
@InstallIn(SingletonComponent::class)
object CommonProvidesModule {
    @Provides
    @Dispatcher(KpDispatchers.IO)
    fun io(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Dispatcher(KpDispatchers.Default)
    fun default(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Dispatcher(KpDispatchers.Main)
    fun main(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(
        @Dispatcher(KpDispatchers.Default) dispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
}
