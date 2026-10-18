package com.kingplay.core.common

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagsTest {
    private val local = InMemoryFeatureFlagSource()
    private val flags = DefaultFeatureFlags(setOf(local))

    @Test
    fun usesDefaultsWhenNoOverride() {
        assertTrue(flags.isEnabled(Feature.CROSSFADE))
        assertFalse(flags.isEnabled(Feature.AI_FEATURES))
    }

    @Test
    fun overrideWins() {
        local.set(Feature.AI_FEATURES, true)
        assertTrue(flags.isEnabled(Feature.AI_FEATURES))
    }

    @Test
    fun observeEmitsChangesOnly() = runTest {
        flags.observe(Feature.STEMS).test {
            assertFalse(awaitItem())
            local.set(Feature.STEMS, true)
            assertTrue(awaitItem())
            local.set(Feature.CAST, false) // unrelated flag: no emission
            expectNoEvents()
        }
    }

    @Test
    fun emptySourceSetFallsBackToDefaults() {
        assertEquals(true, DefaultFeatureFlags(emptySet()).isEnabled(Feature.CAST))
    }

    @Test
    fun proIsUnlockedInPhaseOne() {
        assertTrue(ProUnlockedEntitlements().isUnlocked())
    }

    @Test
    fun consentedTrackerDropsEventsWithoutConsent() = runTest {
        val seen = mutableListOf<String>()
        val gate = InMemoryConsentGate()
        val tracker = ConsentedAnalyticsTracker(
            gate,
            object : AnalyticsTracker {
                override fun track(event: String, params: Map<String, String>) {
                    seen += event
                }
            },
        )
        tracker.track("a")
        gate.setAnalyticsConsent(true)
        tracker.track("b")
        assertEquals(listOf("b"), seen)
    }
}
