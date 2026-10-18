package com.kingplay.core.common

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

enum class Feature(val key: String, val defaultEnabled: Boolean) {
    CROSSFADE("crossfade", true),
    CAST("cast", true),
    ANDROID_AUTO("android_auto", true),
    AI_FEATURES("ai_features", false),
    STEMS("stems", false),
    LISTEN_TOGETHER("listen_together", false),
    CLOUD_SYNC("cloud_sync", false),
}

/** A source of flag overrides. Add a remote source later by binding another one `@IntoSet`. */
interface FeatureFlagSource {
    val overrides: Flow<Map<String, Boolean>>

    fun snapshot(): Map<String, Boolean>
}

interface FeatureFlags {
    fun isEnabled(feature: Feature): Boolean

    fun observe(feature: Feature): Flow<Boolean>
}

/** Local, in-memory overrides (debug menu, tests). */
@Singleton
class InMemoryFeatureFlagSource @Inject constructor() : FeatureFlagSource {
    private val state = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    override val overrides: Flow<Map<String, Boolean>> = state

    override fun snapshot(): Map<String, Boolean> = state.value

    fun set(feature: Feature, enabled: Boolean) {
        state.value = state.value + (feature.key to enabled)
    }

    fun clear() {
        state.value = emptyMap()
    }
}

@Singleton
class DefaultFeatureFlags @Inject constructor(
    private val sources: @JvmSuppressWildcards Set<FeatureFlagSource>,
) : FeatureFlags {

    override fun isEnabled(feature: Feature): Boolean {
        val merged = sources.fold(emptyMap<String, Boolean>()) { acc, source -> acc + source.snapshot() }
        return merged[feature.key] ?: feature.defaultEnabled
    }

    override fun observe(feature: Feature): Flow<Boolean> {
        val merged: Flow<Map<String, Boolean>> = if (sources.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(sources.map { it.overrides }) { maps ->
                maps.fold(emptyMap()) { acc, map -> acc + map }
            }
        }
        return merged.map { it[feature.key] ?: feature.defaultEnabled }.distinctUntilChanged()
    }
}
