package com.kingplay.core.common

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Analytics is opt-in. Nothing is tracked unless consent is granted. */
interface ConsentGate {
    val analyticsConsent: Flow<Boolean>

    fun isAnalyticsAllowed(): Boolean

    suspend fun setAnalyticsConsent(granted: Boolean)
}

interface AnalyticsTracker {
    fun track(event: String, params: Map<String, String> = emptyMap())
}

/** In-memory consent store for tests. The app uses the DataStore-backed one in `:data:settings`. */
@Singleton
class InMemoryConsentGate @Inject constructor() : ConsentGate {
    private val granted = MutableStateFlow(false)

    override val analyticsConsent: Flow<Boolean> = granted

    override fun isAnalyticsAllowed(): Boolean = granted.value

    override suspend fun setAnalyticsConsent(granted: Boolean) {
        this.granted.value = granted
    }
}

/** Wraps any tracker so events are dropped without consent. */
class ConsentedAnalyticsTracker(
    private val consent: ConsentGate,
    private val delegate: AnalyticsTracker,
) : AnalyticsTracker {
    override fun track(event: String, params: Map<String, String>) {
        if (consent.isAnalyticsAllowed()) delegate.track(event, params)
    }
}

@Singleton
class NoOpAnalyticsTracker @Inject constructor() : AnalyticsTracker {
    override fun track(event: String, params: Map<String, String>) = Unit
}
